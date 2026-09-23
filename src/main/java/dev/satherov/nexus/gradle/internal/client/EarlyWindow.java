package dev.satherov.nexus.gradle.internal.client;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

///
/// A utility that turns off FML's early loading window in a game directory.
///
@UtilityClass
public class EarlyWindow {

    ///
    /// The key of the FML config that turns the early loading window on and off.
    ///
    private static final String KEY = "earlyWindowControl";

    ///
    /// Writes `earlyWindowControl = false` into `<gameDirectory>/config/fml.toml`, replacing the key if the file has
    /// it and keeping every other line and the file's line separator, creating the file and its directory if missing.
    ///
    /// @param gameDirectory The game directory of the run.
    ///
    /// @throws UncheckedIOException If the file could not be read or written.
    ///
    public static void disable(File gameDirectory) {
        Path config = gameDirectory.toPath().resolve("config");
        Path file = config.resolve("fml.toml");
        String control = EarlyWindow.KEY + " = false";
        try {
            String content = Files.isRegularFile(file) ? Files.readString(file) : "";
            String separator = content.contains("\r\n") ? "\r\n" : "\n";
            List<String> lines = content.lines().collect(Collectors.toCollection(ArrayList::new));
            int existing = IntStream.range(0, lines.size())
                    .filter(index -> lines.get(index).split("=", 2)[0].strip().equals(EarlyWindow.KEY))
                    .findFirst()
                    .orElse(-1);

            if (existing < 0) {
                // If the file has a table, a key added at its end would belong to that table.
                lines.add(0, control);
            } else {
                lines.set(existing, control);
            }

            Files.createDirectories(config);
            Files.writeString(file, String.join(separator, lines) + separator);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write '" + file + "'", exception);
        }
    }
}
