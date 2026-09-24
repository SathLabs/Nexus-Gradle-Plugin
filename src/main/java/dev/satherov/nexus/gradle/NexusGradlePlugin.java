package dev.satherov.nexus.gradle;

import dev.satherov.nexus.gradle.api.GametestExtension;
import dev.satherov.nexus.gradle.api.NexusExtension;
import dev.satherov.nexus.gradle.internal.dependency.HarnessDependency;
import dev.satherov.nexus.gradle.internal.run.GametestRuns;
import dev.satherov.nexus.gradle.internal.run.RunDefaults;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.List;
import java.util.SortedSet;

///
/// The gradle plugin to go along with the nexus library.
///
public class NexusGradlePlugin implements Plugin<Project> {
    
    ///
    /// The identifier of the moddev plugin.
    ///
    private static final String MOD_DEV = "net.neoforged.moddev";
    
    ///
    /// Creates all extensions of this plugin.
    ///
    /// Creates the gametest source set and configures the set mod with the harness.
    /// Gives every moddev run its default game directory and the players to op.
    ///
    /// Will fail the build if moddev was never applied.
    ///
    /// @param project The project that the plugins are applied to.
    ///
    @Override
    public void apply(Project project) {
        NexusExtension extension = project.getExtensions().create(NexusExtension.NAME, NexusExtension.class);
        extension.getOps().convention(List.of("Dev"));
        
        GametestExtension gametest = extension.getGametest();
        gametest.getGoldens().convention(project.getLayout().dir(gametest.getSourceSet().map(sourceSet -> sourceSet.getResources().getSrcDirs().iterator().next())));
        
        ExternalModuleDependency harness = project.getDependencyFactory().create("dev.satherov.nexus", "nexus", null);
        harness.capabilities(capabilities -> capabilities.requireCapability("dev.satherov.nexus:nexus-gametest"));
        gametest.getHarness().convention(harness);
        
        project.getPluginManager().withPlugin(NexusGradlePlugin.MOD_DEV, moddev -> {
            NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
            gametest.getSourceSet().convention(project.getExtensions().getByType(SourceSetContainer.class).create("gametest"));
            gametest.getModId().convention(project.provider(() -> {
                SortedSet<String> names = neoForge.getMods().getNames();
                return names.size() == 1 ? names.first() : null;
            }));
            
            RunDefaults.apply(project, neoForge, extension);
            GametestRuns.register(project, neoForge, gametest);
        });
        
        project.afterEvaluate(evaluated -> {
            if (!evaluated.getPluginManager().hasPlugin(NexusGradlePlugin.MOD_DEV)) {
                throw new GradleException("Could not set up the gametests, the plugin '" + NexusGradlePlugin.MOD_DEV + "' was never applied");
            }
            
            HarnessDependency.add(evaluated, evaluated.getExtensions().getByType(NeoForgeExtension.class), gametest);
        });
    }
}
