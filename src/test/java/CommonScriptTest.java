import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

public abstract class CommonScriptTest {
    public abstract Process executeScript(final Map<String, String> environment, final List<String> args) throws IOException;

    public abstract File getOutputDirectory();

    @Test
    void testJarExists() throws IOException, InterruptedException {
        final File outputDirectory = getOutputDirectory();
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), outputDirectory.getAbsolutePath());

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);

        final File detectJarFile = findDetectJarFile(outputDirectory);
        Assert.assertTrue(detectJarFile.exists());
    }

    @Test
    void testDownloadOnly() throws IOException, InterruptedException {
        final File outputDirectory = getOutputDirectory();
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), outputDirectory.getAbsolutePath());
        environment.put(EnvironmentVariables.DETECT_DOWNLOAD_ONLY.name(), "1");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertEquals(0, process.exitValue());

        final File detectJarFile = findDetectJarFile(outputDirectory);
        Assert.assertTrue(detectJarFile.exists());
    }

    @Test
    void testDetectReleaseVersion() throws IOException, InterruptedException {
        final File outputDirectory = getOutputDirectory();
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), outputDirectory.getAbsolutePath());
        environment.put(EnvironmentVariables.DETECT_DOWNLOAD_ONLY.name(), "1");
        environment.put(EnvironmentVariables.DETECT_LATEST_RELEASE_VERSION.name(), "5.3.2");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertEquals(0, process.exitValue());

        final File detectJarFile = findDetectJarFile(outputDirectory, "5.3.2");
        Assert.assertTrue(detectJarFile.exists());
    }

    @Test
    void testDetectVersionVersionKey() throws IOException, InterruptedException {
        final File outputDirectory = getOutputDirectory();
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), outputDirectory.getAbsolutePath());
        environment.put(EnvironmentVariables.DETECT_DOWNLOAD_ONLY.name(), "1");
        environment.put(EnvironmentVariables.DETECT_VERSION_KEY.name(), "DETECT_LATEST_4");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertEquals(0, process.exitValue());

        final File detectJarFile = findDetectJarFile(outputDirectory, "4.4.2");
        Assert.assertTrue(detectJarFile.exists());
    }

    @Test
    void testDetectSource() throws IOException, InterruptedException {
        final File outputDirectory = getOutputDirectory();
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), outputDirectory.getAbsolutePath());
        environment.put(EnvironmentVariables.DETECT_DOWNLOAD_ONLY.name(), "1");
        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "https://repo.blackducksoftware.com:443/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/5.2.0/hub-detect-5.2.0.jar");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        Assert.assertEquals(0, process.exitValue());

        final File detectJarFile = findDetectJarFile(outputDirectory, "5.2.0");
        Assert.assertTrue(detectJarFile.exists());
    }

    private File findDetectJarFile(final File outputDirectory) {
        return findDetectJarFile(outputDirectory, null);
    }

    private File findDetectJarFile(final File outputDirectory, final String detectVersion) {
        final FilenameFilter filenameFilter = (path, fileName) -> isDetectJar(fileName, detectVersion);
        final File[] jarFiles = outputDirectory.listFiles(filenameFilter);
        Assert.assertNotNull(jarFiles);
        Assert.assertEquals("Failed to find detect jar.", 1, jarFiles.length);

        return jarFiles[0];
    }

    private boolean isDetectJar(final String fileName, final String detectVersion) {
        final boolean isJar = fileName.endsWith(".jar");
        boolean versionCheckPassed = true;
        if (StringUtils.isNotBlank(detectVersion)) {
            versionCheckPassed = fileName.contains(detectVersion);
        }

        return isJar && versionCheckPassed;
    }

    private void waitForProcess(final Process process) throws InterruptedException {
        waitForProcess(process, 100, TimeUnit.SECONDS);
    }

    private void waitForProcess(final Process process, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        final boolean processHitTimeout = !process.waitFor(timeout, timeUnit);
        Assert.assertFalse(processHitTimeout);
    }
}
