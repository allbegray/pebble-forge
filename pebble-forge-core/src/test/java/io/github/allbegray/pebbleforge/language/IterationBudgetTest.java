package io.github.allbegray.pebbleforge.language;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IterationBudgetTest {

    @Test
    void sharesOneBudgetAcrossSequentialLoops() {
        PebbleEngine engine = engine("""
                {% for n in first %}{{ n }}{% endfor %}{% for n in second %}{{ n }}{% endfor %}
                """, 5);

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("first", List.of(1, 2, 3), "second", List.of(1, 2, 3))))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Total loop iteration limit of 5 reached");
    }

    @Test
    void sharesOneBudgetAcrossNestedLoops() {
        PebbleEngine engine = engine("""
                {% for a in outer %}{% for b in inner %}{{ a }}{{ b }}{% endfor %}{% endfor %}
                """, 5);

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("outer", List.of(1, 2, 3), "inner", List.of(1, 2))))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Total loop iteration limit of 5 reached");
    }

    @Test
    void resetsTheBudgetForEachRender() throws Exception {
        PebbleEngine engine = engine("""
                {% for n in items %}{{ n }}{% endfor %}
                """, 5);
        Map<String, Object> model = Map.of("items", List.of(1, 2, 3));

        assertThat(render(engine, model)).isEqualTo("123");
        assertThat(render(engine, model)).isEqualTo("123");
    }

    private static PebbleEngine engine(String template, int maxTotalIterations) {
        return PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", template)))
                .sandbox(SandboxPolicy.builder()
                        .maxTotalIterations(maxTotalIterations)
                        .build())
                .build();
    }

    private static String render(PebbleEngine engine, Map<String, Object> model) throws Exception {
        StringWriter writer = new StringWriter();
        engine.getTemplate("page.peb").evaluate(writer, model);
        return writer.toString();
    }
}
