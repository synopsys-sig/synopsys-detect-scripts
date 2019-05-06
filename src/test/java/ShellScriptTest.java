import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.synopsys.detect.scripts.ScriptBuilder;

public class ShellScriptTest extends CommonScriptTest {
    private static final File shellScriptDataDirectory = new File(TEST_OUTPUT_DIRECTORY, "shellScriptData");

    private static File shellScript;

    @BeforeAll
    static void setUpBeforeAll() throws IOException, IntegrationException {
        TEST_OUTPUT_DIRECTORY.mkdirs();
        shellScriptDataDirectory.mkdirs();

        final ScriptBuilder scriptBuilder = new ScriptBuilder();
        final List<File> scriptFiles = scriptBuilder.generateScript(TEST_OUTPUT_DIRECTORY, "detect-sh.sh", "sh", "something-SNAPSHOT");
        Assert.assertEquals(1, scriptFiles.size());

        shellScript = scriptFiles.get(0);
    }

    @AfterEach
    void tearDown() {
        Arrays.stream(Objects.requireNonNull(shellScriptDataDirectory.listFiles()))
            .map(File::delete)
            .forEach(Assert::assertTrue);
    }

    @AfterAll
    static void tearDownAfterAll() {
        Arrays.stream(Objects.requireNonNull(TEST_OUTPUT_DIRECTORY.listFiles()))
            .map(File::delete)
            .forEach(Assert::assertTrue);
    }

    @Override
    public Process executeScript(final Map<String, String> environment, final List<String> args) throws IOException {
        final List<String> command = new ArrayList<>();
        command.add(getScriptFile().getAbsolutePath());
        command.addAll(args);

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        processBuilder.environment().clear();
        processBuilder.environment().putAll(environment);

        // We could tell the process builder to inheritIO to log to console, but some tests may need data from the process output streams.
        return processBuilder.start();
    }

    @Override
    public File getOutputDirectory() {
        return shellScriptDataDirectory;
    }

    @Override
    public File getScriptFile() {
        return shellScript;
    }

    @Test
    void testEscapingSpacesInner() throws IOException, InterruptedException {
        final boolean success = testEscapingSpaces("--detect.project.name=Synopsys\\ Detect");
        Assert.assertTrue(success);
    }
}
