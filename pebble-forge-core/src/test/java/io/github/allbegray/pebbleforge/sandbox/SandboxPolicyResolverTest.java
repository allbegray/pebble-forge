package io.github.allbegray.pebbleforge.sandbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxPolicyResolverTest {

    private static final SandboxPolicy DEFAULT = SandboxPolicy.strict();
    private static final SandboxPolicy PUBLIC = SandboxPolicy.builder().allowRawOutput().build();
    private static final SandboxPolicy ADMIN = SandboxPolicy.builder().allowTags("macro").build();
    private static final SandboxPolicy SPECIAL = SandboxPolicy.builder().allowTags("embed").build();

    @Test
    void returnsTheDefaultPolicy() {
        SandboxPolicyResolver resolver = SandboxPolicyResolver.builder(DEFAULT).build();

        assertThat(resolver.resolve("page.peb")).isSameAs(DEFAULT);
        assertThat(resolver.defaultPolicy()).isSameAs(DEFAULT);
    }

    @Test
    void matchesExactAndPrefixRules() {
        SandboxPolicyResolver resolver = SandboxPolicyResolver.builder(DEFAULT)
                .exact("public/special.peb", PUBLIC)
                .prefix("admin/", ADMIN)
                .build();

        assertThat(resolver.resolve("public/special.peb")).isSameAs(PUBLIC);
        assertThat(resolver.resolve("admin/page.peb")).isSameAs(ADMIN);
        assertThat(resolver.resolve("page.peb")).isSameAs(DEFAULT);
    }

    @Test
    void exactRulesBeatPrefixRules() {
        SandboxPolicyResolver resolver = SandboxPolicyResolver.builder(DEFAULT)
                .prefix("admin/", ADMIN)
                .exact("admin/page.peb", PUBLIC)
                .build();

        assertThat(resolver.resolve("admin/page.peb")).isSameAs(PUBLIC);
        assertThat(resolver.resolve("admin/other.peb")).isSameAs(ADMIN);
    }

    @Test
    void longestPrefixWins() {
        SandboxPolicyResolver resolver = SandboxPolicyResolver.builder(DEFAULT)
                .prefix("admin/", ADMIN)
                .prefix("admin/secret/", SPECIAL)
                .build();

        assertThat(resolver.resolve("admin/secret/page.peb")).isSameAs(SPECIAL);
        assertThat(resolver.resolve("admin/page.peb")).isSameAs(ADMIN);
    }

    @Test
    void literalTemplatesUseTheDefaultPolicy() {
        SandboxPolicyResolver resolver = SandboxPolicyResolver.builder(DEFAULT)
                .prefix("admin/", ADMIN)
                .build();

        assertThat(resolver.resolve("admin/{{ value | raw }}")).isSameAs(DEFAULT);
        assertThat(resolver.resolve("admin/{% macro badge() %}x{% endmacro %}")).isSameAs(DEFAULT);
        assertThat(resolver.resolve(" admin/page.peb")).isSameAs(DEFAULT);
    }

    @Test
    void exposesDistinctPolicies() {
        SandboxPolicyResolver resolver = SandboxPolicyResolver.builder(DEFAULT)
                .prefix("public/", PUBLIC)
                .prefix("admin/", ADMIN)
                .build();

        assertThat(resolver.policies()).containsExactly(DEFAULT, PUBLIC, ADMIN);
    }
}
