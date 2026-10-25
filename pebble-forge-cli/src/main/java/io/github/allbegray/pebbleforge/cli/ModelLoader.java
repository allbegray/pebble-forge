package io.github.allbegray.pebbleforge.cli;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the render model from inline JSON or a JSON/YAML file.
 */
final class ModelLoader {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new YAMLMapper();
    private static final TypeReference<Map<String, Object>> MODEL_TYPE = new TypeReference<>() {
    };

    private ModelLoader() {
    }

    /**
     * Loads the model for the parsed options.
     *
     * @param options the parsed options
     * @return the model, empty when neither model option is set
     * @throws CliUsageException if a model file is missing, unsupported, or malformed
     */
    static Map<String, Object> load(CliOptions options) {
        if (options.inlineModel() != null) {
            try {
                return toModel(JSON, JSON.readTree(options.inlineModel()), "--model");
            } catch (JacksonException error) {
                throw new CliUsageException(
                        "Could not parse JSON model [--model]: " + originalMessage(error));
            }
        }

        Path file = options.modelFile();
        if (file == null) {
            return Map.of();
        }

        ObjectMapper mapper = mapperFor(file);
        if (!Files.isRegularFile(file)) {
            throw new CliUsageException("Model file does not exist: " + file);
        }
        try {
            return toModel(mapper, mapper.readTree(file), file.toString());
        } catch (JacksonException error) {
            throw new CliUsageException(
                    "Could not parse model file [" + file + "]: " + originalMessage(error));
        }
    }

    private static Map<String, Object> toModel(ObjectMapper mapper, JsonNode root, String source) {
        if (root == null || !root.isObject()) {
            throw new CliUsageException("Model root must be an object: " + source);
        }
        return mapper.convertValue(root, MODEL_TYPE);
    }

    private static ObjectMapper mapperFor(Path file) {
        Path fileName = file.getFileName();
        String name = fileName == null ? "" : fileName.toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return JSON;
        }
        if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            return YAML;
        }
        throw new CliUsageException("Unsupported model file extension for [" + file
                + "]. Use .json, .yaml, or .yml.");
    }

    private static String originalMessage(JacksonException error) {
        String message = error.getOriginalMessage();
        return message == null ? error.getMessage() : message;
    }
}
