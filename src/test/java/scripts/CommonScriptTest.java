package scripts;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public abstract class CommonScriptTest {
    protected static final File WORKING_DIRECTORY = new File(System.getProperty("user.dir"));
    protected static final File TEST_OUTPUT_DIRECTORY = new File(WORKING_DIRECTORY, "build/tmp/scripts/");

    public abstract Process executeScript(final Map<String, String> environment, final List<String> args, final boolean inheritIO) throws IOException;

    public abstract File getOutputDirectory();

    public abstract File getScriptFile();

    protected static void cleanupFiles(final File file) {
        if (file.isDirectory()) {
            for (final File listFile : file.listFiles()) {
                cleanupFiles(listFile);
            }
        }
        file.delete();
    }

    @Test
    void testBadSourceButLocalCopy() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true, false);

        final Process process = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(process);
        assertEquals(0, process.exitValue());
        assertJarExists(null);

        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "");
        final Process badSourceProcess = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(badSourceProcess);

        assertNotEquals(-1, process.exitValue());
    }

    @Test
    void testJarExists() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false, false);

        final Process process = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(process);
        assertNotEquals(0, process.exitValue());

        assertJarExists(null);
    }

    @Test
    void testJarExistsOldEnvVar() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false, true);

        final Process process = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(process);
        assertNotEquals(0, process.exitValue());

        assertJarExists(null);
    }

    @Test
    void testDownloadOnly() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true, false);

        final Process process = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(process);
        assertEquals(0, process.exitValue());

        assertJarExists(null);
    }

    @Test
    void testDetectReleaseVersion() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true, false);
        environment.put(EnvironmentVariables.DETECT_LATEST_RELEASE_VERSION.name(), "5.3.2");

        final Process process = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(process);
        assertEquals(0, process.exitValue());

        assertJarExists("5.3.2");
    }

    @Test
    void testDetectVersionVersionKey() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true, false);
        environment.put(EnvironmentVariables.DETECT_VERSION_KEY.name(), "DETECT_LATEST_5");

        final Process process = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(process);
        assertEquals(0, process.exitValue());

        assertJarExists("5.6.2");
    }

    @Test
    void testDetectSource() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true, false);
        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "https://sig-repo.synopsys.com/bds-integrations-release/com/synopsys/integration/synopsys-detect/5.1.0/synopsys-detect-5.1.0.jar");

        final Process process = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(process);
        assertEquals(0, process.exitValue());

        assertJarExists("5.1.0");
    }

    @Test
    void testEscapingSpacesOuter() throws IOException, InterruptedException {
        final boolean success = testEscapingSpaces("--detect.project.name=\"Synopsys Detect\"");
        assertTrue(success);
    }

    @Test
    void testEscapingSpacesInvalid() throws IOException, InterruptedException {
        final boolean success = testEscapingSpaces("--detect.project.name=Synopsys Detect");
        assertFalse(success);
    }

    @Test
    void testJavaHome() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false, false);
        environment.put(EnvironmentVariables.JAVA_HOME.name(), "test/java/home");

        final Process process = executeScript(environment, new ArrayList<>(), false);
        waitForProcess(process);
        assertEquals(127, process.exitValue());

        final String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        assertTrue(output.contains("Java Source: JAVA_HOME/bin/java=test/java/home/bin/java"));
    }

    @Test
    void testDetectJavaPath() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false, false);
        environment.put(EnvironmentVariables.JAVA_HOME.name(), "test/java/home/badtest"); // Testing precedence
        environment.put(EnvironmentVariables.DETECT_JAVA_PATH.name(), "test/java/home/java");

        final Process process = executeScript(environment, new ArrayList<>(), false);
        waitForProcess(process);
        assertEquals(127, process.exitValue());

        final String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        assertTrue(output.contains("Java Source: DETECT_JAVA_PATH=test/java/home/java"));
    }

    @Test
    void testJavaPath() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false, false);

        final Process process = executeScript(environment, new ArrayList<>(), false);
        waitForProcess(process);
        assertEquals(7, process.exitValue());

        final String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        assertTrue(output.contains("Java Source: PATH"));
    }

    @Test
    void testSpacesInDownloadDir() throws IOException, InterruptedException {
        final Map<String, String> environment = new HashMap<>();
        final File directoryWithSpaces = new File(getOutputDirectory(), "directory with spaces");
        directoryWithSpaces.mkdirs();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), directoryWithSpaces.getAbsolutePath());

        final Process process = executeScript(environment, new ArrayList<>(), true);
        waitForProcess(process);

        final File detectJarFile = assertJarExists(directoryWithSpaces, null);
        assertTrue(detectJarFile.delete());
    }

    protected boolean testEscapingSpaces(final String escapedProjectName) throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false, false);
        final List<String> arguments = new ArrayList<>();
        arguments.add(escapedProjectName);

        final Process process = executeScript(environment, arguments, false);
        waitForProcess(process);
        assertNotEquals(0, process.exitValue());
        final String standardOutput = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        IOUtils.copy(process.getErrorStream(), System.err);
        System.out.println(standardOutput);

        return standardOutput.contains("detect.project.name = Synopsys Detect");
    }

    protected File assertJarExists(final String detectVersion) {
        return assertJarExists(getOutputDirectory(), detectVersion);
    }

    protected File assertJarExists(final File searchDirectory, final String detectVersion) {
        final FilenameFilter filenameFilter = (path, fileName) -> fileName.endsWith(".jar");
        final File[] jarFiles = searchDirectory.listFiles(filenameFilter);
        assertNotNull(jarFiles);

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

        assertNotNull(detectJarFile);
        assertTrue(detectJarFile.exists());

        return detectJarFile;
    }

    protected Map<String, String> createEnvironment(final boolean downloadOnly, final boolean setOldJarDownloadDirVar) {
        final Map<String, String> environment = new HashMap<>();
        if (setOldJarDownloadDirVar) {
            environment.put(EnvironmentVariables.DETECT_JAR_PATH.name(), getOutputDirectory().getAbsolutePath());
        } else {
            environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), getOutputDirectory().getAbsolutePath());
        }
        environment.put(EnvironmentVariables.DETECT_DOWNLOAD_ONLY.name(), downloadOnly ? "1" : "");

        return environment;
    }

    protected void waitForProcess(final Process process) throws InterruptedException {
        waitForProcess(process, 100, TimeUnit.SECONDS);
    }

    private void waitForProcess(final Process process, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        final boolean processHitTimeout = !process.waitFor(timeout, timeUnit);
        assertFalse(processHitTimeout);
    }

    protected Process createProcess(final List<String> finalCommand, final Map<String, String> environment, final boolean inheritIO) throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(finalCommand);
        processBuilder.environment().clear();
        processBuilder.environment().put("PATH", System.getenv("PATH"));
        processBuilder.environment().putAll(environment);

        System.out.println(String.format("Creating process for '%s' with the following environment.", finalCommand));
        for (final Map.Entry<String, String> entry : processBuilder.environment().entrySet()) {
            System.out.println(String.format("    %s=%s", entry.getKey(), entry.getValue()));
        }

        if (inheritIO) {
            // inheritIO to log to console unless the test requires the data from the output streams.
            processBuilder.inheritIO();
        }

        // We could tell the process builder to inheritIO to log to console, but some tests may need data from the process output streams.
        return processBuilder.start();
    }

}
