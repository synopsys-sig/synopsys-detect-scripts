package scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.synopsys.detect.scripts.scripts.ScriptBuilder;

public class ShellScriptTest extends CommonScriptTest {
    private static final File shellScriptDataDirectory = new File(TEST_OUTPUT_DIRECTORY, "shellScriptData");

    private static File shellScriptDetect6;
    private static File shellScriptDetect7;

    @BeforeAll
    static void setUpBeforeAll() throws IOException, IntegrationException {
        TEST_OUTPUT_DIRECTORY.mkdirs();

        final ScriptBuilder scriptBuilder = new ScriptBuilder();
        final List<File> scriptFiles = new ArrayList<>();
        scriptBuilder.generateScript(scriptFiles, TEST_OUTPUT_DIRECTORY, "detect-sh.sh", "sh", "version-SNAPSHOT", 6);
        assertEquals(1, scriptFiles.size());

        scriptBuilder.generateScript(scriptFiles, TEST_OUTPUT_DIRECTORY, "detect-sh.sh", "sh", "version-SNAPSHOT", 7);
        assertEquals(2, scriptFiles.size());

        shellScriptDetect6 = scriptFiles.get(0);
        shellScriptDetect7 = scriptFiles.get(1);
    }

    @BeforeEach
    void setupBeforeEach() {
        shellScriptDataDirectory.mkdirs();
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(shellScriptDataDirectory);
    }

    @AfterAll
    static void tearDownAfterAll() throws IOException {
        FileUtils.deleteDirectory(TEST_OUTPUT_DIRECTORY);
    }

    @Override
    public Process executeScript(final Map<String, String> environment, final List<String> args, final boolean inheritIO) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>();
        command.add(getScriptFile().getAbsolutePath());
        command.addAll(args);

        return createProcess(command, environment, inheritIO);
    }

    @Override
    public File getOutputDirectory() {
        return shellScriptDataDirectory;
    }

    @Override
    public File getScriptFile() {
        return shellScriptDetect6;
    }

    @Test
    void testEscapingSpacesInner() throws IOException, InterruptedException {
        final boolean success = testEscapingSpaces("--detect.project.name=Synopsys\\ Detect");
        assertTrue(success);
    }

}
