package io.github.allbegray.pebbleforge.gradle;

import io.github.allbegray.pebbleforge.contracts.ModelType;
import org.gradle.api.GradleException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads a JSON or YAML contract schema file.
 */
final class SchemaFileLoader {

    private static final Set<String> ALLOWED_FIELDS =
            Set.of("variables", "allowAnyVariable", "$schema");
    private static final Set<String> TYPE_FIELDS =
            Set.of("type", "properties", "required", "items");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new YAMLMapper();

    private SchemaFileLoader() {
    }

    /**
     * Loads a schema file, selecting the parser from the file extension.
     *
     * @param file the JSON or YAML schema file
     * @return the loaded schema
     */
    static Schema load(File file) {
        ObjectMapper mapper = mapperFor(file);
        if (!file.isFile()) {
            throw new GradleException("Schema file does not exist: " + file);
        }

        JsonNode root;
        try {
            root = mapper.readTree(file);
        } catch (JacksonException e) {
            throw new GradleException("Could not parse schema file [" + file + "]: "
                    + e.getOriginalMessage(), e);
        }

        if (root == null) {
            return Schema.empty();
        }
        if (!root.isObject()) {
            throw new GradleException("Schema file root must be an object: " + file);
        }

        for (Map.Entry<String, JsonNode> field : root.properties()) {
            if (!ALLOWED_FIELDS.contains(field.getKey())) {
                throw new GradleException("Unknown schema field [" + field.getKey() + "] in "
                        + file + ". Allowed fields are variables, allowAnyVariable, and $schema.");
            }
        }

        Map<String, ModelType> variableTypes = new LinkedHashMap<>();
        Map<String, Set<String>> required = new LinkedHashMap<>();
        JsonNode variablesNode = root.get("variables");
        if (variablesNode != null && !variablesNode.isNull()) {
            if (variablesNode.isArray()) {
                loadLegacyVariables(variablesNode, variableTypes, file);
            } else if (variablesNode.isObject()) {
                loadTypedVariables(variablesNode, variableTypes, required, file);
            } else {
                throw new GradleException(
                        "Schema field [variables] must be an array or an object in " + file);
            }
        }

        boolean allowAnyVariable = false;
        JsonNode allowNode = root.get("allowAnyVariable");
        if (allowNode != null && !allowNode.isNull()) {
            if (!allowNode.isBoolean()) {
                throw new GradleException(
                        "Schema field [allowAnyVariable] must be a boolean in " + file);
            }
            allowAnyVariable = allowNode.booleanValue();
        }

        return new Schema(Collections.unmodifiableMap(variableTypes),
                Collections.unmodifiableMap(required), allowAnyVariable);
    }

    private static void loadLegacyVariables(JsonNode variablesNode,
            Map<String, ModelType> variableTypes, File file) {
        int index = 0;
        for (JsonNode variable : variablesNode) {
            if (!variable.isString() || variable.stringValue().isBlank()) {
                throw new GradleException("Schema variable at index " + index
                        + " must be a non-blank string in " + file);
            }
            variableTypes.put(variable.stringValue().trim(), ModelType.any());
            index++;
        }
    }

    private static void loadTypedVariables(JsonNode variablesNode,
            Map<String, ModelType> variableTypes, Map<String, Set<String>> required, File file) {
        for (Map.Entry<String, JsonNode> variable : variablesNode.properties()) {
            String name = variable.getKey();
            if (name == null || name.isBlank()) {
                throw new GradleException("Schema variable name at variables"
                        + " must be a non-blank string in " + file);
            }
            variableTypes.put(name, parseType(variable.getValue(), "variables." + name,
                    required, file));
        }
    }

    private static ModelType parseType(JsonNode node, String path,
            Map<String, Set<String>> required, File file) {
        if (node == null || node.isNull()) {
            return ModelType.any();
        }
        if (node.isString()) {
            return namedType(node.stringValue(), path, file);
        }
        if (!node.isObject()) {
            throw new GradleException("Schema type at " + path
                    + " must be a string or an object in " + file);
        }

        for (Map.Entry<String, JsonNode> field : node.properties()) {
            if (!TYPE_FIELDS.contains(field.getKey())) {
                throw new GradleException("Unknown schema type field [" + field.getKey()
                        + "] at " + path + " in " + file
                        + ". Allowed fields are type, properties, required, and items.");
            }
        }

        String typeName = "any";
        JsonNode typeNode = node.get("type");
        if (typeNode != null && !typeNode.isNull()) {
            if (!typeNode.isString() || typeNode.stringValue().isBlank()) {
                throw new GradleException("Schema type field [type] at " + path
                        + " must be a non-blank string in " + file);
            }
            typeName = typeNode.stringValue().trim();
        }

        return switch (typeName) {
            case "any" -> {
                rejectFields(node, path, "any", file);
                yield ModelType.any();
            }
            case "string" -> {
                rejectFields(node, path, "string", file);
                yield ModelType.string();
            }
            case "number" -> {
                rejectFields(node, path, "number", file);
                yield ModelType.number();
            }
            case "boolean" -> {
                rejectFields(node, path, "boolean", file);
                yield ModelType.booleanType();
            }
            case "object" -> parseObject(node, path, required, file);
            case "array" -> parseArray(node, path, required, file);
            default -> throw new GradleException("Unknown schema type [" + typeName
                    + "] at " + path + " in " + file
                    + ". Use any, string, number, boolean, object, or array.");
        };
    }

    private static ModelType namedType(String value, String path, File file) {
        String typeName = value.trim();
        return switch (typeName) {
            case "any" -> ModelType.any();
            case "string" -> ModelType.string();
            case "number" -> ModelType.number();
            case "boolean" -> ModelType.booleanType();
            case "object" -> ModelType.object(Map.of());
            case "array" -> ModelType.array(ModelType.any());
            default -> throw new GradleException("Unknown schema type [" + typeName
                    + "] at " + path + " in " + file
                    + ". Use any, string, number, boolean, object, or array.");
        };
    }

    private static ModelType parseObject(JsonNode node, String path,
            Map<String, Set<String>> required, File file) {
        rejectField(node, "items", path, "object", file);

        Map<String, ModelType> properties = new LinkedHashMap<>();
        JsonNode propertiesNode = node.get("properties");
        if (propertiesNode != null && !propertiesNode.isNull()) {
            if (!propertiesNode.isObject()) {
                throw new GradleException("Schema type field [properties] at " + path
                        + " must be an object in " + file);
            }
            for (Map.Entry<String, JsonNode> property : propertiesNode.properties()) {
                String name = property.getKey();
                if (name == null || name.isBlank()) {
                    throw new GradleException("Schema property name at " + path
                            + ".properties must be a non-blank string in " + file);
                }
                properties.put(name, parseType(property.getValue(),
                        path + ".properties." + name, required, file));
            }
        }

        JsonNode requiredNode = node.get("required");
        if (requiredNode != null && !requiredNode.isNull()) {
            if (!requiredNode.isArray()) {
                throw new GradleException("Schema type field [required] at " + path
                        + " must be an array of strings in " + file);
            }
            Set<String> names = new LinkedHashSet<>();
            int index = 0;
            for (JsonNode entry : requiredNode) {
                if (!entry.isString() || entry.stringValue().isBlank()) {
                    throw new GradleException("Schema required entry at " + path
                            + ".required index " + index
                            + " must be a non-blank string in " + file);
                }
                names.add(entry.stringValue().trim());
                index++;
            }
            required.put(path, Collections.unmodifiableSet(names));
        }

        return ModelType.object(properties);
    }

    private static ModelType parseArray(JsonNode node, String path,
            Map<String, Set<String>> required, File file) {
        rejectField(node, "properties", path, "array", file);
        rejectField(node, "required", path, "array", file);
        return ModelType.array(parseType(node.get("items"), path + ".items", required, file));
    }

    private static void rejectFields(JsonNode node, String path, String typeName, File file) {
        rejectField(node, "properties", path, typeName, file);
        rejectField(node, "required", path, typeName, file);
        rejectField(node, "items", path, typeName, file);
    }

    private static void rejectField(JsonNode node, String field, String path,
            String typeName, File file) {
        if (node.get(field) == null) {
            return;
        }
        throw new GradleException("Schema type field [" + field + "] at " + path
                + " is not valid for type [" + typeName + "] in " + file);
    }

    private static ObjectMapper mapperFor(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return JSON;
        }
        if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            return YAML;
        }
        throw new GradleException("Unsupported schema file extension for [" + file
                + "]. Use .json, .yaml, or .yml.");
    }

    /**
     * The variables, types, and permissive flag from a schema file.
     *
     * @param variableTypes the declared type of each variable
     * @param required the required property names per type path, parsed but
     *        not enforced yet
     * @param allowAnyVariable whether every variable is allowed
     */
    record Schema(Map<String, ModelType> variableTypes, Map<String, Set<String>> required,
                  boolean allowAnyVariable) {

        /**
         * Returns the declared variable names.
         *
         * @return the variable names
         */
        Set<String> variables() {
            return variableTypes.keySet();
        }

        /**
         * Returns an empty schema.
         *
         * @return a schema with no variables and no permissive flag
         */
        static Schema empty() {
            return new Schema(Map.of(), Map.of(), false);
        }
    }
}
