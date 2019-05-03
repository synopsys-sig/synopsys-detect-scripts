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

    private File findDetectJarFile(final File outputDirectory) {
        final FilenameFilter filenameFilter = (path, fileName) -> fileName.endsWith(".jar");
        final File[] jarFiles = outputDirectory.listFiles(filenameFilter);
        Assert.assertNotNull(jarFiles);
        Assert.assertEquals(1, jarFiles.length);

        return jarFiles[0];
    }

    private void waitForProcess(final Process process) throws InterruptedException {
        waitForProcess(process, 100, TimeUnit.SECONDS);
    }

    private void waitForProcess(final Process process, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        final boolean processHitTimeout = !process.waitFor(timeout, timeUnit);
        Assert.assertFalse(processHitTimeout);
    }
}
