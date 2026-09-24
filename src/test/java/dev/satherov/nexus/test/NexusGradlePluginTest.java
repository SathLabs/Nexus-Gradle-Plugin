package dev.satherov.nexus.test;

import dev.satherov.nexus.gradle.NexusGradlePlugin;
import dev.satherov.nexus.gradle.api.NexusGametestExtension;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;

import org.assertj.core.api.Assertions;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.capability.CapabilitySelector;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

///
/// Checks the defaults of the extension, the runs, and the wiring the plugin sets up.
///
public class NexusGradlePluginTest {

    @Test
    public void modIdDefaultsToOnlyMod() {
        Project project = NexusGradlePluginTest.project(Map.of());
        NexusGradlePluginTest.neoForge(project).getMods().create("sample");
        Assertions.assertThat(NexusGradlePluginTest.extension(project).getModId().get()).isEqualTo("sample");
    }

    @Test
    public void modIdHasNoDefaultWithSeveralMods() {
        Project project = NexusGradlePluginTest.project(Map.of());
        NexusGradlePluginTest.neoForge(project).getMods().create("sample");
        NexusGradlePluginTest.neoForge(project).getMods().create("other");
        Assertions.assertThat(NexusGradlePluginTest.extension(project).getModId().isPresent()).isFalse();
    }

    @Test
    public void sourceSetDefaultsToCreatedGametestSet() {
        Project project = NexusGradlePluginTest.project(Map.of());
        SourceSet gametest = project.getExtensions().getByType(SourceSetContainer.class).getByName("gametest");
        Assertions.assertThat(NexusGradlePluginTest.extension(project).getSourceSet().get()).isSameAs(gametest);
    }

    @Test
    public void goldensDefaultToResourcesOfSourceSet() {
        Project project = NexusGradlePluginTest.project(Map.of());
        Assertions.assertThat(NexusGradlePluginTest.extension(project).getGoldens().get().getAsFile()).isEqualTo(project.file("src/gametest/resources"));
    }

    @Test
    public void goldensFollowChosenSourceSet() {
        Project project = NexusGradlePluginTest.project(Map.of());
        NexusGametestExtension extension = NexusGradlePluginTest.extension(project);
        extension.getSourceSet().set(project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME));
        Assertions.assertThat(extension.getGoldens().get().getAsFile()).isEqualTo(project.file("src/main/resources"));
    }

    @Test
    public void harnessDefaultsToVersionlessNexusWithGametestCapability() {
        Dependency harness = NexusGradlePluginTest.extension(NexusGradlePluginTest.project(Map.of())).getHarness().get();
        Assertions.assertThat(harness).isInstanceOfSatisfying(ExternalModuleDependency.class, dependency -> {
            Assertions.assertThat(dependency.getGroup()).isEqualTo("dev.satherov.nexus");
            Assertions.assertThat(dependency.getName()).isEqualTo("nexus");
            Assertions.assertThat(dependency.getVersion()).isNull();
            Assertions.assertThat(dependency.getCapabilitySelectors())
                    .extracting(CapabilitySelector::getDisplayName)
                    .singleElement()
                    .asString()
                    .contains("dev.satherov.nexus:nexus-gametest");
        });
    }

    @Test
    public void runsAreRegistered() {
        Project project = NexusGradlePluginTest.project(Map.of());
        Assertions.assertThat(NexusGradlePluginTest.neoForge(project).getRuns().getNames()).containsExactlyInAnyOrder("gameTestServer", "gameTestClient");
    }

    @Test
    public void runsUseTheirGameDirectoriesAndTypes() {
        Project project = NexusGradlePluginTest.project(Map.of());
        RunModel server = NexusGradlePluginTest.run(project, "gameTestServer");
        RunModel client = NexusGradlePluginTest.run(project, "gameTestClient");
        Assertions.assertThat(server.getType().get()).isEqualTo("gameTestServer");
        Assertions.assertThat(client.getType().get()).isEqualTo("client");
        Assertions.assertThat(server.getGameDirectory().get().getAsFile()).isEqualTo(project.file("runs/gametest"));
        Assertions.assertThat(client.getGameDirectory().get().getAsFile()).isEqualTo(project.file("runs/gametest-client"));
    }

    @Test
    public void runsFollowSourceSetOfExtension() {
        Project project = NexusGradlePluginTest.project(Map.of());
        SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        NexusGradlePluginTest.extension(project).getSourceSet().set(main);
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestServer").getSourceSet().get()).isSameAs(main);
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestClient").getSourceSet().get()).isSameAs(main);
    }

    @Test
    public void serverRunGetsReportArguments() {
        Project project = NexusGradlePluginTest.project(Map.of());
        String report = project.file("build/reports/gametest/server/server.xml").getAbsolutePath();
        RunModel server = NexusGradlePluginTest.run(project, "gameTestServer");
        Assertions.assertThat(server.getProgramArguments().get()).containsExactly("--report", report);
        Assertions.assertThat(server.getSystemProperties().get()).containsEntry("nexus.gametest.report", report);
    }

    @Test
    public void serverRunEnablesGameTests() {
        Project project = NexusGradlePluginTest.project(Map.of());
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestServer").getSystemProperties().get()).containsEntry("neoforge.enableGameTest", "true");
    }

    @Test
    public void clientRunGetsReportProperty() {
        Project project = NexusGradlePluginTest.project(Map.of());
        String report = project.file("build/reports/gametest/client/client.xml").getAbsolutePath();
        RunModel client = NexusGradlePluginTest.run(project, "gameTestClient");
        Assertions.assertThat(client.getProgramArguments().get()).isEmpty();
        Assertions.assertThat(client.getSystemProperties().get()).containsEntry("nexus.gametest.report", report);
    }

    @Test
    public void onlyClientRunOverridesMainClass() {
        Project project = NexusGradlePluginTest.project(Map.of());
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestClient").getMainClass().get()).isEqualTo("dev.satherov.nexus.gametest.GametestClient");
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestServer").getMainClass().isPresent()).isFalse();
    }

    @Test
    public void selectorReachesBothRuns() {
        Project project = NexusGradlePluginTest.project(Map.of("tests", "sample"));
        RunModel server = NexusGradlePluginTest.run(project, "gameTestServer");
        Assertions.assertThat(server.getProgramArguments().get()).containsSubsequence("--tests", "*:sample");
        Assertions.assertThat(server.getSystemProperties().get()).containsEntry("nexus.gametest.tests", "*:sample");
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestClient").getSystemProperties().get()).containsEntry("nexus.gametest.tests", "*:sample");
    }

    @Test
    public void flagsReachBothRuns() {
        Project project = NexusGradlePluginTest.project(Map.of("realtime", "", "show", "false"));
        for (String name : List.of("gameTestServer", "gameTestClient")) {
            Assertions.assertThat(NexusGradlePluginTest.run(project, name).getSystemProperties().get())
                    .containsEntry("nexus.gametest.realtime", "true")
                    .containsEntry("nexus.gametest.show", "false");
        }
    }

    @Test
    public void missingPropertiesReachNoRun() {
        Project project = NexusGradlePluginTest.project(Map.of());
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestServer").getSystemProperties().get()).containsOnlyKeys("neoforge.enableGameTest", "nexus.gametest.report");
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestClient").getSystemProperties().get()).containsOnlyKeys("nexus.gametest.report", "nexus.gametest.goldens");
    }

    @Test
    public void clientGoldensDefaultToExtension() {
        Project project = NexusGradlePluginTest.project(Map.of());
        File goldens = project.file("goldens");
        NexusGradlePluginTest.extension(project).getGoldens().set(goldens);
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestClient").getSystemProperties().get()).containsEntry("nexus.gametest.goldens", goldens.getAbsolutePath());
    }

    @Test
    public void clientGoldensComeFromProperty() {
        Project project = NexusGradlePluginTest.project(Map.of("goldens", "given"));
        NexusGradlePluginTest.extension(project).getGoldens().set(project.file("goldens"));
        Assertions.assertThat(NexusGradlePluginTest.run(project, "gameTestClient").getSystemProperties().get()).containsEntry("nexus.gametest.goldens", project.file("given").getAbsolutePath());
    }

    @Test
    public void failsAfterEvaluationWithoutModdev() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(NexusGradlePlugin.class);
        Assertions.assertThatThrownBy(((ProjectInternal) project)::evaluate)
                .rootCause()
                .hasMessage("Could not set up the gametests, the plugin 'net.neoforged.moddev' was never applied");
    }

    @Test
    public void evaluationWiresSourceSet() {
        Project project = NexusGradlePluginTest.evaluated("sample");
        ConfigurationContainer configurations = project.getConfigurations();
        Assertions.assertThat(configurations.getByName("gametestImplementation").getExtendsFrom()).contains(configurations.getByName("implementation"));
        Assertions.assertThat(configurations.getByName("gametestCompileOnly").getExtendsFrom()).contains(configurations.getByName("compileOnly"));
        Assertions.assertThat(configurations.getByName("gametestRuntimeOnly").getExtendsFrom()).contains(configurations.getByName("runtimeOnly"));
        Assertions.assertThat(configurations.getByName("gametestImplementation").getDependencies()).contains(NexusGradlePluginTest.extension(project).getHarness().get());
    }

    @Test
    public void evaluationAddsSourceSetToMod() {
        Project project = NexusGradlePluginTest.evaluated("sample");
        SourceSet gametest = project.getExtensions().getByType(SourceSetContainer.class).getByName("gametest");
        Assertions.assertThat(NexusGradlePluginTest.neoForge(project).getMods().getByName("sample").getModSourceSets().get()).containsExactly(gametest);
    }

    @Test
    public void evaluationAddsSourceSetToChosenMod() {
        Project project = NexusGradlePluginTest.project(Map.of());
        NexusGradlePluginTest.extension(project).getModId().set("other");
        NexusGradlePluginTest.evaluate(project, "sample", "other");
        SourceSet gametest = project.getExtensions().getByType(SourceSetContainer.class).getByName("gametest");
        Assertions.assertThat(NexusGradlePluginTest.neoForge(project).getMods().getByName("other").getModSourceSets().get()).containsExactly(gametest);
        Assertions.assertThat(NexusGradlePluginTest.neoForge(project).getMods().getByName("sample").getModSourceSets().get()).isEmpty();
    }

    @Test
    public void failsAfterEvaluationWithoutModId() {
        Assertions.assertThatThrownBy(NexusGradlePluginTest::evaluated)
                .rootCause()
                .hasMessage("Could not add gametests to any mod, 'nexusGametest.modId' must be specified because moddev knows '0' mods instead of exactly one.");
    }

    @Test
    public void runTasksPrepareTheirDirectories() throws IOException {
        Project project = NexusGradlePluginTest.evaluated("sample");
        File stale = project.file("build/reports/gametest/client/stale.json");
        Files.createDirectories(stale.getParentFile().toPath());
        Files.writeString(stale.toPath(), "{}");
        NexusGradlePluginTest.runFirstActions(project, "runGameTestClient");
        Assertions.assertThat(stale).doesNotExist();
        Assertions.assertThat(stale.getParentFile()).isDirectory();
    }

    @Test
    public void runTasksCreateMissingReportDirectory() {
        Project project = NexusGradlePluginTest.evaluated("sample");
        File reports = project.file("build/reports/gametest/server");
        Assertions.assertThat(reports).doesNotExist();
        NexusGradlePluginTest.runFirstActions(project, "runGameTestServer");
        Assertions.assertThat(reports).isEmptyDirectory();
    }

    private static void runFirstActions(Project project, String name) {
        Task task = project.getTasks().getByName(name);
        List<Action<? super Task>> actions = task.getActions();
        for (Action<? super Task> action : actions.subList(0, actions.size() - 1)) {
            action.execute(task);
        }
    }

    private static Project project(Map<String, String> properties) {
        Project project = ProjectBuilder.builder().build();
        properties.forEach((name, value) -> project.getExtensions().getExtraProperties().set(name, value));
        project.getPluginManager().apply("net.neoforged.moddev");
        project.getPluginManager().apply(NexusGradlePlugin.class);
        return project;
    }

    private static Project evaluated(String... mods) {
        return NexusGradlePluginTest.evaluate(NexusGradlePluginTest.project(Map.of()), mods);
    }

    private static Project evaluate(Project project, String... mods) {
        NexusGradlePluginTest.neoForge(project).setVersion("26.1.2.109");
        for (String mod : mods) {
            NexusGradlePluginTest.neoForge(project).getMods().create(mod);
        }

        ((ProjectInternal) project).evaluate();
        return project;
    }

    private static NexusGametestExtension extension(Project project) {
        return project.getExtensions().getByType(NexusGametestExtension.class);
    }

    private static NeoForgeExtension neoForge(Project project) {
        return project.getExtensions().getByType(NeoForgeExtension.class);
    }

    private static RunModel run(Project project, String name) {
        return NexusGradlePluginTest.neoForge(project).getRuns().getByName(name);
    }
}
