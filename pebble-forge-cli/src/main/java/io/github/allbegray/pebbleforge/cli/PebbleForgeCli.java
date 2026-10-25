package io.github.allbegray.pebbleforge.cli;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.diagnostics.TemplateDiagnostics;
import io.github.allbegray.pebbleforge.trace.TemplateTrace;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.loader.FileLoader;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Renders one Pebble Forge template with a JSON or YAML model and prints source-frame diagnostics.
 *
 * <p>The CLI is a development tool. It builds the engine without a sandbox, so run it only with
 * templates you trust.
 */
public final class PebbleForgeCli {

    private static final String EXIT_HINT = "Run with --help for usage.";
    private static final String USAGE = """
            Usage: pebble-forge-cli --template <name> [options]

            Renders one Pebble Forge template with a JSON or YAML model and prints source-frame
            diagnostics when rendering fails.

            Options:
              -t, --templates <dir>      Template root (default: src/main/resources/templates)
              -s, --suffix <suffix>      Loader suffix (default: .peb)
              -n, --template <name>      Template name without the suffix (required)
              -m, --model <json>         Inline JSON model
              -f, --model-file <file>    JSON or YAML model file
              -c, --component-root <dir> Component root (default: components)
                  --context-lines <n>    Source lines around an error (default: 2)
                  --strict               Fail on missing variables (default)
                  --lenient              Render missing variables as empty values
              -o, --output <file>        Write the rendered output to a file
              -h, --help                 Print this usage and exit

            Exit codes: 0 success, 1 render failure, 2 usage or model error.
            """;

    private PebbleForgeCli() {
    }

    /**
     * Runs the CLI and exits with its status.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        int exitCode = run(args, out, err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        CliOptions options;
        Map<String, Object> model;
        try {
            options = CliArguments.parse(args);
            if (options.help()) {
                out.print(USAGE);
                out.flush();
                return 0;
            }
            model = ModelLoader.load(options);
        } catch (CliUsageException error) {
            err.println(error.getMessage());
            err.println(EXIT_HINT);
            return 2;
        }
        return render(options, model, out, err);
    }

    private static int render(CliOptions options, Map<String, Object> model,
            PrintStream out, PrintStream err) {
        Path root = options.templatesDirectory().toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            err.println("Template directory does not exist: " + root);
            return 1;
        }

        Path templateFile = root.resolve(options.templateName() + options.suffix()).normalize();
        if (!templateFile.startsWith(root)) {
            err.println("Pebble template [" + options.templateName()
                    + "] must stay under the template root: " + root);
            return 1;
        }
        if (!Files.isRegularFile(templateFile)) {
            err.println("Pebble template [" + options.templateName() + "] does not exist at "
                    + templateFile + ". Set the template name relative to the template root "
                    + "without the suffix.");
            return 1;
        }

        FileLoader loader = new FileLoader(root.toString());
        loader.setSuffix(options.suffix());
        loader.setCharset(StandardCharsets.UTF_8.name());

        PebbleEngine engine;
        try {
            Components components = Components.builder()
                    .root(options.componentRoot())
                    .suffix("")
                    .build();
            engine = PebbleForge.engine()
                    .loader(loader)
                    .components(components)
                    .strictVariables(options.strictVariables())
                    .languageExtensions(true)
                    .cacheActive(true)
                    .build();
        } catch (IllegalArgumentException | IllegalStateException error) {
            err.println(error.getMessage());
            err.println(EXIT_HINT);
            return 2;
        }

        TemplateDiagnostics diagnostics = TemplateDiagnostics.builder()
                .loader(loader)
                .contextLines(options.contextLines())
                .build();

        StringWriter buffer = new StringWriter();
        try {
            TemplateTrace.run(() -> engine.getTemplate(options.templateName()).evaluate(buffer, model));
        } catch (PebbleException error) {
            err.println(diagnostics.render(error).format());
            return 1;
        } catch (IOException error) {
            err.println("Could not render Pebble template [" + options.templateName()
                    + "]: " + error.getMessage());
            return 1;
        } catch (RuntimeException error) {
            err.println(diagnostics.render(error).format());
            return 1;
        }

        return write(options, buffer.toString(), out, err);
    }

    private static int write(CliOptions options, String rendered, PrintStream out, PrintStream err) {
        Path output = options.outputFile();
        if (output == null) {
            out.print(rendered);
            out.flush();
            return 0;
        }
        try {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, rendered, StandardCharsets.UTF_8);
            return 0;
        } catch (IOException error) {
            err.println("Could not write output file [" + output + "]: " + error.getMessage());
            return 1;
        }
    }
}
