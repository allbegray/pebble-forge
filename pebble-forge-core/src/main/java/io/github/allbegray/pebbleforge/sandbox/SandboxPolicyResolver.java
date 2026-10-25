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

    /**
     * Resolves the policy for a template name. The default policy applies when no rule matches
     * and to literal template text.
     *
     * @param templateName the template name
     * @return the policy
     */
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

    /**
     * Returns the policy used when no rule matches.
     *
     * @return the default policy
     */
    public SandboxPolicy defaultPolicy() {
        return defaultPolicy;
    }

    /**
     * Returns every distinct policy in this resolver.
     *
     * @return the policies
     */
    public Collection<SandboxPolicy> policies() {
        return policies;
    }

    /**
     * Returns a resolver that uses one policy for every template.
     *
     * @param policy the policy to use
     * @return the resolver
     */
    public static SandboxPolicyResolver fixed(SandboxPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        return new SandboxPolicyResolver(policy, List.of());
    }

    /**
     * Creates a resolver builder.
     *
     * @param defaultPolicy the policy used when no rule matches
     * @return a new builder
     */
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

    /**
     * Builds a {@link SandboxPolicyResolver}. Exact rules win over prefix rules, and longer
     * prefixes win over shorter prefixes.
     */
    public static final class Builder {

        private final SandboxPolicy defaultPolicy;
        private final List<Rule> rules = new ArrayList<>();

        private Builder(SandboxPolicy defaultPolicy) {
            this.defaultPolicy = Objects.requireNonNull(defaultPolicy, "defaultPolicy");
        }

        /**
         * Adds a rule for one template name.
         *
         * @param templateName the exact template name
         * @param policy the policy to use
         * @return this builder
         */
        public Builder exact(String templateName, SandboxPolicy policy) {
            rules.add(new Rule(validateRule(templateName, false),
                    Objects.requireNonNull(policy, "policy"), true));
            return this;
        }

        /**
         * Adds a rule for a template name prefix.
         *
         * @param prefix the directory or name prefix
         * @param policy the policy to use
         * @return this builder
         */
        public Builder prefix(String prefix, SandboxPolicy policy) {
            rules.add(new Rule(validateRule(prefix, true) + "/",
                    Objects.requireNonNull(policy, "policy"), false));
            return this;
        }

        /**
         * Builds the resolver.
         *
         * @return the resolver
         */
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
