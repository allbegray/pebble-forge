package io.github.allbegray.pebbleforge.language;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class ForCompatibilityTest {

    @Test
    void listIterationMatchesPebble() throws Exception {
        assertSameOutput("{% for x in items %}{{ x }};{% endfor %}",
                Map.of("items", List.of("a", "b", "c")));
    }

    @Test
    void setIterationMatchesPebble() throws Exception {
        assertSameOutput("{% for x in items %}{{ x }};{% endfor %}",
                Map.of("items", new LinkedHashSet<>(List.of("a", "b", "c"))));
    }

    @Test
    void mapIterationMatchesPebble() throws Exception {
        Map<String, Integer> entries = new LinkedHashMap<>();
        entries.put("a", 1);
        entries.put("b", 2);

        assertSameOutput("{% for e in entries %}{{ e.key }}={{ e.value }};{% endfor %}",
                Map.of("entries", entries));
    }

    @Test
    void arrayIterationMatchesPebble() throws Exception {
        assertSameOutput("{% for x in items %}{{ x }};{% endfor %}",
                Map.of("items", new int[]{1, 2, 3}));
    }

    @Test
    void enumerationIterationMatchesPebble() throws Exception {
        Map<String, String> templates = Map.of("page.peb",
                "{% for x in items %}{{ x }};{% endfor %}");

        assertThat(renderForge(templates,
                Map.of("items", Collections.enumeration(List.of(1, 2, 3))), null))
                .isEqualTo(renderPlain(templates,
                        Map.of("items", Collections.enumeration(List.of(1, 2, 3))), null));
    }

    @Test
    void emptyIterableAndElseMatchPebble() throws Exception {
        assertSameOutput("{% for x in items %}{{ x }}{% else %}empty{% endfor %}",
                Map.of("items", List.of()));
    }

    @Test
    void loopVariablesMatchPebble() throws Exception {
        assertSameOutput("""
                {% for x in items %}{{ loop.index }}:{{ loop.first }}:{{ loop.last }}:\
                {{ loop.length }}:{{ loop.revindex }}:{{ x }};{% endfor %}
                """, Map.of("items", List.of("a", "b", "c")));
    }

    @Test
    void nestedLoopsMatchPebble() throws Exception {
        assertSameOutput("""
                {% for x in outer %}{% for y in inner %}{{ x }}{{ y }};{% endfor %}{% endfor %}
                """, Map.of("outer", List.of(1, 2), "inner", List.of("a", "b")));
    }

    @Test
    void parallelIterationMatchesPebble() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Map<String, String> templates = Map.of("page.peb", """
                    {% parallel %}{% for x in items %}{{ x }};{% endfor %}{% endparallel %}
                    """);
            Map<String, Object> model = Map.of("items", List.of("a", "b", "c"));

            assertThat(renderForge(templates, model, executor))
                    .isEqualTo(renderPlain(templates, model, executor));
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertSameOutput(String template, Map<String, Object> model)
            throws Exception {
        Map<String, String> templates = Map.of("page.peb", template);
        assertThat(renderForge(templates, model, null))
                .isEqualTo(renderPlain(templates, model, null));
    }

    private static String renderPlain(Map<String, String> templates, Map<String, Object> model,
            ExecutorService executor) throws Exception {
        PebbleEngine.Builder builder = new PebbleEngine.Builder()
                .loader(new MapLoader(templates));
        if (executor != null) {
            builder.executorService(executor);
        }
        return render(builder.build(), model);
    }

    private static String renderForge(Map<String, String> templates, Map<String, Object> model,
            ExecutorService executor) throws Exception {
        PebbleForge.Builder builder = PebbleForge.engine()
                .loader(new MapLoader(templates));
        if (executor != null) {
            builder.executorService(executor);
        }
        return render(builder.build(), model);
    }

    private static String render(PebbleEngine engine, Map<String, Object> model) throws Exception {
        StringWriter writer = new StringWriter();
        engine.getTemplate("page.peb").evaluate(writer, model);
        return writer.toString();
    }
}
