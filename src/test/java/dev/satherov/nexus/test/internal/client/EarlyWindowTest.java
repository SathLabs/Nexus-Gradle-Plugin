package dev.satherov.nexus.test.internal.client;

import dev.satherov.nexus.gradle.internal.client.EarlyWindow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        Path file = EarlyWindowTest.write(directory, "# FML config", "earlyWindowControl = true", "maxThreads = -1");
        EarlyWindow.disable(directory.toFile());
        Assertions.assertThat(Files.readAllLines(file)).containsExactly("# FML config", "earlyWindowControl = false", "maxThreads = -1");
    }

    @Test
    public void missingKeyIsAdded(@TempDir Path directory) throws IOException {
        Path file = EarlyWindowTest.write(directory, "maxThreads = -1", "versionCheck = true");
        EarlyWindow.disable(directory.toFile());
        Assertions.assertThat(Files.readAllLines(file))
                .hasSize(3)
                .containsOnlyOnce("earlyWindowControl = false")
                .containsSubsequence("maxThreads = -1", "versionCheck = true");
    }

    private static Path write(Path directory, String... lines) throws IOException {
        Path config = Files.createDirectories(directory.resolve("config"));
        return Files.write(config.resolve("fml.toml"), List.of(lines));
    }
}
