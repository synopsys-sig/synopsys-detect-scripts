import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

public abstract class CommonScriptTest {
    public abstract Process executeScript(final Map<String, String> environment, final List<String> args) throws IOException;

    public abstract File getOutputDirectory();

    @Test
    void testJarExists() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false);

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertNotEquals(0, process.exitValue());

        assertJarExists(null);
    }

    @Test
    void testDownloadOnly() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertEquals(0, process.exitValue());

        assertJarExists(null);
    }

    @Test
    void testDetectReleaseVersion() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_LATEST_RELEASE_VERSION.name(), "5.3.2");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertEquals(0, process.exitValue());

        assertJarExists("5.3.2");
    }

    @Test
    void testDetectVersionVersionKey() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_VERSION_KEY.name(), "DETECT_LATEST_4");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertEquals(0, process.exitValue());

        assertJarExists("4.4.2");
    }

    @Test
    void testDetectSource() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "https://repo.blackducksoftware.com:443/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/5.2.0/hub-detect-5.2.0.jar");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertEquals(0, process.exitValue());

        assertJarExists("5.2.0");
    }

    private void assertJarExists(@Nullable final String detectVersion) {
        final FilenameFilter filenameFilter = (path, fileName) -> fileName.endsWith(".jar");
        final File[] jarFiles = getOutputDirectory().listFiles(filenameFilter);
        Assert.assertNotNull(jarFiles);

        File detectJarFile = null;
        for (final File jarFile : jarFiles) {
            if (StringUtils.isNotBlank(detectVersion) && jarFile.getName().contains(detectVersion)) {
                detectJarFile = jarFile;
                break;
            } else {
                detectJarFile = jarFile;
            }
        }

        Assert.assertNotNull(detectJarFile);
        Assert.assertTrue(detectJarFile.exists());
    }

    private Map<String, String> createEnvironment(final boolean downloadOnly) {
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), getOutputDirectory().getAbsolutePath());
        environment.put(EnvironmentVariables.DETECT_DOWNLOAD_ONLY.name(), downloadOnly ? "1" : "");

        return environment;
    }

    private void waitForProcess(final Process process) throws InterruptedException {
        waitForProcess(process, 100, TimeUnit.SECONDS);
    }

    private void waitForProcess(final Process process, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        final boolean processHitTimeout = !process.waitFor(timeout, timeUnit);
        Assert.assertFalse(processHitTimeout);
    }
}
