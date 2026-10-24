package io.github.allbegray.pebbleforge.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    private GradleRunner runner() {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("checkPebbleTemplates")
                .withPluginClasspath();
    }

    private void writeProject(String page, String component) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"),
                "rootProject.name = \"contracts-test\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.github.allbegray.pebbleforge.contracts")
                }

                pebbleForgeContracts {
                    variables.add("page")
                }
                """);

        Path templates = projectDir.resolve("src/main/resources/templates");
        Files.createDirectories(templates);
        Files.writeString(templates.resolve("page.peb"), page);
        if (component != null) {
            Path components = templates.resolve("components");
            Files.createDirectories(components);
            Files.writeString(components.resolve("card.peb"), component);
        }
    }
}
