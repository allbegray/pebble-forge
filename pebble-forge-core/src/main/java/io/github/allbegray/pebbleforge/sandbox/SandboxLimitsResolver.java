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

    SandboxLimits resolve(String templateName);

    static SandboxLimitsResolver fixed(SandboxLimits limits) {
        Objects.requireNonNull(limits, "limits");
        return templateName -> limits;
    }

    static Builder builder(SandboxLimits defaultLimits) {
        return new Builder(defaultLimits);
    }

    final class Builder {

        private final SandboxLimits defaultLimits;
        private final List<Rule> rules = new ArrayList<>();

        private Builder(SandboxLimits defaultLimits) {
            this.defaultLimits = Objects.requireNonNull(defaultLimits, "defaultLimits");
        }

        public Builder exact(String templateName, SandboxLimits limits) {
            rules.add(new Rule(normalize(templateName), limits, true));
            return this;
        }

        public Builder prefix(String prefix, SandboxLimits limits) {
            String value = normalize(prefix);
            if (!value.endsWith("/")) {
                value = value + "/";
            }
            rules.add(new Rule(value, limits, false));
            return this;
        }

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

    record Rule(String value, SandboxLimits limits, boolean exact) {

        public Rule {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(limits, "limits");
        }
    }
}
