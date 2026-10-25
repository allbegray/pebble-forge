package io.github.allbegray.pebbleforge.sandbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Resolves resource limits for a template name.
 */
@FunctionalInterface
public interface SandboxLimitsResolver {

    /**
     * Resolves the limits for a template name.
     *
     * @param templateName the template name
     * @return the limits
     */
    SandboxLimits resolve(String templateName);

    /**
     * Returns a resolver that uses one limit set for every template.
     *
     * @param limits the limits to use
     * @return the resolver
     */
    static SandboxLimitsResolver fixed(SandboxLimits limits) {
        Objects.requireNonNull(limits, "limits");
        return templateName -> limits;
    }

    /**
     * Creates a resolver builder.
     *
     * @param defaultLimits the limits used when no rule matches
     * @return a new builder
     */
    static Builder builder(SandboxLimits defaultLimits) {
        return new Builder(defaultLimits);
    }

    /**
     * Builds a {@link SandboxLimitsResolver}. Exact rules win over prefix rules, and longer
     * prefixes win over shorter prefixes.
     */
    final class Builder {

        private final SandboxLimits defaultLimits;
        private final List<Rule> rules = new ArrayList<>();

        private Builder(SandboxLimits defaultLimits) {
            this.defaultLimits = Objects.requireNonNull(defaultLimits, "defaultLimits");
        }

        /**
         * Adds a rule for one template name.
         *
         * @param templateName the exact template name
         * @param limits the limits to use
         * @return this builder
         */
        public Builder exact(String templateName, SandboxLimits limits) {
            rules.add(new Rule(normalize(templateName), limits, true));
            return this;
        }

        /**
         * Adds a rule for a template name prefix.
         *
         * @param prefix the directory or name prefix
         * @param limits the limits to use
         * @return this builder
         */
        public Builder prefix(String prefix, SandboxLimits limits) {
            String value = normalize(prefix);
            if (!value.endsWith("/")) {
                value = value + "/";
            }
            rules.add(new Rule(value, limits, false));
            return this;
        }

        /**
         * Builds the resolver.
         *
         * @return the resolver
         */
        public SandboxLimitsResolver build() {
            List<Rule> ordered = rules.stream()
                    .sorted(Comparator.comparing(Rule::exact).reversed()
                            .thenComparing(rule -> rule.value().length(),
                                    Comparator.reverseOrder()))
                    .toList();
            return templateName -> {
                String name = normalize(templateName);
                for (Rule rule : ordered) {
                    if (rule.exact() ? name.equals(rule.value()) : name.startsWith(rule.value())) {
                        return rule.limits();
                    }
                }
                return defaultLimits;
            };
        }

        private static String normalize(String value) {
            Objects.requireNonNull(value, "templateName");
            String normalized = value.trim().replace('\\', '/');
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Template name must not be empty");
            }
            return normalized;
        }
    }

    /**
     * A template name rule.
     *
     * @param value the exact name or prefix
     * @param limits the limits for the matching templates
     * @param exact whether the rule matches the whole name
     */
    record Rule(String value, SandboxLimits limits, boolean exact) {

        /**
         * Validates the rule.
         *
         * @throws NullPointerException if a value is null
         */
        public Rule {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(limits, "limits");
        }
    }
}
