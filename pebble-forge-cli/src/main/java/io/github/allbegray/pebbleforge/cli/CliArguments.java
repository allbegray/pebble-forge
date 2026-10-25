package io.github.allbegray.pebbleforge.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Parses command-line arguments without an external CLI library.
 */
final class CliArguments {

    private static final Path DEFAULT_TEMPLATES = Path.of("src/main/resources/templates");
    private static final String DEFAULT_SUFFIX = ".peb";
    private static final String DEFAULT_COMPONENT_ROOT = "components";
    private static final int DEFAULT_CONTEXT_LINES = 2;

    private CliArguments() {
    }

    /**
     * Parses the arguments into options.
     *
     * @param args the command-line arguments
     * @return the parsed options
     * @throws CliUsageException if an option is unknown, missing a value, or invalid
     */
    static CliOptions parse(String[] args) {
        Deque<String> remaining = new ArrayDeque<>(List.of(args));
        Path templates = DEFAULT_TEMPLATES;
        String suffix = DEFAULT_SUFFIX;
        String template = null;
        String inlineModel = null;
        Path modelFile = null;
        String componentRoot = DEFAULT_COMPONENT_ROOT;
        int contextLines = DEFAULT_CONTEXT_LINES;
        boolean strict = true;
        Path output = null;
        boolean help = false;

        while (!remaining.isEmpty()) {
            String argument = remaining.removeFirst();
            String option = argument;
            String inlineValue = null;
            int equals = argument.indexOf('=');
            if (equals > 0) {
                option = argument.substring(0, equals);
                inlineValue = argument.substring(equals + 1);
            }

            switch (option) {
                case "-h", "--help" -> {
                    rejectValue(option, inlineValue);
                    help = true;
                }
                case "-t", "--templates" -> templates = pathValue(option, inlineValue, remaining);
                case "-s", "--suffix" -> suffix = value(option, inlineValue, remaining);
                case "-n", "--template" -> template = value(option, inlineValue, remaining);
                case "-m", "--model" -> {
                    if (modelFile != null) {
                        throw modelConflict();
                    }
                    inlineModel = value(option, inlineValue, remaining);
                }
                case "-f", "--model-file" -> {
                    if (inlineModel != null) {
                        throw modelConflict();
                    }
                    modelFile = pathValue(option, inlineValue, remaining);
                }
                case "-c", "--component-root" -> componentRoot = value(option, inlineValue, remaining);
                case "--context-lines" ->
                    contextLines = contextLines(option, value(option, inlineValue, remaining));
                case "--strict" -> {
                    rejectValue(option, inlineValue);
                    strict = true;
                }
                case "--lenient" -> {
                    rejectValue(option, inlineValue);
                    strict = false;
                }
                case "-o", "--output" -> output = pathValue(option, inlineValue, remaining);
                default -> throw new CliUsageException("Unknown option: " + argument);
            }
        }

        if (!help && (template == null || template.isBlank())) {
            throw new CliUsageException("Missing required option: --template");
        }
        return new CliOptions(templates, suffix, template, inlineModel, modelFile, componentRoot,
                contextLines, strict, output, help);
    }

    private static String value(String option, String inlineValue, Deque<String> remaining) {
        if (inlineValue != null) {
            return inlineValue;
        }
        if (remaining.isEmpty()) {
            throw new CliUsageException("Option " + option + " requires a value");
        }
        return remaining.removeFirst();
    }

    private static Path pathValue(String option, String inlineValue, Deque<String> remaining) {
        String value = value(option, inlineValue, remaining);
        if (value.isBlank()) {
            throw new CliUsageException("Option " + option + " requires a path");
        }
        try {
            return Path.of(value);
        } catch (InvalidPathException error) {
            throw new CliUsageException("Invalid path for " + option + ": " + value);
        }
    }

    private static void rejectValue(String option, String inlineValue) {
        if (inlineValue != null) {
            throw new CliUsageException("Option " + option + " does not take a value");
        }
    }

    private static int contextLines(String option, String value) {
        try {
            int lines = Integer.parseInt(value);
            if (lines < 0) {
                throw new CliUsageException(option + " must not be negative: " + value);
            }
            return lines;
        } catch (NumberFormatException error) {
            throw new CliUsageException(option + " must be an integer: " + value);
        }
    }

    private static CliUsageException modelConflict() {
        return new CliUsageException("--model and --model-file cannot be used together");
    }
}
