package dev.satherov.nexus.gradle.internal.client;

import org.gradle.api.GradleException;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

///
/// A build service that runs a virtual screen through Xvfb.
///
/// Safe to use from several tasks at once.
///
public abstract class XvfbDisplay implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    ///
    /// The name the service is registered under.
    ///
    public static final String NAME = "nexusXvfb";

    ///
    /// The first display number to try.
    ///
    private static final int FIRST_DISPLAY = 99;

    ///
    /// The last display number to try.
    ///
    private static final int LAST_DISPLAY = 198;

    ///
    /// The number of milliseconds Xvfb is given to take its display.
    ///
    private static final long START_TIMEOUT = 10_000L;

    ///
    /// The Xvfb process, or `null` if it hasn't been started yet.
    ///
    private @Nullable Process screen;

    ///
    /// The display of the Xvfb process, or `null` if it hasn't been started yet.
    ///
    private @Nullable String display;

    ///
    /// The display of a virtual screen, `:99` or the first free number above it, started at 1920x1080 on the first
    /// call and reused afterwards.
    ///
    /// @return The display of the virtual screen.
    ///
    /// @throws GradleException If no display from `:99` to `:198` is free or Xvfb does not start.
    ///
    public synchronized String display() {
        if (this.display != null) {
            return this.display;
        }

        for (int number = XvfbDisplay.FIRST_DISPLAY; number <= XvfbDisplay.LAST_DISPLAY; number++) {
            if (Files.exists(XvfbDisplay.lock(number)) || Files.exists(XvfbDisplay.socket(number))) {
                continue;
            }

            String name = ":" + number;
            Process screen;
            try {
                screen = new ProcessBuilder("Xvfb", name, "-screen", "0", "1920x1080x24").inheritIO().start();
            } catch (IOException exception) {
                throw new GradleException("Could not start Xvfb on '" + name + "'", exception);
            }

            if (XvfbDisplay.awaitStart(screen, number)) {
                this.screen = screen;
                this.display = name;
                return name;
            }

            screen.destroy();
        }

        throw new GradleException("Could not start Xvfb on any display from ':" + XvfbDisplay.FIRST_DISPLAY + "' to ':" + XvfbDisplay.LAST_DISPLAY + "'");
    }

    ///
    /// Waits until the given screen holds the lock file and the socket of the given display number.
    ///
    /// Stops once it does, or immediately if the screen exits or {@value #START_TIMEOUT} milliseconds have passed.
    ///
    /// @param screen The Xvfb process to wait for.
    /// @param number The number of the display.
    /// @return `true` if the screen holds the display.
    ///
    /// @throws GradleException If the thread is interrupted while waiting.
    ///
    private static boolean awaitStart(Process screen, int number) {
        Path lock = XvfbDisplay.lock(number);
        Path socket = XvfbDisplay.socket(number);
        long deadline = System.currentTimeMillis() + XvfbDisplay.START_TIMEOUT;
        while (screen.isAlive() && System.currentTimeMillis() < deadline) {
            // Another build may have started a screen on this display first, and its socket would be there too.
            if (Files.exists(socket) && XvfbDisplay.holdsLock(screen, lock)) {
                return true;
            }

            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                screen.destroy();
                Thread.currentThread().interrupt();
                throw new GradleException("Could not start Xvfb on ':" + number + "'", exception);
            }
        }

        return false;
    }

    ///
    /// @param screen The Xvfb process.
    /// @param lock   The lock file of a display.
    /// @return `true` if the lock file holds the pid of the screen.
    ///
    private static boolean holdsLock(Process screen, Path lock) {
        try {
            return Long.parseLong(Files.readString(lock).strip()) == screen.pid();
        } catch (IOException | NumberFormatException exception) {
            return false;
        }
    }

    ///
    /// The lock file an X server holds for the given display number.
    ///
    /// @param number The number of the display.
    /// @return The lock file of the display.
    ///
    private static Path lock(int number) {
        return Path.of("/tmp/.X" + number + "-lock");
    }

    ///
    /// The socket Xvfb opens for the given display number.
    ///
    /// @param number The number of the display.
    /// @return The socket of the display.
    ///
    private static Path socket(int number) {
        return Path.of("/tmp/.X11-unix/X" + number);
    }

    ///
    /// Stops the screen if one was started.
    ///
    @Override
    public synchronized void close() {
        if (this.screen != null) {
            this.screen.destroy();
        }
    }
}
