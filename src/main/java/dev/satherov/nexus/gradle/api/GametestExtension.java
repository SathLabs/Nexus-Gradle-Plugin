package dev.satherov.nexus.gradle.api;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

///
/// An extension to configure the nexus gametests of a mod depending on it.
///
/// @see NexusExtension#getGametest()
///
public abstract class GametestExtension {
    
    ///
    /// The identifier of the mod that the gametests belong to.
    ///
    /// Defaults to the first mod that moddev knows.
    ///
    /// If moddev knows multiple mods or none, this must be set or the build will fail.
    ///
    /// @return The identifier of the mod the gametests belong to.
    ///
    public abstract Property<String> getModId();
    
    ///
    /// The source set that the gametests are placed in.
    ///
    /// Defaults to a `gametest` source set created by the plugin.
    ///
    /// @return The source set the gametests live in.
    ///
    public abstract Property<SourceSet> getSourceSet();
    
    ///
    /// The directory that `-Precord` writes the goldens into.
    ///
    /// Defaults to the resources directory of the source set.
    ///
    /// @return The directory that `-Precord` writes the goldens into.
    ///
    public abstract DirectoryProperty getGoldens();
    
    ///
    /// The harness dependency added to the source set.
    ///
    /// Defaults to `dev.satherov.nexus:nexus` with the `dev.satherov.nexus:nexus-gametest` capability.
    /// Does not set a version itself, so the implementing mod must set one itself.
    ///
    /// @return The harness dependency added to the source set.
    ///
    public abstract Property<Dependency> getHarness();
}
