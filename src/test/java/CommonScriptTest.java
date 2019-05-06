import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

public abstract class CommonScriptTest {
    protected static final File TEST_OUTPUT_DIRECTORY = new File(System.getProperty("user.dir") + "/tmp/script-test/");

    public abstract Process executeScript(final Map<String, String> environment, final List<String> args) throws IOException;

    public abstract File getOutputDirectory();

    public abstract File getScriptFile();

    @Test
    void testJarExists() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false);

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        logToConsole(process);
        Assert.assertNotEquals(0, process.exitValue());

        assertJarExists(null);
    }

    @Test
    void testDownloadOnly() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        logToConsole(process);
        Assert.assertEquals(0, process.exitValue());

        assertJarExists(null);
    }

    @Test
    void testDetectReleaseVersion() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_LATEST_RELEASE_VERSION.name(), "5.3.2");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        logToConsole(process);
        Assert.assertEquals(0, process.exitValue());

        assertJarExists("5.3.2");
    }

    @Test
    void testDetectVersionVersionKey() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_VERSION_KEY.name(), "DETECT_LATEST_4");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        logToConsole(process);
        Assert.assertEquals(0, process.exitValue());

        assertJarExists("4.4.2");
    }

    @Test
    void testDetectSource() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "https://repo.blackducksoftware.com:443/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/5.2.0/hub-detect-5.2.0.jar");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        logToConsole(process);
        Assert.assertEquals(0, process.exitValue());

        assertJarExists("5.2.0");
    }

    @Test
    void testEscapingSpacesOuter() throws IOException, InterruptedException {
        final boolean success = testEscapingSpaces("--detect.project.name=\"Synopsys Detect\"");
        Assert.assertTrue(success);
    }

    @Test
    void testEscapingSpacesInvalid() throws IOException, InterruptedException {
        final boolean success = testEscapingSpaces("--detect.project.name=Synopsys Detect");
        Assert.assertFalse(success);
    }

    protected boolean testEscapingSpaces(final String escapedProjectName) throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false);
        final List<String> arguments = new ArrayList<>();
        arguments.add(escapedProjectName);

        final Process process = executeScript(environment, arguments);
        waitForProcess(process);
        Assert.assertNotEquals(0, process.exitValue());
        final String standardOutput = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        IOUtils.copy(process.getErrorStream(), System.err);
        System.out.println(standardOutput);

        return standardOutput.contains("detect.project.name = Synopsys Detect");
    }

    protected void assertJarExists(final String detectVersion) {
        final FilenameFilter filenameFilter = (path, fileName) -> fileName.endsWith(".jar");
        final File[] jarFiles = getOutputDirectory().listFiles(filenameFilter);
        Assert.assertNotNull(jarFiles);

        File detectJarFile = null;
        for (final File jarFile : jarFiles) {
            if (StringUtils.isNotBlank(detectVersion)) {
                if (jarFile.getName().contains(detectVersion)) {
                    detectJarFile = jarFile;
                    break;
                }
            } else {
                detectJarFile = jarFile;
            }
        }

        Assert.assertNotNull(detectJarFile);
        Assert.assertTrue(detectJarFile.exists());
    }

    protected Map<String, String> createEnvironment(final boolean downloadOnly) {
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), getOutputDirectory().getAbsolutePath());
        environment.put(EnvironmentVariables.DETECT_DOWNLOAD_ONLY.name(), downloadOnly ? "1" : "");

        return environment;
    }

    protected void waitForProcess(final Process process) throws InterruptedException {
        waitForProcess(process, 100, TimeUnit.SECONDS);
    }

    private void waitForProcess(final Process process, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        final boolean processHitTimeout = !process.waitFor(timeout, timeUnit);
        Assert.assertFalse(processHitTimeout);
    }

    // Copying both streams will cause the logs to be deformed in the console.
    protected void logToConsole(final Process process) throws IOException {
        IOUtils.copy(process.getErrorStream(), System.err);
        final String standardOutput = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        System.out.println(standardOutput);
    }
}
