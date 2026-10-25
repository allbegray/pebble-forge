package io.github.allbegray.pebbleforge.fuzz;

import io.github.allbegray.pebbleforge.fuzz.support.FuzzText;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxAllowlistFuzzTest {

    private static final SandboxPolicy POLICY = SandboxPolicy.builder()
            .allowOnlyTags("if")
            .allowOnlyFilters("escape")
            .allowOnlyFunctions("slot")
            .allowOnlyTests("empty")
            .build();

    @Property(tries = 200)
    void allowlistsMatchExactNames(@ForAll("names") String name) {
        String description = "name [" + FuzzText.escape(name) + "]";
        assertThat(POLICY.isTagAllowed(name)).as("tag %s", description).isEqualTo("if".equals(name));
        assertThat(POLICY.isFilterAllowed(name)).as("filter %s", description)
                .isEqualTo("escape".equals(name));
        assertThat(POLICY.isFunctionAllowed(name)).as("function %s", description)
                .isEqualTo("slot".equals(name));
        assertThat(POLICY.isTestAllowed(name)).as("test %s", description)
                .isEqualTo("empty".equals(name));
    }

    @Property(tries = 100)
    void builderNormalizesOrRejectsNamesWithoutLeaking(@ForAll("names") String name) {
        SandboxPolicy.Builder builder = SandboxPolicy.builder();
        try {
            builder.allowOnlyTags(name);
        } catch (IllegalArgumentException expected) {
            assertThat(name.trim()).isEmpty();
            return;
        }

        SandboxPolicy policy = builder.build();
        String stored = name.trim();
        assertThat(policy.isTagAllowed(stored)).isTrue();
        assertThat(policy.isTagAllowed(name)).isEqualTo(stored.equals(name));
    }

    @Test
    void exactNamesAreAllowed() {
        assertThat(POLICY.isTagAllowed("if")).isTrue();
        assertThat(POLICY.isFilterAllowed("escape")).isTrue();
        assertThat(POLICY.isFunctionAllowed("slot")).isTrue();
        assertThat(POLICY.isTestAllowed("empty")).isTrue();
    }

    @Test
    void nearMissesAreRejected() {
        for (String name : List.of("if ", " if", "IF", "If", "if\t", "if\n", "if\0", "if/", "iff", "")) {
            assertThat(POLICY.isTagAllowed(name))
                    .as("tag [%s]", FuzzText.escape(name))
                    .isFalse();
        }
        for (String name : List.of("escape ", " escape", "ESCAPE", "escape/", "escapee", "")) {
            assertThat(POLICY.isFilterAllowed(name))
                    .as("filter [%s]", FuzzText.escape(name))
                    .isFalse();
        }
    }

    @Test
    void allowOnlyDropsDefaultSafeEntries() {
        assertThat(POLICY.isTagAllowed("for")).isFalse();
        assertThat(POLICY.isTagAllowed("set")).isFalse();
        assertThat(POLICY.isFilterAllowed("upper")).isFalse();
        assertThat(POLICY.isFunctionAllowed("i18n")).isFalse();
        assertThat(POLICY.isTestAllowed("null")).isFalse();
    }

    @Provide
    Arbitrary<String> names() {
        return Arbitraries.strings()
                .withChars("_- . /\\:;=&%#{}<>\"'@!?*+~^$|()[],")
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMaxLength(24);
    }
}
