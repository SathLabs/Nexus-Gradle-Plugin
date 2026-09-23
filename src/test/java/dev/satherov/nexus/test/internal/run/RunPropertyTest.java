package dev.satherov.nexus.test.internal.run;

import dev.satherov.nexus.gradle.internal.run.RunProperty;

import org.assertj.core.api.Assertions;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

///
/// Checks the value each kind of run property gives the run.
///
public class RunPropertyTest {

    @Test
    public void missingPropertyHasNoValue() {
        Project project = ProjectBuilder.builder().build();
        Assertions.assertThat(RunProperty.values()).allSatisfy(property -> Assertions.assertThat(property.value(project)).isNull());
    }

    @Test
    public void emptyFlagIsTrue() {
        Assertions.assertThat(RunProperty.REALTIME.value(RunPropertyTest.projectWith("realtime", ""))).isEqualTo("true");
    }

    @Test
    public void flagWithTextIsKept() {
        Assertions.assertThat(RunProperty.SHOW.value(RunPropertyTest.projectWith("show", "false"))).isEqualTo("false");
    }

    @Test
    public void relativePathIsResolvedAgainstProjectDirectory() {
        Project project = RunPropertyTest.projectWith("compare", "runs/baseline");
        Assertions.assertThat(RunProperty.COMPARE.value(project)).isEqualTo(project.getProjectDir().toPath().resolve("runs").resolve("baseline").toString());
    }

    @Test
    public void absolutePathIsKept(@TempDir Path directory) {
        String goldens = directory.resolve("goldens").toAbsolutePath().toString();
        Assertions.assertThat(RunProperty.GOLDENS.value(RunPropertyTest.projectWith("goldens", goldens))).isEqualTo(goldens);
    }

    @Test
    public void emptyPathHasNoValue() {
        Assertions.assertThat(RunProperty.COMPARE.value(RunPropertyTest.projectWith("compare", ""))).isNull();
    }

    @Test
    public void emptySelectorHasNoValue() {
        Assertions.assertThat(RunProperty.TESTS.value(RunPropertyTest.projectWith("tests", ""))).isNull();
    }

    @Test
    public void selectorWithoutNamespaceGetsEveryNamespace() {
        Assertions.assertThat(RunProperty.TESTS.value(RunPropertyTest.projectWith("tests", "capture_samples/*"))).isEqualTo("*:capture_samples/*");
    }

    @Test
    public void selectorWithNamespaceIsKept() {
        Assertions.assertThat(RunProperty.TESTS.value(RunPropertyTest.projectWith("tests", "nexus:capture_samples/*"))).isEqualTo("nexus:capture_samples/*");
    }

    private static Project projectWith(String property, String value) {
        Project project = ProjectBuilder.builder().build();
        project.getExtensions().getExtraProperties().set(property, value);
        return project;
    }
}
