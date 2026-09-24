package dev.satherov.nexus.gradle.internal.run;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import org.gradle.api.Project;
import org.jspecify.annotations.Nullable;

///
/// A Gradle project property that a gametest run receives as a system property.
///
@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public enum RunProperty {
    ///
    /// The selector of the tests to run.
    ///
    TESTS("tests", "nexus.gametest.tests", Kind.SELECTOR),
    
    ///
    /// `true` to run the tests in real time.
    ///
    REALTIME("realtime", "nexus.gametest.realtime", Kind.FLAG),
    
    ///
    /// `true` to show the client window.
    ///
    SHOW("show", "nexus.gametest.show", Kind.FLAG),
    
    ///
    /// The path to compare the measurements against.
    ///
    COMPARE("compare", "nexus.gametest.compare", Kind.PATH),
    
    ///
    /// The directory golden images are written into.
    ///
    GOLDENS("goldens", "nexus.gametest.goldens", Kind.PATH),
    
    ///
    /// `true` to record golden images instead of comparing against them.
    ///
    RECORD("record", "nexus.gametest.record", Kind.FLAG);
    
    ///
    /// The name of the Gradle project property, given as `-P<name>`.
    ///
    private final String projectProperty;
    
    ///
    /// The system property the run receives it as.
    ///
    private final String key;
    
    ///
    /// How the property's text becomes the run's value.
    ///
    private final Kind kind;
    
    ///
    /// The value the run receives, or `null` if the project has no such property.
    ///
    /// If a path or a selector is given empty, this will be `null` as well.
    ///
    /// @param project The project to read the property from.
    ///
    /// @return The value the run receives, or `null` if the run doesn't receive the property.
    ///
    public @Nullable String value(Project project) {
        Object given = project.findProperty(this.projectProperty);
        if (given == null) {
            return null;
        }
        
        String text = given.toString();
        if (text.isEmpty() && this.kind != Kind.FLAG) {
            return null;
        }
        
        return switch (this.kind) {
            case FLAG -> text.isEmpty() ? "true" : text;
            case PATH -> project.file(text).getAbsolutePath();
            case SELECTOR -> text.contains(":") ? text : "*:" + text;
        };
    }
    
    ///
    /// The three shapes a property's text can take.
    ///
    public enum Kind {
        ///
        /// `true` when given empty, otherwise the text as given.
        ///
        FLAG,
        
        ///
        /// The text made absolute against the project directory, or `null` if given empty.
        ///
        PATH,
        
        ///
        /// The text with `*:` in front if it has no namespace, or `null` if given empty.
        ///
        SELECTOR
    }
}
