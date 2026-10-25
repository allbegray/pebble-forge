package io.github.allbegray.pebbleforge.gradle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Loads a JSON or YAML contract schema file.
 */
final class SchemaFileLoader {

    private static final Set<String> ALLOWED_FIELDS =
            Set.of("variables", "allowAnyVariable", "$schema");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

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
        } catch (JsonProcessingException e) {
            throw new GradleException("Could not parse schema file [" + file + "]: "
                    + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new GradleException("Could not read schema file [" + file + "]", e);
        }

        if (root == null) {
            return Schema.empty();
        }
        if (!root.isObject()) {
            throw new GradleException("Schema file root must be an object: " + file);
        }

        for (Iterator<String> fields = root.fieldNames(); fields.hasNext(); ) {
            String field = fields.next();
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new GradleException("Unknown schema field [" + field + "] in " + file
                        + ". Allowed fields are variables, allowAnyVariable, and $schema.");
            }
        }

        Set<String> variables = new LinkedHashSet<>();
        JsonNode variablesNode = root.get("variables");
        if (variablesNode != null && !variablesNode.isNull()) {
            if (!variablesNode.isArray()) {
                throw new GradleException(
                        "Schema field [variables] must be an array in " + file);
            }
            int index = 0;
            for (JsonNode variable : variablesNode) {
                if (!variable.isTextual() || variable.textValue().isBlank()) {
                    throw new GradleException("Schema variable at index " + index
                            + " must be a non-blank string in " + file);
                }
                variables.add(variable.textValue().trim());
                index++;
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

        return new Schema(Collections.unmodifiableSet(variables), allowAnyVariable);
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
     * The variables and permissive flag from a schema file.
     *
     * @param variables the declared variable names
     * @param allowAnyVariable whether every variable is allowed
     */
    record Schema(Set<String> variables, boolean allowAnyVariable) {

        /**
         * Returns an empty schema.
         *
         * @return a schema with no variables and no permissive flag
         */
        static Schema empty() {
            return new Schema(Set.of(), false);
        }
    }
}
