package io.github.allbegray.pebbleforge.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PebbleForgeDiagnosticsPluginTest {

    @TempDir
    Path projectDir;

    @Test
    void rendersNestedMapAndListModelValues() throws IOException {
        writeProject(
                Map.of("page.peb", """
                        <h1>{{ page.title }}</h1>
                        {% for item in page.items %}<li>{{ item.name }}={{ item.price }}</li>{% endfor %}
                        """),
                """
                templateName.set("page")
                model.put("page", mapOf(
                    "title" to "Store",
                    "items" to listOf(
                        mapOf("name" to "A", "price" to 3),
                        mapOf("name" to "B", "price" to 5)
                    )
                ))
                """);

        BuildResult result = runner().build();

        assertThat(result.getOutput())
                .contains("BUILD SUCCESSFUL")
                .contains("<h1>Store</h1>")
                .contains("<li>A=3</li>")
                .contains("<li>B=5</li>");
    }

    @Test
    void missingVariablePrintsSourceFrameWithCaretAndFails() throws IOException {
        writeProject(
                Map.of("page.peb", """
                        first
                        second
                        {{ missing }}
                        """),
                "templateName.set(\"page\")");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("[missing]")
                .contains("page:3")
                .contains("> 3 | {{ missing }}")
                .contains("^")
                .contains("Pebble template [page] failed to render");
    }

    @Test
    void includeAndExtendsChainPrintsBothEdges() throws IOException {
        writeProject(
                Map.of(
                        "page.peb", """
                                {% extends "layout" %}
                                {% block body %}child{% endblock %}
                                """,
                        "layout.peb", """
                                layout
                                {% include "card" %}
                                {% block body %}base{% endblock %}
                                """,
                        "card.peb", """
                                card
                                {{ missing }}
                                """),
                "templateName.set(\"page\")");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("extends layout")
                .contains("includes card")
                .contains("card:2")
                .contains("[missing]");
    }

    @Test
    void missingTopLevelTemplateFailsWithTheExpectedPath() throws IOException {
        writeProject(Map.of(), "templateName.set(\"ghost\")");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Pebble template [ghost] does not exist at")
                .contains("ghost.peb");
    }

    @Test
    void templateOptionOverridesTheConfiguredTemplate() throws IOException {
        writeProject(
                Map.of(
                        "page.peb", "PAGE_BODY",
                        "other.peb", "OTHER_BODY"),
                "templateName.set(\"page\")");

        BuildResult result = runner("--template", "other").build();

        assertThat(result.getOutput())
                .contains("BUILD SUCCESSFUL")
                .contains("OTHER_BODY")
                .doesNotContain("PAGE_BODY");
    }

    @Test
    void directRegistrationUsesDiagnosticsConventions() {
        Project project = ProjectBuilder.builder().build();

        RenderPebbleTemplateTask task = project.getTasks()
                .create("renderDirect", RenderPebbleTemplateTask.class);

        assertThat(task.getTemplateSuffix().get()).isEqualTo(".peb");
        assertThat(task.getComponentRoot().get()).isEqualTo("components");
        assertThat(task.getStrictVariables().get()).isTrue();
        assertThat(task.getContextLines().get()).isEqualTo(2);
        assertThat(task.getModel().get()).isEmpty();
        assertThat(task.getTemplateName().isPresent()).isFalse();
    }

    private GradleRunner runner(String... arguments) {
        List<String> args = new ArrayList<>();
        args.add("renderPebbleTemplate");
        for (String argument : arguments) {
            args.add(argument);
        }
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(args)
                .withPluginClasspath();
    }

    private void writeProject(Map<String, String> templates, String configuration)
            throws IOException {
        configureProject(configuration);
        Path root = projectDir.resolve("src/main/resources/templates");
        Files.createDirectories(root);
        for (Map.Entry<String, String> template : templates.entrySet()) {
            Path file = root.resolve(template.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, template.getValue());
        }
    }

    private void configureProject(String configuration) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"),
                "rootProject.name = \"diagnostics-test\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.github.allbegray.pebbleforge.diagnostics")
                }

                pebbleForgeDiagnostics {
                %s}
                """.formatted(indent(configuration)));
    }

    private static String indent(String value) {
        StringBuilder result = new StringBuilder();
        for (String line : value.split("\\R", -1)) {
            if (line.isEmpty()) {
                result.append('\n');
            } else {
                result.append("    ").append(line).append('\n');
            }
        }
        return result.toString();
    }
}
