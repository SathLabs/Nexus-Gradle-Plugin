package dev.satherov.nexus.test.internal.client;

import dev.satherov.nexus.gradle.internal.client.EarlyWindow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

///
/// Checks that the early loading window gets turned off in the fml.toml of a game directory.
///
public class EarlyWindowTest {

    @Test
    public void missingFileIsCreated(@TempDir Path directory) throws IOException {
        EarlyWindow.disable(directory.toFile());
        Assertions.assertThat(Files.readAllLines(directory.resolve("config").resolve("fml.toml"))).containsExactly("earlyWindowControl = false");
    }

    @Test
    public void existingKeyIsReplaced(@TempDir Path directory) throws IOException {
        Path file = EarlyWindowTest.write(directory, "# FML config\nearlyWindowControl = true\nmaxThreads = -1\n");
        EarlyWindow.disable(directory.toFile());
        Assertions.assertThat(Files.readString(file)).isEqualTo("# FML config\nearlyWindowControl = false\nmaxThreads = -1\n");
    }

    @Test
    public void missingKeyIsAdded(@TempDir Path directory) throws IOException {
        Path file = EarlyWindowTest.write(directory, "maxThreads = -1\nversionCheck = true\n");
        EarlyWindow.disable(directory.toFile());
        Assertions.assertThat(Files.readAllLines(file))
                .hasSize(3)
                .containsOnlyOnce("earlyWindowControl = false")
                .containsSubsequence("maxThreads = -1", "versionCheck = true");
    }

    @Test
    public void existingKeyIsReplacedKeepingCrlf(@TempDir Path directory) throws IOException {
        Path file = EarlyWindowTest.write(directory, "# FML config\r\nearlyWindowControl = true\r\nmaxThreads = -1\r\n");
        EarlyWindow.disable(directory.toFile());
        Assertions.assertThat(Files.readString(file)).isEqualTo("# FML config\r\nearlyWindowControl = false\r\nmaxThreads = -1\r\n");
    }

    @Test
    public void missingKeyIsAddedKeepingCrlf(@TempDir Path directory) throws IOException {
        Path file = EarlyWindowTest.write(directory, "maxThreads = -1\r\nversionCheck = true\r\n");
        EarlyWindow.disable(directory.toFile());
        String content = Files.readString(file);
        Assertions.assertThat(content.split("\r\n"))
                .hasSize(3)
                .containsOnlyOnce("earlyWindowControl = false")
                .containsSubsequence("maxThreads = -1", "versionCheck = true");
        Assertions.assertThat(content).endsWith("\r\n");
    }

    private static Path write(Path directory, String content) throws IOException {
        Path config = Files.createDirectories(directory.resolve("config"));
        return Files.writeString(config.resolve("fml.toml"), content);
    }
}
