package dev.satherov.nexus.gradle.internal.run;

import lombok.experimental.UtilityClass;

import dev.satherov.nexus.gradle.api.NexusExtension;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;

import org.gradle.api.Project;

///
/// Gives every moddev run its default game directory and the players to op.
///
@UtilityClass
public class RunDefaults {
    
    ///
    /// The system property the dev servers read the names to op from.
    ///
    public static final String OPS = "nexus.dev.ops";
    
    ///
    /// Gives every moddev run, present and later, the game directory `runs/<run name>` as its convention and the
    /// extension's ops as `nexus.dev.ops`, comma separated.
    ///
    /// @param project   The project the runs are registered in.
    /// @param neoForge  The moddev extension of the project.
    /// @param extension The nexus extension of the project.
    ///
    public static void apply(Project project, NeoForgeExtension neoForge, NexusExtension extension) {
        neoForge.getRuns().configureEach(run -> {
            run.getGameDirectory().convention(project.getLayout().getProjectDirectory().dir("runs").dir(run.getName()));
            run.getSystemProperties().put(RunDefaults.OPS, extension.getOps().map(ops -> String.join(",", ops)));
        });
    }
}
