package io.github.allbegray.pebbleforge.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PebbleForgeContractsPluginTest {

    @TempDir
    Path projectDir;

    @Test
    void passesWhenModelVariablesAreDeclared() throws IOException {
        writeProject("{{ page.title }}", null);

        BuildResult result = runner().build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    @Test
    void failsWhenAModelVariableIsMissing() throws IOException {
        writeProject("{{ missing }}", null);

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Variable [missing]")
                .contains("Pebble template contract check failed");
    }

    @Test
    void failsWhenARequiredComponentPropIsMissing() throws IOException {
        writeProject("{% component \"card\" %}{% endcomponent %}",
                "{% props title %}<h1>{{ title }}</h1>");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput()).contains("Component [card] requires prop [title]");
    }

    @Test
    void passesWhenAnIncludedPartialReadsACallerLoopVariable() throws IOException {
        writeProject(Map.of(
                "page.peb", "{% for item in items %}{% include \"partial\" %}{% endfor %}",
                "partial.peb", "{{ item }}"),
                "variables.add(\"items\")");

        BuildResult result = runner().build();

        assertThat(result.getOutput())
                .contains("BUILD SUCCESSFUL")
                .doesNotContain("Variable [item]");
    }

    @Test
    void failsWhenATrueRootReadsAMissingVariable() throws IOException {
        writeProject(Map.of("orphan.peb", "{{ missing }}"));

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("orphan:1")
                .contains("Variable [missing]");
    }

    @Test
    void failsWhenAComponentReadsAMissingVariable() throws IOException {
        writeProject(Map.of(
                "page.peb", "{% component \"card\" %}{% endcomponent %}",
                "components/card.peb", "{{ missing }}"));

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("components/card:1")
                .contains("Variable [missing]");
    }

    @Test
    void reportsAMissingComponentAtTheCallLine() throws IOException {
        writeProject(Map.of(
                "page.peb", "first line\n{% component \"ghost\" %}{% endcomponent %}"));

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("page:2")
                .contains("Component [ghost]")
                .doesNotContain("NullPointerException");
    }

    @Test
    void warnsOnADynamicIncludeTargetAndSucceeds() throws IOException {
        writeProject(Map.of("page.peb", "{% include fragment %}"));

        BuildResult result = runner().build();

        assertThat(result.getOutput())
                .contains("BUILD SUCCESSFUL")
                .contains("Include target is dynamic");
    }

    @Test
    void partialTemplatesKeepsADynamicFragmentOutOfRootInference() throws IOException {
        writeProject(Map.of(
                "page.peb", "{% for item in items %}{% include fragment %}{% endfor %}",
                "fragment.peb", "{{ item }}"),
                "variables.add(\"items\")");

        BuildResult withoutSetting = runner().buildAndFail();
        assertThat(withoutSetting.getOutput()).contains("Variable [item]");

        configureProject("variables.add(\"items\")", "partialTemplates.add(\"fragment\")");

        BuildResult withSetting = runner().build();
        assertThat(withSetting.getOutput()).contains("BUILD SUCCESSFUL");
    }

    @Test
    void rootTemplatesForcesADualRoleTemplateToBeCheckedAsARoot() throws IOException {
        writeProject(Map.of(
                "layout.peb", "{% for item in items %}{% include \"snippet\" %}{% endfor %}",
                "snippet.peb", "{{ item }}"),
                "variables.add(\"items\")");

        BuildResult inferred = runner().build();
        assertThat(inferred.getOutput()).contains("BUILD SUCCESSFUL");

        configureProject("variables.add(\"items\")", "rootTemplates.add(\"snippet\")");

        BuildResult forced = runner().buildAndFail();
        assertThat(forced.getOutput())
                .contains("snippet:1")
                .contains("Variable [item]");
    }

    @Test
    void componentListedInPartialTemplatesIsStillCheckedAsAComponent() throws IOException {
        writeProject(Map.of(
                "page.peb", "{% component \"card\" %}{% endcomponent %}",
                "components/card.peb", "{{ missing }}"),
                "partialTemplates.add(\"components/card\")");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("listed in partialTemplates")
                .contains("checked as a component")
                .contains("components/card:1")
                .contains("Variable [missing]");
    }

    @Test
    void failsWhenAConfiguredTemplateNameIsUnknown() throws IOException {
        writeProject(Map.of("page.peb", "{{ page.title }}"),
                "rootTemplates.add(\"ghost\")");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Unknown template [ghost] in rootTemplates")
                .contains("with the template suffix removed");
    }

    private GradleRunner runner() {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("checkPebbleTemplates")
                .withPluginClasspath();
    }

    private void writeProject(String page, String component) throws IOException {
        Map<String, String> templates = new LinkedHashMap<>();
        templates.put("page.peb", page);
        if (component != null) {
            templates.put("components/card.peb", component);
        }
        writeProject(templates);
    }

    private void writeProject(Map<String, String> templates, String... configuration)
            throws IOException {
        configureProject(configuration);
        Path root = projectDir.resolve("src/main/resources/templates");
        for (Map.Entry<String, String> template : templates.entrySet()) {
            Path file = root.resolve(template.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, template.getValue());
        }
    }

    private void configureProject(String... configuration) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"),
                "rootProject.name = \"contracts-test\"\n");
        StringBuilder extra = new StringBuilder();
        for (String line : configuration) {
            extra.append("    ").append(line).append('\n');
        }
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.github.allbegray.pebbleforge.contracts")
                }

                pebbleForgeContracts {
                    variables.add("page")
                %s}
                """.formatted(extra));
    }
}
