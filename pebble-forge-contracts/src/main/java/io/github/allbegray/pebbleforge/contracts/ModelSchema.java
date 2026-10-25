package io.github.allbegray.pebbleforge.contracts;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The model variables a template is allowed to read.
 */
public final class ModelSchema {

    private final Map<String, ModelType> variableTypes;
    private final boolean allowAny;

    private ModelSchema(Map<String, ModelType> variableTypes, boolean allowAny) {
        this.variableTypes = Collections.unmodifiableMap(new LinkedHashMap<>(variableTypes));
        this.allowAny = allowAny;
    }

    /**
     * Creates a model schema builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a schema that allows every variable.
     *
     * @return the permissive schema
     */
    public static ModelSchema any() {
        return builder().allowAnyVariable().build();
    }

    /**
     * Returns whether the schema allows a variable.
     *
     * @param name the variable name
     * @return true when the variable may be read
     */
    public boolean allows(String name) {
        return allowAny || variableTypes.containsKey(name);
    }

    /**
     * Returns the allowed variable names.
     *
     * @return the variable names
     */
    public Set<String> variables() {
        return variableTypes.keySet();
    }

    /**
     * Returns the declared type of a variable.
     *
     * @param name the variable name
     * @return the variable type, {@link ModelType#any()} for a permissive
     *         schema, or null when the variable is not declared
     */
    public ModelType typeOf(String name) {
        Objects.requireNonNull(name, "name");
        ModelType type = variableTypes.get(name);
        if (type != null) {
            return type;
        }
        return allowAny ? ModelType.any() : null;
    }

    /**
     * Returns whether the schema allows every variable.
     *
     * @return true when any variable is allowed
     */
    public boolean allowsAny() {
        return allowAny;
    }

    /**
     * Builds a {@link ModelSchema}.
     */
    public static final class Builder {

        /**
         * Creates an empty builder. Prefer {@link ModelSchema#builder()}.
         */
        public Builder() {
        }

        private final Map<String, ModelType> variableTypes = new LinkedHashMap<>();
        private boolean allowAny;

        /**
         * Allows one variable with the any type.
         *
         * @param name the variable name
         * @return this builder
         */
        public Builder variable(String name) {
            return variable(name, ModelType.any());
        }

        /**
         * Allows one variable with a declared type.
         *
         * @param name the variable name
         * @param type the variable type
         * @return this builder
         */
        public Builder variable(String name, ModelType type) {
            variableTypes.put(requireName(name), Objects.requireNonNull(type, "type"));
            return this;
        }

        /**
         * Allows several variables with the any type.
         *
         * @param names the variable names
         * @return this builder
         */
        public Builder variables(String... names) {
            for (String name : names) {
                variable(name);
            }
            return this;
        }

        /**
         * Allows every variable.
         *
         * @return this builder
         */
        public Builder allowAnyVariable() {
            this.allowAny = true;
            return this;
        }

        /**
         * Builds the schema.
         *
         * @return the schema
         */
        public ModelSchema build() {
            return new ModelSchema(variableTypes, allowAny);
        }

        private static String requireName(String name) {
            Objects.requireNonNull(name, "name");
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Variable name must not be empty");
            }
            return trimmed;
        }
    }
}
