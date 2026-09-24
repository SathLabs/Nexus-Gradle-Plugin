package dev.satherov.nexus.gradle.internal.dependency;

import lombok.experimental.UtilityClass;

import dev.satherov.nexus.gradle.api.NexusGametestExtension;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

///
/// Wires the source set of the gametests into the mod and the harness.
///
@UtilityClass
public class HarnessDependency {
    
    ///
    /// Wires the source set of the extension into the mod:
    /// - Its `implementation`, `compileOnly`, and `runtimeOnly` extend the ones of `main`, so the mod's own libraries
    ///   and the versioned nexus dependency reach it.
    /// - The output of `main` joins its classpaths.
    /// - The modding dependencies of moddev and the harness dependency of the extension are added to it.
    /// - It is added to the mod named by the extension.
    ///
    /// Should only ever be called after the project is evaluated.
    ///
    /// @param project   The project the source set belongs to.
    /// @param neoForge  The moddev extension of the project.
    /// @param extension The gametest extension of the project.
    ///
    /// @throws GradleException If the extension has no mod identifier.
    ///
    public static void add(Project project, NeoForgeExtension neoForge, NexusGametestExtension extension) {
        String modId = extension.getModId().getOrNull();
        if (modId == null) {
            throw new GradleException("Could not add gametests to any mod, '" + NexusGametestExtension.NAME + ".modId' must be specified because moddev knows '" + neoForge.getMods().size() + "' mods instead of exactly one.");
        }
        
        SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSet sourceSet = extension.getSourceSet().get();
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.getByName(sourceSet.getImplementationConfigurationName()).extendsFrom(configurations.getByName(main.getImplementationConfigurationName()));
        configurations.getByName(sourceSet.getCompileOnlyConfigurationName()).extendsFrom(configurations.getByName(main.getCompileOnlyConfigurationName()));
        configurations.getByName(sourceSet.getRuntimeOnlyConfigurationName()).extendsFrom(configurations.getByName(main.getRuntimeOnlyConfigurationName()));
        
        sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(main.getOutput()));
        sourceSet.setRuntimeClasspath(sourceSet.getRuntimeClasspath().plus(main.getOutput()));
        
        neoForge.addModdingDependenciesTo(sourceSet);
        project.getDependencies().addProvider(sourceSet.getImplementationConfigurationName(), extension.getHarness());
        neoForge.getMods().getByName(modId).sourceSet(sourceSet);
    }
}
