package dev.satherov.nexus.gradle.internal.run;

import lombok.experimental.UtilityClass;

import dev.satherov.nexus.gradle.api.NexusGametestExtension;
import dev.satherov.nexus.gradle.internal.client.XvfbDisplay;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskCollection;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

///
/// Registers the gametest runs on moddev.
///
@UtilityClass
public class GametestRuns {

    ///
    /// The system property with the path to place the report file of the run at.
    ///
    private static final String REPORT = "nexus.gametest.report";

    ///
    /// The name of the project property that puts the client run on a virtual screen.
    ///
    private static final String XVFB = "xvfb";

    ///
    /// Registers the `gameTestServer` and `gameTestClient` runs on moddev, each with:
    /// - Its game directory, `runs/gametest` or `runs/gametest-client`.
    /// - Its report at `build/reports/gametest/<side>/<side>.xml`, where `<side>` is `server` or `client`.
    /// - Every [RunProperty] the project gives, as a system property.
    /// - The source set of the extension.
    /// - A `doFirst` that empties its report directory, creating it if missing.
    ///
    /// The server run also gets `neoforge.enableGameTest`, and `--report <path>` and, if a selector is given,
    /// `--tests <selector>` as program arguments.
    ///
    /// The client run also gets `nexus.gametest.goldens` from `-Pgoldens` or else the directory of the extension, and
    /// under `-Pxvfb` the display of [XvfbDisplay].
    ///
    /// @param project   The project to register the runs in.
    /// @param neoForge  The moddev extension of the project.
    /// @param extension The gametest extension of the project.
    ///
    public static void register(Project project, NeoForgeExtension neoForge, NexusGametestExtension extension) {
        RunModel server = neoForge.getRuns().create("gameTestServer");
        server.getType().set("gameTestServer");
        server.getGameDirectory().set(project.getLayout().getProjectDirectory().dir("runs/gametest"));
        server.systemProperty("neoforge.enableGameTest", "true");
        GametestRuns.configure(project, extension, server, "server");
        server.getProgramArguments().add("--report");
        server.getProgramArguments().add(server.getSystemProperties().getting(GametestRuns.REPORT));
        String selector = RunProperty.TESTS.value(project);
        if (selector != null) {
            server.getProgramArguments().addAll("--tests", selector);
        }

        RunModel client = neoForge.getRuns().create("gameTestClient");
        client.client();
        client.getGameDirectory().set(project.getLayout().getProjectDirectory().dir("runs/gametest-client"));
        GametestRuns.configure(project, extension, client, "client");
        if (RunProperty.GOLDENS.value(project) == null) {
            client.getSystemProperties().put(RunProperty.GOLDENS.key(), extension.getGoldens().map(goldens -> goldens.getAsFile().getAbsolutePath()));
        }

        if (!project.hasProperty(GametestRuns.XVFB)) {
            return;
        }

        Provider<XvfbDisplay> xvfb = project.getGradle().getSharedServices().registerIfAbsent(XvfbDisplay.NAME, XvfbDisplay.class);
        client.getEnvironment().put("DISPLAY", xvfb.map(XvfbDisplay::display));
        GametestRuns.task(project, client).configureEach(task -> task.usesService(xvfb));
    }

    ///
    /// Gives the given run what both runs share.
    ///
    /// @param project   The project the run is registered in.
    /// @param extension The gametest extension of the project.
    /// @param run       The run to configure.
    /// @param side      The side the run serves, `server` or `client`.
    ///
    private static void configure(Project project, NexusGametestExtension extension, RunModel run, String side) {
        Provider<Directory> reports = project.getLayout().getBuildDirectory().dir("reports/gametest/" + side);
        run.getSourceSet().set(extension.getSourceSet());
        run.getSystemProperties().put(GametestRuns.REPORT, reports.map(directory -> directory.file(side + ".xml").getAsFile().getAbsolutePath()));
        for (RunProperty property : RunProperty.values()) {
            String value = property.value(project);
            if (value != null) {
                run.getSystemProperties().put(property.key(), value);
            }
        }

        GametestRuns.task(project, run).configureEach(task -> task.doFirst(running -> GametestRuns.empty(reports.get().getAsFile())));
    }

    ///
    /// The task moddev registers for the given run, once it does.
    ///
    /// @param project The project the run is registered in.
    /// @param run     The run to get the task of.
    ///
    /// @return The task of the run, or an empty collection until moddev registers it.
    ///
    private static TaskCollection<Task> task(Project project, RunModel run) {
        String name = "run" + run.getName().substring(0, 1).toUpperCase(Locale.ROOT) + run.getName().substring(1);
        return project.getTasks().named(name::equals);
    }

    ///
    /// Deletes everything inside the given directory, creating the directory if it is missing.
    ///
    /// @param directory The directory to empty.
    ///
    /// @throws UncheckedIOException If the directory could not be created or anything inside it could not be deleted.
    ///
    private static void empty(File directory) {
        try (Stream<Path> paths = Files.walk(Files.createDirectories(directory.toPath()))) {
            List<Path> contents = paths.skip(1).sorted(Comparator.reverseOrder()).toList();
            for (Path path : contents) {
                Files.delete(path);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not empty the report directory '" + directory + "'", exception);
        }
    }
}
