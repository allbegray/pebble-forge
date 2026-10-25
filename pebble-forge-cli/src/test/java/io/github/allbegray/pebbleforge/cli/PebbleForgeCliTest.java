package io.github.allbegray.pebbleforge.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PebbleForgeCliTest {

    @TempDir
    Path tempDir;

    private Path templates;

    @BeforeEach
    void setUp() throws IOException {
        templates = Files.createDirectories(tempDir.resolve("templates"));
    }

    @Test
    void rendersWithAnInlineJsonModel() throws IOException {
        writeTemplate("page.peb", "Hello {{ name }}");

        Result result = run("--templates", templates.toString(), "--template", "page",
                "--model", "{\"name\":\"World\"}");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo("Hello World");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void rendersWithAYamlModelFile() throws IOException {
        writeTemplate("page.peb", "{{ page.title }}");
        Path model = tempDir.resolve("model.yaml");
        Files.writeString(model, """
                page:
                  title: Store
                """);

        Result result = run("--templates", templates.toString(), "--template", "page",
                "--model-file", model.toString());

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo("Store");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void rendersNestedMapsAndListsFromAJsonModelFile() throws IOException {
        writeTemplate("page.peb", """
                <h1>{{ page.title }}</h1>
                {% for item in page.items %}<li>{{ item.name }}={{ item.price }}</li>{% endfor %}
                """);
        Path model = tempDir.resolve("model.json");
        Files.writeString(model, """
                {"page":{"title":"Store","items":[{"name":"A","price":3},{"name":"B","price":5}]}}
                """);

        Result result = run("--templates", templates.toString(), "--template", "page",
                "--model-file", model.toString());

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout())
                .isEqualTo("<h1>Store</h1>\n<li>A=3</li><li>B=5</li>");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void missingVariablePrintsASourceFrameAndExitsOne() throws IOException {
        writeTemplate("page.peb", """
                first
                second
                {{ missing }}
                """);

        Result result = run("--templates", templates.toString(), "--template", "page");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("[missing]")
                .contains("> 3 | {{ missing }}")
                .contains("^");
    }

    @Test
    void includeAndExtendsChainPrintsBothLabels() throws IOException {
        writeTemplate("page.peb", """
                {% extends "layout" %}
                {% block body %}child{% endblock %}
                """);
        writeTemplate("layout.peb", """
                layout
                {% include "card" %}
                {% block body %}base{% endblock %}
                """);
        writeTemplate("card.peb", """
                card
                {{ missing }}
                """);

        Result result = run("--templates", templates.toString(), "--template", "page");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("extends layout")
                .contains("includes card")
                .contains("[missing]");
    }

    @Test
    void missingTopLevelTemplateExitsOneWithTheExpectedPath() {
        Result result = run("--templates", templates.toString(), "--template", "ghost");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("Pebble template [ghost] does not exist at")
                .contains("ghost.peb");
    }

    @Test
    void templateTraversalIsRejected() throws IOException {
        Files.writeString(tempDir.resolve("escape.peb"), "ESCAPED");

        Result result = run("--templates", templates.toString(), "--template", "../escape");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("Pebble template [../escape] must stay under the template root");
    }

    @Test
    void malformedJsonExitsTwo() throws IOException {
        writeTemplate("page.peb", "page");

        Result result = run("--templates", templates.toString(), "--template", "page",
                "--model", "{\"name\":");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("Could not parse JSON model [--model]")
                .contains("Run with --help for usage.");
    }

    @Test
    void unsupportedModelFileExtensionExitsTwo() throws IOException {
        writeTemplate("page.peb", "page");
        Path model = tempDir.resolve("model.txt");
        Files.writeString(model, "{}");

        Result result = run("--templates", templates.toString(), "--template", "page",
                "--model-file", model.toString());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("Unsupported model file extension")
                .contains(".json");
    }

    @Test
    void modelAndModelFileTogetherExitTwo() throws IOException {
        writeTemplate("page.peb", "page");
        Path model = tempDir.resolve("model.json");
        Files.writeString(model, "{}");

        Result result = run("--templates", templates.toString(), "--template", "page",
                "--model", "{}", "--model-file", model.toString());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("--model and --model-file cannot be used together");
    }

    @Test
    void helpPrintsUsageAndExitsZero() {
        Result result = run("--help");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout())
                .contains("Usage: pebble-forge-cli --template <name>")
                .contains("--templates")
                .contains("--model-file")
                .contains("--lenient")
                .contains("Exit codes");
    }

    @Test
    void outputWritesTheRenderedFile() throws IOException {
        writeTemplate("page.peb", "OUTPUT");
        Path output = tempDir.resolve("out/result.html");

        Result result = run("--templates", templates.toString(), "--template", "page",
                "--output", output.toString());

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).isEmpty();
        assertThat(Files.readString(output)).isEqualTo("OUTPUT");
    }

    @Test
    void lenientAllowsAMissingVariable() throws IOException {
        writeTemplate("page.peb", "before{{ missing }}after");

        Result result = run("--templates", templates.toString(), "--template", "page",
                "--lenient");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo("beforeafter");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void unknownOptionExitsTwo() {
        Result result = run("--bogus");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("Unknown option: --bogus")
                .contains("Run with --help for usage.");
    }

    private void writeTemplate(String name, String source) throws IOException {
        Path file = templates.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }

    private static Result run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
                PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
            exitCode = PebbleForgeCli.run(args, out, err);
        }
        return new Result(exitCode, stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) {
    }
}
