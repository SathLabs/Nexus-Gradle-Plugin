package dev.satherov.nexus.gradle.api;

import org.gradle.api.Action;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Nested;

///
/// An extension to configure the runs and the nexus gametests of a mod depending on it.
///
public abstract class NexusExtension {
    
    ///
    /// The name that the extension is registered under.
    ///
    public static final String NAME = "nexus";
    
    ///
    /// The names of the players every dev server ops when they log in.
    ///
    /// Defaults to `Dev`.
    /// Set it empty to op nobody.
    ///
    /// @return The names of the players every dev server ops.
    ///
    public abstract ListProperty<String> getOps();
    
    ///
    /// The gametest settings.
    ///
    /// @return The gametest settings.
    ///
    @Nested
    public abstract GametestExtension getGametest();
    
    ///
    /// Configures the gametest settings.
    ///
    /// @param action The configuration to apply.
    ///
    public void gametest(Action<? super GametestExtension> action) {
        action.execute(this.getGametest());
    }
}
