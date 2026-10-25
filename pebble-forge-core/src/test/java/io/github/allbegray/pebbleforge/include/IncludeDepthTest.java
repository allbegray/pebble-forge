package io.github.allbegray.pebbleforge.include;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncludeDepthTest {

    private static final Map<String, String> TEMPLATES = Map.of(
            "page.peb", "{% include \"a.peb\" %}",
            "a.peb", "{% include \"b.peb\" %}",
            "b.peb", "b");

    @Test
    void allowsIncludesWithinTheLimit() throws Exception {
        assertThat(render(SandboxPolicy.builder().maxIncludeDepth(2).build()))
                .isEqualTo("b");
    }

    @Test
    void rejectsTheNextIncludeAtTheLimit() {
        PebbleEngine engine = engine(SandboxPolicy.builder().maxIncludeDepth(1).build());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Include depth limit of 1 reached");
    }

    @Test
    void zeroDisablesIncludes() {
        PebbleEngine engine = engine(SandboxPolicy.builder().maxIncludeDepth(0).build());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Include depth limit of 0 reached");
    }

    @Test
    void includesAreUnlimitedWithoutTheSandbox() throws Exception {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(TEMPLATES))
                .build();
        StringWriter writer = new StringWriter();

        engine.getTemplate("page.peb").evaluate(writer);

        assertThat(writer.toString()).isEqualTo("b");
    }

    private static String render(SandboxPolicy policy) throws Exception {
        PebbleEngine engine = engine(policy);
        StringWriter writer = new StringWriter();
        engine.getTemplate("page.peb").evaluate(writer);
        return writer.toString();
    }

    private static PebbleEngine engine(SandboxPolicy policy) {
        return PebbleForge.engine()
                .loader(new MapLoader(TEMPLATES))
                .sandbox(policy)
                .build();
    }
}
