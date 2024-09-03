package scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public abstract class CommonScriptTest {
    // TODO: Change this to 8 once the artifactory property DETECT_LATEST_8 exists
    protected static final int DETECT_LATEST_VERSION = 7;

    protected static final File WORKING_DIRECTORY = new File(System.getProperty("user.dir"));
    protected static final File TEST_OUTPUT_DIRECTORY = new File(WORKING_DIRECTORY, "build/tmp/scripts/");

    public abstract Process executeScript(final Map<String, String> environment, final List<String> args, final boolean inheritIO) throws IOException, InterruptedException;

    public abstract File getOutputDirectory();

    public abstract File getScriptFile();

    @Test
    void testBadSourceButLocalCopy() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);

        final Process process = executeScript(environment, new ArrayList<>(), true);
        assertExitCode(process, 0);
        assertAnyJarExists();

        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "");
        final Process badSourceProcess = executeScript(environment, new ArrayList<>(), true);
        assertNotExitCode(badSourceProcess, -1);
    }

    @Test
    void testJarExists() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false);

        final Process process = executeScript(environment, new ArrayList<>(), true);
        assertNotExitCode(process, 0);
        assertAnyJarExists();
    }

    @Test
    void testJarExistsOldEnvVar() throws IOException, InterruptedException {
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_PATH.name(), getOutputDirectory().getAbsolutePath());

        final Process process = executeScript(environment, new ArrayList<>(), true);
        assertNotExitCode(process, 0);
        assertAnyJarExists();
    }

    @Test
    void testDownloadOnly() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);

        final Process process = executeScript(environment, new ArrayList<>(), true);
        assertExitCode(process, 0);
        assertAnyJarExists();
    }

    @Test
    void testDetectReleaseVersion() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_LATEST_RELEASE_VERSION.name(), "5.3.2");

        final Process process = executeScript(environment, new ArrayList<>(), true);
        assertExitCode(process, 0);
        assertJarExists("5.3.2");
    }

    @Test
    void testDetectVersionKey() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_VERSION_KEY.name(), "DETECT_LATEST_5");

        final Process process = executeScript(environment, new ArrayList<>(), true);
        assertExitCode(process, 0);
        assertJarExists("5.6.2");
    }

    @Test
    void testDetectSource() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "https://sig-repo.synopsys.com/bds-integrations-release/com/synopsys/integration/synopsys-detect/5.1.0/synopsys-detect-5.1.0.jar");

        final Process process = executeScript(environment, new ArrayList<>(), true);
        assertExitCode(process, 0);
        assertJarExists("5.1.0");
    }

    @Test
    void testDetectQASource() throws IOException, InterruptedException {
        // This test requires the system running it to be on the internal network.
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "https://artifactory.internal.synopsys.com/artifactory/bds-integrations-test/com/synopsys/integration/synopsys-detect/6.3.0-SIGQA1/synopsys-detect-6.3.0-SIGQA1.jar");

        final Process process = executeScript(environment, new ArrayList<>(), true);
        assertExitCode(process, 0);
        assertJarExists("6.3.0-SIGQA1");
    }

    @Test
    void testJavaHome() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false);
        environment.put(EnvironmentVariables.JAVA_HOME.name(), "test/java/home");

        final Process process = executeScript(environment, new ArrayList<>(), false);
        assertExitCode(process, 127);

        final String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        assertTrue(output.contains("Java Source: JAVA_HOME/bin/java=test/java/home/bin/java"));
    }

    @Test
    void testDetectJavaPath() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false);
        environment.put(EnvironmentVariables.JAVA_HOME.name(), "test/java/home/badtest"); // Testing precedence
        environment.put(EnvironmentVariables.DETECT_JAVA_PATH.name(), "test/java/home/java");

        final Process process = executeScript(environment, new ArrayList<>(), false);
        assertExitCode(process, 127);

        final String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        assertTrue(output.contains("Java Source: DETECT_JAVA_PATH=test/java/home/java"));
    }

    @Test
    void testJavaPath() throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false);

        final Process process = executeScript(environment, new ArrayList<>(), false);
        assertExitCode(process, 7);

        final String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        assertTrue(output.contains("Java Source: PATH"));
    }

    @Test
    void testSpacesInDownloadDir() throws IOException, InterruptedException {
        final Map<String, String> environment = new HashMap<>();
        final File directoryWithSpaces = new File(getOutputDirectory(), "directory with spaces");
        directoryWithSpaces.mkdirs();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), directoryWithSpaces.getAbsolutePath());

        executeScript(environment, new ArrayList<>(), true);

        final File detectJarFile = assertJarExists(directoryWithSpaces, null);
        assertTrue(detectJarFile.delete());
    }

    protected boolean testEscapingSpaces(final String escapedProjectName) throws IOException, InterruptedException {
        final Map<String, String> environment = createEnvironment(false);
        final List<String> arguments = new ArrayList<>();
        arguments.add(escapedProjectName);

        final Process process = executeScript(environment, arguments, false);
        ///////////assertNotExitCode(process, 0);

        final String standardOutput = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        IOUtils.copy(process.getErrorStream(), System.err);
        System.out.println(standardOutput);

        return standardOutput.contains("detect.project.name = Blackduck Detect");
    }

    protected void assertExitCode(final Process process, final int exitCode) {
        assertEquals(exitCode, process.exitValue(), "Unexpected exit code was returned.");
    }

    protected void assertNotExitCode(final Process process, final int exitCode) {
        assertNotEquals(exitCode, process.exitValue(), String.format("Expected an exit code other than %d to be returned.", exitCode));
    }

    protected void assertAnyJarExists() throws IOException {
        assertJarExists(null);
    }

    protected void assertJarExists(@Nullable final String detectVersion) throws IOException {
        assertJarExists(getOutputDirectory(), detectVersion);
    }

    protected File assertJarExists(final File searchDirectory, @Nullable final String detectVersion) throws IOException {
        return Files.walk(searchDirectory.toPath())
                                 .filter(filePath -> {
                                     final String fileName = filePath.toFile().getName();
                                     if (StringUtils.isNotBlank(detectVersion)) {
                                         return fileName.endsWith(String.format("%s.jar", detectVersion));
                                     } else {
                                         return fileName.endsWith(".jar");
                                     }
                                 })
                                 .findFirst()
                                 .map(Path::toFile)
                                 .orElseThrow(() -> new FileNotFoundException("Expected to find a .jar file."));
    }

    protected Map<String, String> createEnvironment(final boolean downloadOnly) {
        final Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariables.DETECT_JAR_DOWNLOAD_DIR.name(), getOutputDirectory().getAbsolutePath());
        if (downloadOnly) {
            environment.put(EnvironmentVariables.DETECT_DOWNLOAD_ONLY.name(), "1");
        }

        return environment;
    }

    protected Process createProcess(final List<String> finalCommand, final Map<String, String> environment, final boolean inheritIO) throws IOException, InterruptedException {
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
        final Process process = processBuilder.start();

        final boolean processHitTimeout = !process.waitFor(10, TimeUnit.MINUTES);
        assertFalse(processHitTimeout);

        return process;
    }
}
