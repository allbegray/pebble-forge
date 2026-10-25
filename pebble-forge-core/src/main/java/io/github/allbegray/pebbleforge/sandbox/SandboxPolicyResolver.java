package io.github.allbegray.pebbleforge.sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves a sandbox policy by template name.
 */
public final class SandboxPolicyResolver {

    private final SandboxPolicy defaultPolicy;
    private final List<Rule> rules;
    private final List<SandboxPolicy> policies;

    private SandboxPolicyResolver(SandboxPolicy defaultPolicy, List<Rule> rules) {
        this.defaultPolicy = defaultPolicy;
        this.rules = rules.stream()
                .sorted(Comparator.comparing(Rule::exact).reversed()
                        .thenComparing(rule -> rule.value().length(),
                                Comparator.reverseOrder()))
                .toList();
        Set<SandboxPolicy> distinct = new LinkedHashSet<>();
        distinct.add(defaultPolicy);
        for (Rule rule : this.rules) {
            distinct.add(rule.policy());
        }
        this.policies = List.copyOf(distinct);
    }

    public SandboxPolicy resolve(String templateName) {
        Objects.requireNonNull(templateName, "templateName");
        if (isLiteral(templateName)) {
            return defaultPolicy;
        }
        for (Rule rule : rules) {
            if (rule.exact() ? templateName.equals(rule.value())
                    : templateName.startsWith(rule.value())) {
                return rule.policy();
            }
        }
        return defaultPolicy;
    }

    public SandboxPolicy defaultPolicy() {
        return defaultPolicy;
    }

    public Collection<SandboxPolicy> policies() {
        return policies;
    }

    public static SandboxPolicyResolver fixed(SandboxPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        return new SandboxPolicyResolver(policy, List.of());
    }

    public static Builder builder(SandboxPolicy defaultPolicy) {
        return new Builder(defaultPolicy);
    }

    private static boolean isLiteral(String templateName) {
        return templateName.indexOf('{') >= 0
                || templateName.indexOf('}') >= 0
                || templateName.indexOf('%') >= 0
                || templateName.indexOf('\n') >= 0
                || templateName.indexOf('\r') >= 0;
    }

    public static final class Builder {

        private final SandboxPolicy defaultPolicy;
        private final List<Rule> rules = new ArrayList<>();

        private Builder(SandboxPolicy defaultPolicy) {
            this.defaultPolicy = Objects.requireNonNull(defaultPolicy, "defaultPolicy");
        }

        public Builder exact(String templateName, SandboxPolicy policy) {
            rules.add(new Rule(validateRule(templateName, false),
                    Objects.requireNonNull(policy, "policy"), true));
            return this;
        }

        public Builder prefix(String prefix, SandboxPolicy policy) {
            rules.add(new Rule(validateRule(prefix, true) + "/",
                    Objects.requireNonNull(policy, "policy"), false));
            return this;
        }

        public SandboxPolicyResolver build() {
            return new SandboxPolicyResolver(defaultPolicy, List.copyOf(rules));
        }

        private static String validateRule(String value, boolean prefix) {
            Objects.requireNonNull(value, "templateName");
            String name = value;
            if (name.endsWith("/")) {
                if (!prefix) {
                    throw new IllegalArgumentException(
                            "An exact template name must not end with a slash");
                }
                name = name.substring(0, name.length() - 1);
            }
            if (name.isEmpty() || name.startsWith("/") || name.indexOf('\\') >= 0) {
                throw new IllegalArgumentException("Invalid template rule: " + value);
            }
            if (name.indexOf('{') >= 0 || name.indexOf('}') >= 0
                    || name.indexOf('%') >= 0) {
                throw new IllegalArgumentException("Invalid template rule: " + value);
            }
            if (name.chars().anyMatch(character ->
                    Character.isWhitespace(character) || Character.isISOControl(character))) {
                throw new IllegalArgumentException("Invalid template rule: " + value);
            }
            for (String segment : name.split("/", -1)) {
                if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                    throw new IllegalArgumentException("Invalid template rule: " + value);
                }
            }
            return name;
        }
    }

    private record Rule(String value, SandboxPolicy policy, boolean exact) {
    }
}
