package io.github.allbegray.pebbleforge.sandbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxLimitsResolverTest {

    private static final SandboxLimits DEFAULT = SandboxLimits.builder()
            .maxLoopIterations(100)
            .build();
    private static final SandboxLimits PUBLIC = SandboxLimits.builder()
            .maxLoopIterations(10)
            .build();
    private static final SandboxLimits PUBLIC_EXACT = SandboxLimits.builder()
            .maxLoopIterations(5)
            .build();
    private static final SandboxLimits ADMIN = SandboxLimits.builder()
            .maxLoopIterations(1_000)
            .build();

    @Test
    void returnsTheDefaultLimits() {
        SandboxLimitsResolver resolver = SandboxLimitsResolver.builder(DEFAULT).build();

        assertThat(resolver.resolve("page.peb").maxLoopIterations()).isEqualTo(100);
    }

    @Test
    void matchesTemplatePrefixes() {
        SandboxLimitsResolver resolver = SandboxLimitsResolver.builder(DEFAULT)
                .prefix("public/", PUBLIC)
                .prefix("admin/", ADMIN)
                .build();

        assertThat(resolver.resolve("public/page.peb").maxLoopIterations()).isEqualTo(10);
        assertThat(resolver.resolve("admin/page.peb").maxLoopIterations()).isEqualTo(1_000);
        assertThat(resolver.resolve("page.peb").maxLoopIterations()).isEqualTo(100);
    }

    @Test
    void letsAnExactRuleBeatAPrefixRule() {
        SandboxLimitsResolver resolver = SandboxLimitsResolver.builder(DEFAULT)
                .prefix("public/", PUBLIC)
                .exact("public/special.peb", PUBLIC_EXACT)
                .build();

        assertThat(resolver.resolve("public/special.peb").maxLoopIterations()).isEqualTo(5);
        assertThat(resolver.resolve("public/other.peb").maxLoopIterations()).isEqualTo(10);
    }
}
