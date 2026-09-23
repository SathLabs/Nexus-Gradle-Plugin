package dev.satherov.nexus.gradle.api;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

///
/// An extension that configures the gametests of a mod and the harness they run with.
///
public abstract class NexusGametestExtension {

    ///
    /// The name the extension is registered under.
    ///
    public static final String NAME = "nexusGametest";

    ///
    /// The identifier of the mod the gametests belong to.
    ///
    /// Defaults to the only mod moddev knows.
    ///
    /// Required if moddev knows several mods.
    /// If moddev knows none and this is unset, the build will fail after evaluation.
    ///
    /// @return The identifier of the mod the gametests belong to.
    ///
    public abstract Property<String> getModId();

    ///
    /// The source set the gametests live in.
    ///
    /// Defaults to the `gametest` source set the plugin creates when applied.
    ///
    /// The chosen set is compiled against `main` and the harness and added to the mod.
    /// If the build script adds it to a mod as well, moddev will fail on the duplicate source set.
    ///
    /// @return The source set the gametests live in.
    ///
    public abstract Property<SourceSet> getSourceSet();

    ///
    /// The directory `-Precord` writes goldens into.
    ///
    /// Defaults to the resources directory of the source set.
    ///
    /// @return The directory `-Precord` writes goldens into.
    ///
    public abstract DirectoryProperty getGoldens();

    ///
    /// The harness dependency added to the source set.
    ///
    /// Defaults to `dev.satherov.nexus:nexus` without a version and with the `dev.satherov.nexus:nexus-gametest`
    /// capability, so the version is the one of the project's own nexus dependency.
    ///
    /// @return The harness dependency added to the source set.
    ///
    public abstract Property<Dependency> getHarness();
}
