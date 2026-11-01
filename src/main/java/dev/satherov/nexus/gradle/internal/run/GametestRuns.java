package dev.satherov.nexus.gradle.internal.run;

import lombok.experimental.UtilityClass;

import dev.satherov.nexus.gradle.api.GametestExtension;
import dev.satherov.nexus.gradle.api.NexusExtension;
import dev.satherov.nexus.gradle.internal.client.XvfbDisplay;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyCollector;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
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
    /// The main class of the client run.
    ///
    private static final String CLIENT_MAIN_CLASS = "dev.satherov.nexus.gametest.GametestClient";
    
    ///
    /// Registers the `gameTestServer` and `gameTestClient` runs on moddev,
    /// and a `gameTestClient<Name>` run for every entry of [GametestExtension#getClients()], each with:
    /// - Its game directory, `runs/gametest`, `runs/gametest-client`, or `runs/gametest-client-<name>`.
    /// - Its report at `build/reports/gametest/<side>/<side>.xml`, where `<side>` is `server`, `client`, or `client-<name>`.
    /// - Every [RunProperty] the project gives, as a system property.
    /// - The source set of the extension, or a source set named after the run for the run of an entry.
    /// - A `doFirst` that empties its report directory, creating it if missing.
    ///
    /// The server run also gets `neoforge.enableGameTest`, and `--report <path>` and, if a selector is given,
    /// `--tests <selector>` as program arguments.
    ///
    /// The client runs also get `nexus.gametest.goldens`, and under `-Pxvfb` the display of [XvfbDisplay].
    /// The main class of the client runs is `dev.satherov.nexus.gametest.GametestClient`.
    ///
    /// The goldens of `gameTestClient` come from `-Pgoldens` or else the directory of the extension, and those of an entry default to `<name>` inside them.
    /// The run of an entry also gets the system properties of the entry.
    ///
    /// @param project   The project to register the runs in.
    /// @param neoForge  The moddev extension of the project.
    /// @param extension The gametest extension of the project.
    ///
    /// @throws GradleException If an entry with an empty name is added to [GametestExtension#getClients()].
    ///
    public static void register(Project project, NeoForgeExtension neoForge, GametestExtension extension) {
        RunModel server = neoForge.getRuns().create("gameTestServer");
        server.getType().set("gameTestServer");
        server.getGameDirectory().set(project.getLayout().getProjectDirectory().dir("runs/gametest"));
        server.systemProperty("neoforge.enableGameTest", "true");
        GametestRuns.configure(project, server, "server", extension.getSourceSet());
        server.getProgramArguments().add("--report");
        server.getProgramArguments().add(server.getSystemProperties().getting(GametestRuns.REPORT));
        String selector = RunProperty.TESTS.value(project);
        if (selector != null) {
            server.getProgramArguments().addAll("--tests", selector);
        }
        
        String path = RunProperty.GOLDENS.value(project);
        Provider<Directory> goldens = path == null ? extension.getGoldens() : project.getLayout().dir(project.provider(() -> new File(path)));
        GametestRuns.configureClient(project, neoForge.getRuns().create("gameTestClient"), "client", extension.getSourceSet(), goldens);
        extension.getClients().all(entry -> {
            String name = entry.getName();
            if (name.isEmpty()) {
                throw new GradleException("Could not set up a client gametest run, an entry of '" + NexusExtension.NAME + ".gametest.clients' has an empty name");
            }
            
            String runName = "gameTestClient" + GametestRuns.capitalized(name);
            entry.getGoldens().convention(goldens.map(root -> root.dir(name)));
            RunModel client = neoForge.getRuns().create(runName);
            GametestRuns.configureClient(project, client, "client-" + name, GametestRuns.createSourceSet(project, runName, extension.getSourceSet(), entry.getMods()), entry.getGoldens());
            client.getSystemProperties().putAll(entry.getSystemProperties());
        });
    }
    
    ///
    /// Creates a source set of the given name, with the given mods as `runtimeOnly` and the runtime classpath of the given gametest source set.
    ///
    /// @param project  The project to create the source set in.
    /// @param name     The name of the source set.
    /// @param gametest The gametest source set of the extension.
    /// @param mods     The mods of the entry.
    ///
    /// @return The new source set.
    ///
    private static Provider<SourceSet> createSourceSet(Project project, String name, Provider<SourceSet> gametest, DependencyCollector mods) {
        ConfigurationContainer configurations = project.getConfigurations();
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet sourceSet = sourceSets.create(name);
        Configuration runtimeClasspath = configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName());
        configurations.getByName(sourceSet.getRuntimeOnlyConfigurationName()).fromDependencyCollector(mods);
        sourceSet.setRuntimeClasspath(project.files(gametest.map(SourceSet::getRuntimeClasspath), runtimeClasspath));
        // Waits for the final gametest source set, or a library that both classpaths resolve could end up on the run's classpath twice.
        project.afterEvaluate(evaluated -> runtimeClasspath.shouldResolveConsistentlyWith(configurations.getByName(gametest.get().getRuntimeClasspathConfigurationName())));
        return sourceSets.named(name);
    }
    
    ///
    /// Gives the given run what every client run shares.
    ///
    /// @param project   The project the run is registered in.
    /// @param run       The run to configure.
    /// @param side      The side the run serves, `client` or `client-<name>`.
    /// @param sourceSet The source set the run uses.
    /// @param goldens   The directory the goldens of the run are read from and recorded into.
    ///
    private static void configureClient(Project project, RunModel run, String side, Provider<SourceSet> sourceSet, Provider<Directory> goldens) {
        run.client();
        run.getMainClass().set(GametestRuns.CLIENT_MAIN_CLASS);
        run.getGameDirectory().set(project.getLayout().getProjectDirectory().dir("runs/gametest-" + side));
        GametestRuns.configure(project, run, side, sourceSet);
        // This has to come after configure, or the `-Pgoldens` path it puts would replace the goldens of an entry.
        run.getSystemProperties().put(RunProperty.GOLDENS.key(), goldens.map(directory -> directory.getAsFile().getAbsolutePath()));
        if (project.hasProperty(GametestRuns.XVFB)) {
            Provider<XvfbDisplay> xvfb = project.getGradle().getSharedServices().registerIfAbsent(XvfbDisplay.NAME, XvfbDisplay.class);
            run.getEnvironment().put("DISPLAY", xvfb.map(XvfbDisplay::display));
            GametestRuns.task(project, run).configureEach(task -> task.usesService(xvfb));
        }
    }
    
    ///
    /// Gives the given run what every run shares.
    ///
    /// @param project   The project the run is registered in.
    /// @param run       The run to configure.
    /// @param side      The side the run serves, `server`, `client`, or `client-<name>`.
    /// @param sourceSet The source set the run uses.
    ///
    private static void configure(Project project, RunModel run, String side, Provider<SourceSet> sourceSet) {
        Provider<Directory> reports = project.getLayout().getBuildDirectory().dir("reports/gametest/" + side);
        run.getSourceSet().set(sourceSet);
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
    /// The task moddev registers for the given run.
    ///
    /// @param project The project the run is registered in.
    /// @param run     The run to get the task of.
    ///
    /// @return The task of the run, or an empty collection until moddev registers it.
    ///
    private static TaskCollection<Task> task(Project project, RunModel run) {
        String name = "run" + GametestRuns.capitalized(run.getName());
        return project.getTasks().named(name::equals);
    }
    
    ///
    /// Upper-cases the first letter of the given name.
    ///
    /// @param name The name.
    ///
    /// @return The name with its first letter upper-cased.
    ///
    private static String capitalized(String name) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
    
    ///
    /// Deletes everything inside the given directory, creating the directory if it is missing.
    ///
    /// @param directory The directory to clear.
    ///
    /// @throws UncheckedIOException If the directory could not be created, or anything inside it could not be deleted.
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
