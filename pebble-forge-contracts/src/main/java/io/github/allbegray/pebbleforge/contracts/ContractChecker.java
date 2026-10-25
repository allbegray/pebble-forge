package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.Loader;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Checks template variable reads and component prop calls against a model schema.
 */
public final class ContractChecker {

    private final PebbleEngine engine;
    private final Components components;
    private final ModelSchema model;
    private final Set<String> rootTemplates;
    private final Set<String> partialTemplates;

    private ContractChecker(Builder builder) {
        this.components = builder.components;
        this.model = builder.model;
        this.rootTemplates = builder.rootTemplates;
        this.partialTemplates = builder.partialTemplates;
        this.engine = PebbleForge.engine()
                .loader(builder.loader)
                .components(builder.components)
                .languageExtensions(true)
                .strictVariables(false)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks one template as a root and follows its static includes.
     */
    public ContractResult check(String templateName) {
        Objects.requireNonNull(templateName, "templateName");
        return session().check(templateName);
    }

    /**
     * Checks a complete template set, inferring which templates are roots.
     * Every supplied template is parsed and structurally analyzed. Roots are
     * evaluated with an empty scope and follow their static includes.
     */
    public ContractResult checkAll(Collection<String> templateNames) {
        Objects.requireNonNull(templateNames, "templateNames");
        return session().checkAll(templateNames);
    }

    private CheckSession session() {
        return new CheckSession(engine, components, model, rootTemplates, partialTemplates);
    }

    public static final class Builder {

        private Loader<?> loader;
        private Components components = Components.defaults();
        private ModelSchema model = ModelSchema.any();
        private Set<String> rootTemplates = Set.of();
        private Set<String> partialTemplates = Set.of();

        public Builder loader(Loader<?> loader) {
            this.loader = Objects.requireNonNull(loader, "loader");
            return this;
        }

        public Builder components(Components components) {
            this.components = Objects.requireNonNull(components, "components");
            return this;
        }

        public Builder model(ModelSchema model) {
            this.model = Objects.requireNonNull(model, "model");
            return this;
        }

        /**
         * Forces the named templates to be checked as roots. Wins over
         * {@link #partialTemplates(Collection)}.
         */
        public Builder rootTemplates(Collection<String> names) {
            this.rootTemplates = copyNames(names, "rootTemplates");
            return this;
        }

        /**
         * Keeps the named templates out of root inference. They are still
         * checked when a root or component reaches them statically.
         */
        public Builder partialTemplates(Collection<String> names) {
            this.partialTemplates = copyNames(names, "partialTemplates");
            return this;
        }

        public ContractChecker build() {
            if (loader == null) {
                throw new IllegalStateException("A loader is required");
            }
            return new ContractChecker(this);
        }

        private static Set<String> copyNames(Collection<String> names, String label) {
            Objects.requireNonNull(names, label);
            Set<String> copy = new LinkedHashSet<>();
            for (String name : names) {
                Objects.requireNonNull(name, "templateName");
                String trimmed = name.trim();
                if (trimmed.isEmpty()) {
                    throw new IllegalArgumentException(
                            label + " must not contain an empty template name");
                }
                copy.add(trimmed);
            }
            return Set.copyOf(copy);
        }
    }
}
