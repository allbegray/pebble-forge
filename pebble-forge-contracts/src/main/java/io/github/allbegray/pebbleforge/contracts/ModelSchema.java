package io.github.allbegray.pebbleforge.contracts;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The model variables a template is allowed to read.
 */
public final class ModelSchema {

    private final Set<String> variables;
    private final boolean allowAny;

    private ModelSchema(Set<String> variables, boolean allowAny) {
        this.variables = Set.copyOf(variables);
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
        return allowAny || variables.contains(name);
    }

    /**
     * Returns the allowed variable names.
     *
     * @return the variable names
     */
    public Set<String> variables() {
        return variables;
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

        private final Set<String> variables = new LinkedHashSet<>();
        private boolean allowAny;

        /**
         * Allows one variable.
         *
         * @param name the variable name
         * @return this builder
         */
        public Builder variable(String name) {
            variables.add(requireName(name));
            return this;
        }

        /**
         * Allows several variables.
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
            return new ModelSchema(variables, allowAny);
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
