package dev.satherov.nexus.gradle;

import dev.satherov.nexus.gradle.api.NexusGametestExtension;
import dev.satherov.nexus.gradle.internal.dependency.HarnessDependency;
import dev.satherov.nexus.gradle.internal.run.GametestRuns;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.SourceSetContainer;

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
    /// @param project The project that the plugins are applied to.
    ///
    @Override
    public void apply(Project project) {
        NexusGradlePlugin.nexusGametest(project);
    }
    
    ///
    /// Creates the nexus gametest extension.
    ///
    /// Creates the gametest source set and configures the set mod with the harness.
    ///
    /// Will fail the build if moddev was never applied.
    ///
    /// @param project The project that the plugins are applied to.
    ///
    private static void nexusGametest(Project project) {
        NexusGametestExtension extension = project.getExtensions().create(NexusGametestExtension.NAME, NexusGametestExtension.class);
        extension.getGoldens().convention(project.getLayout().dir(extension.getSourceSet().map(sourceSet -> sourceSet.getResources().getSrcDirs().iterator().next())));
        
        ExternalModuleDependency harness = project.getDependencyFactory().create("dev.satherov.nexus", "nexus", null);
        harness.capabilities(capabilities -> capabilities.requireCapability("dev.satherov.nexus:nexus-gametest"));
        extension.getHarness().convention(harness);
        
        project.getPluginManager().withPlugin(NexusGradlePlugin.MOD_DEV, moddev -> {
            NeoForgeExtension neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
            extension.getSourceSet().convention(project.getExtensions().getByType(SourceSetContainer.class).create("gametest"));
            extension.getModId().convention(project.provider(() -> {
                SortedSet<String> names = neoForge.getMods().getNames();
                return names.size() == 1 ? names.first() : null;
            }));
            
            GametestRuns.register(project, neoForge, extension);
        });
        
        project.afterEvaluate(evaluated -> {
            if (!evaluated.getPluginManager().hasPlugin(NexusGradlePlugin.MOD_DEV)) {
                throw new GradleException("Could not set up the gametests, the plugin '" + NexusGradlePlugin.MOD_DEV + "' was never applied");
            }
            
            HarnessDependency.add(evaluated, evaluated.getExtensions().getByType(NeoForgeExtension.class), extension);
        });
    }
}
