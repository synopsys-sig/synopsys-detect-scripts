package scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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

    private static File powershellScriptDetect;

    @BeforeAll
    static void setUpBeforeAll() throws IOException, IntegrationException {
        TEST_OUTPUT_DIRECTORY.mkdirs();
        powershellScriptDataDirectory.mkdirs();

        final ScriptBuilder scriptBuilder = new ScriptBuilder();
        final List<File> scriptFiles = new ArrayList<>();
        scriptBuilder.generateScript(scriptFiles, TEST_OUTPUT_DIRECTORY, "detect-ps.ps1", "ps1", "version-SNAPSHOT", DETECT_LATEST_VERSION);
        assertEquals(1, scriptFiles.size());

        powershellScriptDetect = scriptFiles.get(0);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(powershellScriptDataDirectory);
    }

    @AfterAll
    static void tearDownAfterAll() throws IOException {
        FileUtils.deleteDirectory(TEST_OUTPUT_DIRECTORY);
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
        return powershellScriptDetect;
    }

    @Test
    void testEscapedSpace() throws IOException, InterruptedException {
        final boolean success = testEscapingSpaces("--detect.project.name=Synopsys` Detect");
        assertTrue(success);
    }

    @Test
    void testEscapingSpacesOuter() throws IOException, InterruptedException {
        final boolean success = testEscapingSpaces("--detect.project.name=\"Synopsys Detect\"");
        assertTrue(success);
    }

    @Override
    @Test
    @Disabled
    void testDownloadOnly() {
        // TODO: Implement the download only capability in powershell then remove this test
    }
}
