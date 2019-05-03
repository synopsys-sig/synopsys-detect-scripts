import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.synopsys.detect.scripts.ScriptBuilder;

// TODO: Implement the download only capability in powershell then remove tests that have this message. If the script cannot download only, it will try to run detect, and get the wrong exit codes for the test.
public class PowershellScriptTest extends CommonScriptTest {
    private static final File powershellScriptOutputDirectory = new File("/tmp/script-test/");
    private static final File powershellScriptDataDirectory = new File(powershellScriptOutputDirectory, "shellScriptData");

    private static File powershellScript;

    @BeforeAll
    static void setUpBeforeAll() throws IOException, IntegrationException {
        powershellScriptOutputDirectory.mkdirs();
        powershellScriptDataDirectory.mkdirs();

        final ScriptBuilder scriptBuilder = new ScriptBuilder();
        final List<File> scriptFiles = scriptBuilder.generateScript(powershellScriptOutputDirectory, "detect-ps.ps1", "ps1", "something-SNAPSHOT");
        Assert.assertEquals(1, scriptFiles.size());

        powershellScript = scriptFiles.get(0);
    }

    @AfterEach
    void tearDown() {
        Arrays.stream(Objects.requireNonNull(powershellScriptDataDirectory.listFiles()))
            .map(File::delete)
            .forEach(Assert::assertTrue);
    }

    @AfterAll
    static void tearDownAfterAll() {
        Arrays.stream(Objects.requireNonNull(powershellScriptOutputDirectory.listFiles()))
            .map(File::delete)
            .forEach(Assert::assertTrue);
    }

    @Override
    public Process executeScript(final Map<String, String> environment, final List<String> args) throws IOException {
        final List<String> command = new ArrayList<>();
        // pwsh -Command "& { . /Users/jakem/workspace/scripts/synopsys-detect-scripts/build/libs/detect-2.1.1-SNAPSHOT.ps1; detect --detect.project.name="test"}"
        command.add("pwsh");
        command.add("-Command");

        final String scriptPath = getScriptFile().getAbsolutePath();
        final String argumentsString = StringUtils.join(args, " ");
        final String commandString = String.format("& { . %s; detect %s}", scriptPath, argumentsString);
        command.add(commandString);

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        //        processBuilder.environment().clear();
        processBuilder.environment().putAll(environment);

        // We could tell the process builder to inheritIO to log to console, but some tests may need data from the process output streams.
        return processBuilder.start();
    }

    @Override
    public File getOutputDirectory() {
        return powershellScriptDataDirectory;
    }

    @Override
    public File getScriptFile() {
        return powershellScript;
    }

    @Override
    @Test
    void testDetectVersionVersionKey() throws IOException, InterruptedException {
        // TODO: Implement the download only capability in powershell then remove this test
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_VERSION_KEY.name(), "DETECT_LATEST_4");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        logToConsole(process);
        // Assert.assertEquals(0, process.exitValue());

        assertJarExists("4.4.2");
    }

    @Override
    @Test
    void testDetectSource() throws IOException, InterruptedException {
        // TODO: Implement the download only capability in powershell then remove this test
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "https://repo.blackducksoftware.com:443/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/5.2.0/hub-detect-5.2.0.jar");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        logToConsole(process);
        // Assert.assertEquals(0, process.exitValue());

        assertJarExists("5.2.0");
    }

    @Override
    @Test
    void testDetectReleaseVersion() throws IOException, InterruptedException {
        // TODO: Implement the download only capability in powershell then remove this test
        final Map<String, String> environment = createEnvironment(true);
        environment.put(EnvironmentVariables.DETECT_LATEST_RELEASE_VERSION.name(), "5.3.2");

        final Process process = executeScript(environment, new ArrayList<>());
        waitForProcess(process);
        logToConsole(process);
        // Assert.assertEquals(0, process.exitValue());

        assertJarExists("5.3.2");
    }

    @Override
    @Test
    @Disabled
    void testDownloadOnly() {
        // TODO: Implement the download only capability in powershell then remove this test
    }
}
