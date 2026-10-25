package io.github.allbegray.pebbleforge.cli;

import java.nio.file.Path;

/**
 * Parsed command-line options.
 *
 * @param templatesDirectory the template root
 * @param suffix the loader suffix
 * @param templateName the template name without the suffix
 * @param inlineModel the inline JSON model, or {@code null}
 * @param modelFile the JSON or YAML model file, or {@code null}
 * @param componentRoot the component root
 * @param contextLines the source lines shown around an error
 * @param strictVariables whether missing variables fail the render
 * @param outputFile the output file, or {@code null} for stdout
 * @param help whether to print usage
 */
record CliOptions(
        Path templatesDirectory,
        String suffix,
        String templateName,
        String inlineModel,
        Path modelFile,
        String componentRoot,
        int contextLines,
        boolean strictVariables,
        Path outputFile,
        boolean help) {
}
