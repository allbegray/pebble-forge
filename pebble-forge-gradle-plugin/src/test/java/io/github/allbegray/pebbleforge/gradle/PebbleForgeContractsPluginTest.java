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
        writeProject(Map.of("page.peb", "{% include fragment %}"),
                "variables.add(\"fragment\")");

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
                "variables.add(\"items\")", "variables.add(\"fragment\")");

        BuildResult withoutSetting = runner().buildAndFail();
        assertThat(withoutSetting.getOutput()).contains("Variable [item]");

        configureProject("variables.add(\"items\")", "variables.add(\"fragment\")",
                "partialTemplates.add(\"fragment\")");

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
    void nestedComponentPartialListedAsPartialDoesNotWarn() throws IOException {
        writeProject(Map.of(
                "page.peb", "{% component \"card\" %}{% endcomponent %}",
                "components/card.peb", "{% include \"components/card/detail\" %}",
                "components/card/detail.peb", "detail"),
                "partialTemplates.add(\"components/card/detail\")");

        BuildResult result = runner().build();

        assertThat(result.getOutput())
                .contains("BUILD SUCCESSFUL")
                .doesNotContain("listed in partialTemplates");
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

    @Test
    void jsonSchemaDeclaresAVariable() throws IOException {
        writeSchemaProject("schema.json", """
                {
                  "$schema": "https://example.com/contract.schema.json",
                  "variables": ["page"],
                  "allowAnyVariable": false
                }
                """, "{{ page.title }}");

        BuildResult result = runner().build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    @Test
    void yamlSchemaDeclaresAVariable() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  - page
                allowAnyVariable: false
                """, "{{ page.title }}");

        BuildResult result = runner().build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    @Test
    void schemaVariablesDoNotIncludeUndeclaredNames() throws IOException {
        writeSchemaProject("schema.yml", """
                variables:
                  - items
                """, "{{ missing }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput()).contains("Variable [missing]");
    }

    @Test
    void schemaVariablesMergeWithTheDsl() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  - items
                """, "{{ page.title }} {{ items }}",
                "variables.add(\"page\")");

        BuildResult result = runner().build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    @Test
    void schemaAllowAnyVariableSuppressesMissingVariableErrors() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables: []
                allowAnyVariable: true
                """, "{{ missing }}");

        BuildResult result = runner().build();

        assertThat(result.getOutput())
                .contains("BUILD SUCCESSFUL")
                .doesNotContain("Variable [missing]");
    }

    @Test
    void malformedJsonFailsWithTheSchemaPath() throws IOException {
        writeSchemaProject("schema.json", "{\"variables\": [}", "{{ page.title }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Could not parse schema file")
                .contains("schema.json");
    }

    @Test
    void malformedYamlFailsWithTheSchemaPath() throws IOException {
        writeSchemaProject("schema.yaml", "variables: [page", "{{ page.title }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Could not parse schema file")
                .contains("schema.yaml");
    }

    @Test
    void unknownSchemaFieldNamesTheField() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  - page
                unexpected: true
                """, "{{ page.title }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Unknown schema field [unexpected]")
                .contains("schema.yaml");
    }

    @Test
    void nonArrayVariablesFails() throws IOException {
        writeSchemaProject("schema.yaml", "variables: page\n", "{{ page.title }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Schema field [variables] must be an array")
                .contains("schema.yaml");
    }

    @Test
    void unsupportedSchemaExtensionFails() throws IOException {
        writeSchemaProject("schema.txt", """
                variables:
                  - page
                """, "{{ page.title }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Unsupported schema file extension")
                .contains("schema.txt");
    }

    @Test
    void blankSchemaVariableFails() throws IOException {
        writeSchemaProject("schema.json", """
                {"variables": ["page", "  "]}
                """, "{{ page.title }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("must be a non-blank string")
                .contains("index 1");
    }

    @Test
    void typedYamlSchemaLoadsDeclaredProperties() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  page:
                    type: object
                    properties:
                      title: string
                      count: number
                      tags:
                        type: array
                        items: string
                  items:
                    type: array
                    items: object
                  enabled: boolean
                """, "{{ page.title }}{{ page.count }}{{ page.tags }}{{ items }}{{ enabled }}");

        BuildResult result = runner().build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    @Test
    void typedYamlSchemaReportsAMissingProperty() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  page:
                    type: object
                    properties:
                      title: string
                """, "{{ page.missing }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Variable [page] has no property [missing]")
                .contains("Pebble template contract check failed");
    }

    @Test
    void typedJsonSchemaLoads() throws IOException {
        writeSchemaProject("schema.json", """
                {
                  "variables": {
                    "page": {
                      "type": "object",
                      "properties": {
                        "title": "string",
                        "address": {
                          "type": "object",
                          "properties": {
                            "city": "string"
                          }
                        }
                      }
                    },
                    "items": {"type": "array", "items": "string"}
                  }
                }
                """, "{{ page.title }}{{ page.address.city }}{{ items }}");

        BuildResult result = runner().build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    @Test
    void invalidSchemaTypeNameNamesThePath() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  page:
                    type: mystery
                """, "{{ page.title }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Unknown schema type [mystery]")
                .contains("variables.page")
                .contains("schema.yaml");
    }

    @Test
    void unknownSchemaTypeFieldNamesTheField() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  page:
                    type: object
                    unexpected: true
                """, "{{ page.title }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Unknown schema type field [unexpected]")
                .contains("variables.page")
                .contains("schema.yaml");
    }

    @Test
    void arrayItemsWithTheWrongShapeNamesTheField() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  items:
                    type: array
                    items: 7
                """, "{{ items }}");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Schema type at variables.items.items")
                .contains("must be a string or an object")
                .contains("schema.yaml");
    }

    @Test
    void legacyArraySchemaKeepsAttributesPermissive() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  - page
                """, "{{ page.anything.goes }}");

        BuildResult result = runner().build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    @Test
    void typedFileTypeWinsOverAnUntypedDslVariable() throws IOException {
        writeSchemaProject("schema.yaml", """
                variables:
                  page:
                    type: object
                    properties:
                      title: string
                """, "{{ page.missing }}", "variables.add(\"page\")");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("Variable [page] has no property [missing]");
    }

    @Test
    void missingSchemaFileFailsClearly() throws IOException {
        writeProject("{{ page.title }}", null);
        configureProject(false, "schemaFile.set(file(\"missing.yaml\"))");

        BuildResult result = runner().buildAndFail();

        assertThat(result.getOutput())
                .contains("missing.yaml")
                .contains("does not exist");
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
        writeTemplates(templates);
    }

    private void writeSchemaProject(
            String schemaFileName, String schema, String page, String... configuration)
            throws IOException {
        writeSchemaProject(schemaFileName, schema, Map.of("page.peb", page), configuration);
    }

    private void writeSchemaProject(
            String schemaFileName, String schema, Map<String, String> templates,
            String... configuration) throws IOException {
        writeTemplates(templates);
        Files.writeString(projectDir.resolve(schemaFileName), schema);
        String[] schemaConfiguration = new String[configuration.length + 1];
        schemaConfiguration[0] = "schemaFile.set(file(\"" + schemaFileName + "\"))";
        System.arraycopy(configuration, 0, schemaConfiguration, 1, configuration.length);
        configureProject(false, schemaConfiguration);
    }

    private void writeTemplates(Map<String, String> templates) throws IOException {
        Path root = projectDir.resolve("src/main/resources/templates");
        for (Map.Entry<String, String> template : templates.entrySet()) {
            Path file = root.resolve(template.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, template.getValue());
        }
    }

    private void configureProject(String... configuration) throws IOException {
        configureProject(true, configuration);
    }

    private void configureProject(boolean declarePage, String... configuration) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"),
                "rootProject.name = \"contracts-test\"\n");
        StringBuilder extra = new StringBuilder();
        if (declarePage) {
            extra.append("    variables.add(\"page\")\n");
        }
        for (String line : configuration) {
            extra.append("    ").append(line).append('\n');
        }
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.github.allbegray.pebbleforge.contracts")
                }

                pebbleForgeContracts {
                %s}
                """.formatted(extra));
    }
}
