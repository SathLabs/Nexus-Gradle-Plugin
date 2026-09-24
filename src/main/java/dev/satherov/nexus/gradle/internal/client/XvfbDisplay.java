package dev.satherov.nexus.gradle.internal.client;

import org.gradle.api.GradleException;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

///
/// A build service that creates a virtual screen through Xvfb.
///
public abstract class XvfbDisplay implements BuildService<BuildServiceParameters.None>, AutoCloseable {
    
    ///
    /// The name that the service is registered under.
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
    /// The number of milliseconds Xvfb is given to write its display number.
    ///
    private static final long START_TIMEOUT = 10_000L;
    
    ///
    /// The Xvfb process, or `null` if it hasn't started yet.
    ///
    private @Nullable Process screen;
    
    ///
    /// The display of the Xvfb process, or `null` if it hasn't started yet.
    ///
    private @Nullable String display;
    
    ///
    /// Waits until the given screen writes the number of its display.
    ///
    /// Stops when it did, or the screen is not alive anymore.
    ///
    /// If this method fails, the screen will be destroyed.
    ///
    /// @param screen  The Xvfb process to wait for.
    /// @param display The display that the screen was started on, used in the failure message.
    ///
    /// @return The number that the screen wrote, or `null` if it was destroyed before.
    ///
    /// @throws GradleException If the screen writes nothing for {@value #START_TIMEOUT} milliseconds, its output could not be read,
    /// or the thread is interrupted while waiting.
    ///
    private static @Nullable String awaitNumber(Process screen, String display) {
        long deadline = System.currentTimeMillis() + XvfbDisplay.START_TIMEOUT;
        try (BufferedReader output = screen.inputReader()) {
            while (System.currentTimeMillis() < deadline) {
                // Xvfb writes the line break right after the number, so a started line is read to its end.
                if (output.ready()) {
                    return Objects.requireNonNullElse(output.readLine(), "").strip();
                }
                
                if (!screen.isAlive()) {
                    return null;
                }
                
                //noinspection BusyWait Yes intellij this is busy waiting, but that's the point.
                Thread.sleep(50L);
            }
        } catch (IOException exception) {
            screen.destroy();
            throw new GradleException("Could not start Xvfb on '" + display + "'", exception);
        } catch (InterruptedException exception) {
            screen.destroy();
            Thread.currentThread().interrupt();
            throw new GradleException("Could not start Xvfb on '" + display + "'", exception);
        }
        
        screen.destroy();
        throw new GradleException("Could not start Xvfb on '" + display + "', it did not write its display within '" + XvfbDisplay.START_TIMEOUT + "' milliseconds");
    }
    
    ///
    /// The display of a virtual screen, will be `:99` or the next free number above it.
    ///
    /// The display is created at 1920x1080 on the first call and then reused afterward.
    ///
    /// @return The display of the virtual screen.
    ///
    /// @throws GradleException If the operating system is not Linux, Xvfb is not on system env 'PATH', no display from `:99` to `:198` is free, or it did not start.
    ///
    public synchronized String display() {
        if (this.display != null) {
            return this.display;
        }
        
        String system = System.getProperty("os.name", "");
        if (!system.startsWith("Linux")) {
            throw new GradleException("There's no need for Xvfb on '" + system + "', '-Pxvfb' is not necessary");
        }
        
        Path xvfb = Arrays.stream(Objects.requireNonNullElse(System.getenv("PATH"), "").split(File.pathSeparator))
                .map(directory -> Path.of(directory, "Xvfb"))
                .filter(candidate -> Files.isRegularFile(candidate) && Files.isExecutable(candidate))
                .findFirst()
                .orElseThrow(() -> new GradleException("Could not find 'Xvfb' on system env 'PATH'"));
        
        for (int number = XvfbDisplay.FIRST_DISPLAY; number <= XvfbDisplay.LAST_DISPLAY; number++) {
            if (Files.exists(Path.of("/tmp/.X" + number + "-lock")) || Files.exists(Path.of("/tmp/.X11-unix/X" + number))) {
                continue;
            }
            
            String display = ":" + number;
            Process screen;
            try {
                screen = new ProcessBuilder(xvfb.toString(), display, "-displayfd", "1", "-screen", "0", "1920x1080x24").redirectError(ProcessBuilder.Redirect.INHERIT).start();
            } catch (IOException exception) {
                throw new GradleException("Could not start '" + xvfb + "'", exception);
            }
            
            String reported = XvfbDisplay.awaitNumber(screen, display);
            // If Xvfb exits without writing the number, another server took the display after the check.
            if (reported == null) {
                continue;
            }
            
            if (!reported.equals(String.valueOf(number))) {
                screen.destroy();
                throw new GradleException("Could not start Xvfb on '" + display + "', it wrote '" + reported + "' as its display");
            }
            
            this.screen = screen;
            this.display = display;
            return display;
        }
        
        throw new GradleException("Could not start Xvfb on any display from ':" + XvfbDisplay.FIRST_DISPLAY + "' to ':" + XvfbDisplay.LAST_DISPLAY + "'");
    }
    
    ///
    /// Destroys the screen if one was started.
    ///
    @Override
    public synchronized void close() {
        if (this.screen != null) {
            this.screen.destroy();
        }
    }
}
