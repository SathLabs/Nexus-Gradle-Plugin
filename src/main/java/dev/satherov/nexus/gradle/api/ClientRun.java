package dev.satherov.nexus.gradle.api;

import org.gradle.api.Named;
import org.gradle.api.artifacts.dsl.Dependencies;
import org.gradle.api.artifacts.dsl.DependencyCollector;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;

///
/// An extra client gametest run with its own mods, system properties, and goldens.
///
/// Created for every entry of [GametestExtension#getClients()].
/// The run is set up like `gameTestClient` and gets the same `-P` properties, the players to op, and the virtual display under `-Pxvfb`.
///
/// ```groovy
/// nexus {
///     gametest {
///         clients {
///             shaders {
///                 mods libs.sodium
///                 systemProperties.put('key', 'value')
///             }
///         }
///     }
/// }
/// ```
///
public abstract class ClientRun implements Named, Dependencies {

    ///
    /// The name of the run, the `<name>` in its task, directory, and report side.
    ///
    /// An entry `<name>` gets:
    /// - The run `gameTestClient<Name>`, with the first letter upper-cased, and its task `runGameTestClient<Name>`.
    /// - The game directory `runs/gametest-client-<name>`.
    /// - The report `build/reports/gametest/client-<name>/client-<name>.xml`, in a directory the run empties before it starts.
    /// - A source set `gameTestClient<Name>` of its own.
    ///
    /// @return The name of the run.
    ///
    @Override
    public abstract String getName();

    ///
    /// The mods put on the runtime classpath of the run.
    ///
    /// The classpath of the run is the runtime classpath of the gametest source set and these mods, which are on this run only.
    ///
    /// @return The mods of the run.
    ///
    public abstract DependencyCollector getMods();

    ///
    /// The system properties the run receives beside the shared ones.
    ///
    /// If the plugin already set a key on the run, this will replace it.
    ///
    /// @return The system properties of the run.
    ///
    public abstract MapProperty<String, String> getSystemProperties();

    ///
    /// The directory the goldens of the run are read from and recorded into.
    ///
    /// Defaults to `<name>` inside the `-Pgoldens` path, or inside [GametestExtension#getGoldens()] if the property is not given.
    ///
    /// @return The goldens directory of the run.
    ///
    public abstract DirectoryProperty getGoldens();
}
