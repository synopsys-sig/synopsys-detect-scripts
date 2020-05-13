package scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.synopsys.detect.scripts.scripts.ScriptBuilder;

// TODO: Implement the download only capability in powershell then remove tests that have this message. If the script cannot download only, it will try to run detect, and get the wrong exit codes for the test.
public class PowershellScriptTest extends CommonScriptTest {
    private static final File powershellScriptDataDirectory = new File(TEST_OUTPUT_DIRECTORY, "powershellScriptData");

    private static File powershellScript;

    @BeforeAll
    static void setUpBeforeAll() throws IOException, IntegrationException {
        TEST_OUTPUT_DIRECTORY.mkdirs();
        powershellScriptDataDirectory.mkdirs();

        final ScriptBuilder scriptBuilder = new ScriptBuilder();
        final List<File> scriptFiles = scriptBuilder.generateScript(TEST_OUTPUT_DIRECTORY, "detect-ps.ps1", "ps1", "something-SNAPSHOT");
        assertEquals(1, scriptFiles.size());

        powershellScript = scriptFiles.get(0);
    }

    @AfterEach
    void tearDown() {
        cleanupFiles(powershellScriptDataDirectory);
    }

    @AfterAll
    static void tearDownAfterAll() {
        cleanupFiles(TEST_OUTPUT_DIRECTORY);
    }

    @Override
    public Process executeScript(final Map<String, String> environment, final List<String> args, final boolean inheritIO) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>();
        command.add("pwsh");
        command.add("-Command");

        final String scriptPath = getScriptFile().getAbsolutePath();
        final String argumentsString = StringUtils.join(args, " ");
        final String commandString = String.format("& { . %s; detect %s}", scriptPath, argumentsString);
        command.add(commandString);

        return createProcess(command, environment, inheritIO);
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
        final Map<String, String> environment = createEnvironment(true, false);
        environment.put(EnvironmentVariables.DETECT_VERSION_KEY.name(), "DETECT_LATEST_4");

        executeScript(environment, new ArrayList<>(), true);
        // assertEquals(0, process.exitValue());

        assertJarExists("4.4.2");
    }

    @Override
    @Test
    void testDetectSource() throws IOException, InterruptedException {
        // TODO: Implement the download only capability in powershell then remove this test
        final Map<String, String> environment = createEnvironment(true, false);
        environment.put(EnvironmentVariables.DETECT_SOURCE.name(), "https://repo.blackducksoftware.com:443/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/5.2.0/hub-detect-5.2.0.jar");

        executeScript(environment, new ArrayList<>(), true);
        // assertEquals(0, process.exitValue());

        assertJarExists("5.2.0");
    }

    @Override
    @Test
    void testDetectReleaseVersion() throws IOException, InterruptedException {
        // TODO: Implement the download only capability in powershell then remove this test
        final Map<String, String> environment = createEnvironment(true, false);
        environment.put(EnvironmentVariables.DETECT_LATEST_RELEASE_VERSION.name(), "5.3.2");

        executeScript(environment, new ArrayList<>(), true);
        // assertEquals(0, process.exitValue());

        assertJarExists("5.3.2");
    }

    @Override
    @Test
    @Disabled
    void testDownloadOnly() {
        // TODO: Implement the download only capability in powershell then remove this test
    }
}
