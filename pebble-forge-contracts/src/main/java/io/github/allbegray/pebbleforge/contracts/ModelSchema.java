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

    public static Builder builder() {
        return new Builder();
    }

    public static ModelSchema any() {
        return builder().allowAnyVariable().build();
    }

    public boolean allows(String name) {
        return allowAny || variables.contains(name);
    }

    public Set<String> variables() {
        return variables;
    }

    public boolean allowsAny() {
        return allowAny;
    }

    public static final class Builder {

        private final Set<String> variables = new LinkedHashSet<>();
        private boolean allowAny;

        public Builder variable(String name) {
            variables.add(requireName(name));
            return this;
        }

        public Builder variables(String... names) {
            for (String name : names) {
                variable(name);
            }
            return this;
        }

        public Builder allowAnyVariable() {
            this.allowAny = true;
            return this;
        }

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
