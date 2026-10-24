package io.github.allbegray.pebbleforge.language;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LanguageExtensionTest {

    @Test
    void rendersTheFirstMatchingCase() throws Exception {
        assertThat(render("""
                {% switch role %}{% case "admin" %}A{% case "user" %}U{% default %}D{% endswitch %}
                """, Map.of("role", "user"))).isEqualTo("U");
    }

    @Test
    void rendersTheDefaultCase() throws Exception {
        assertThat(render("""
                {% switch role %}{% case "admin" %}A{% default %}D{% endswitch %}
                """, Map.of("role", "guest"))).isEqualTo("D");
    }

    @Test
    void doesNotFallThrough() throws Exception {
        assertThat(render("""
                {% switch role %}{% case "admin" %}A{% case "user" %}U{% endswitch %}
                """, Map.of("role", "admin"))).isEqualTo("A");
    }

    @Test
    void parsesNestedSwitches() throws Exception {
        assertThat(render("""
                {% switch outer %}{% case 1 %}{% switch inner %}{% case "a" %}A{% default %}D{% endswitch %}{% default %}X{% endswitch %}
                """, Map.of("outer", 1, "inner", "a"))).isEqualTo("A");
    }

    @Test
    void matchesAnyValueInAMultiValueCase() throws Exception {
        assertThat(render("""
                {% switch role %}{% case "admin", "owner" %}A{% default %}D{% endswitch %}
                """, Map.of("role", "owner"))).isEqualTo("A");
    }

    @Test
    void breakStopsTheLoop() throws Exception {
        assertThat(render("""
                {% for n in numbers %}{% if n == 3 %}{% break %}{% endif %}{{ n }}{% endfor %}
                """, Map.of("numbers", List.of(1, 2, 3, 4)))).isEqualTo("12");
    }

    @Test
    void continueSkipsTheRestOfTheIteration() throws Exception {
        assertThat(render("""
                {% for n in numbers %}{% if n == 2 %}{% continue %}{% endif %}{{ n }}{% endfor %}
                """, Map.of("numbers", List.of(1, 2, 3, 4)))).isEqualTo("134");
    }

    @Test
    void breakOnlyExitsTheInnerLoop() throws Exception {
        assertThat(render("""
                {% for i in outer %}{% for j in inner %}{% if j == 2 %}{% break %}{% endif %}{{ i }}{{ j }}{% endfor %}{% endfor %}
                """, Map.of("outer", List.of(1, 2), "inner", List.of(1, 2)))).isEqualTo("1121");
    }

    @Test
    void breakExitsTheSwitchWithoutExitingTheLoop() throws Exception {
        assertThat(render("""
                {% for n in numbers %}{% switch n %}{% case 2 %}{% break %}{% default %}{{ n }}{% endswitch %}{% endfor %}
                """, Map.of("numbers", List.of(1, 2, 3)))).isEqualTo("13");
    }

    @Test
    void continueInsideSwitchContinuesTheLoop() throws Exception {
        assertThat(render("""
                {% for n in numbers %}{% switch n %}{% case 2 %}{% continue %}{% default %}{{ n }}{% endswitch %}{% endfor %}
                """, Map.of("numbers", List.of(1, 2, 3)))).isEqualTo("13");
    }

    @Test
    void withAddsScopedVariables() throws Exception {
        assertThat(render("""
                {% with {"name": "Ada", "count": 2} %}{{ name }}{{ count }}{% endwith %}
                """, Map.of())).isEqualTo("Ada2");
    }

    @Test
    void withSeesOuterVariables() throws Exception {
        assertThat(render("""
                {% with {"count": 2} %}{{ name }}{{ count }}{% endwith %}
                """, Map.of("name", "Ada"))).isEqualTo("Ada2");
    }

    @Test
    void withOnlyHidesOuterVariables() throws Exception {
        assertThat(render("""
                {% set outer = "x" %}{% with {"inner": "y"} only %}{{ inner }}{{ outer | default("missing") }}{% endwith %}
                """, Map.of())).isEqualTo("ymissing");
    }

    @Test
    void withScopesNewVariables() throws Exception {
        assertThat(render("""
                {% with {"temp": "x"} %}{{ temp }}{% endwith %}{{ temp | default("missing") }}
                """, Map.of())).isEqualTo("xmissing");
    }

    @Test
    void withRequiresAMap() {
        assertThatThrownBy(() -> render("{% with value %}{{ value }}{% endwith %}",
                Map.of("value", "not a map")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("requires a map");
    }

    @Test
    void withRejectsNonStringKeys() {
        Map<Object, Object> values = new LinkedHashMap<>();
        values.put(1, "one");

        assertThatThrownBy(() -> render("{% with values %}{{ values }}{% endwith %}",
                Map.of("values", values)))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("keys must be strings");
    }

    @Test
    void escapesPrintNodesInsideLanguageBlocks() throws Exception {
        assertThat(render("""
                {% switch mode %}{% case "x" %}{{ value }}{% endswitch %}{% with {"v": value} %}{{ v }}{% endwith %}
                """, Map.of("mode", "x", "value", "<b>"))).isEqualTo("&lt;b&gt;&lt;b&gt;");
    }

    @Test
    void forStillProvidesLoopVariables() throws Exception {
        assertThat(render("""
                {% for n in numbers %}{{ loop.index }}:{{ loop.first }}:{{ loop.last }}:{{ n }};{% endfor %}
                """, Map.of("numbers", List.of(1, 2, 3))))
                .isEqualTo("0:true:false:1;1:false:false:2;2:false:true:3;");
    }

    @Test
    void forElseStillRunsWhenTheIterableIsEmpty() throws Exception {
        assertThat(render("""
                {% for n in numbers %}x{% else %}empty{% endfor %}
                """, Map.of("numbers", List.of()))).isEqualTo("empty");
    }

    @Test
    void forDestructuresMapEntries() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        assertThat(render("""
                {% for key, value in map %}{{ key }}={{ value }};{% endfor %}
                """, Map.of("map", map))).isEqualTo("a=1;b=2;");
    }

    @Test
    void forDestructuresIndexesFromLists() throws Exception {
        assertThat(render("""
                {% for index, value in items %}{{ index }}:{{ value }};{% endfor %}
                """, Map.of("items", List.of("a", "b")))).isEqualTo("0:a;1:b;");
    }

    @Test
    void forIteratesMaps() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        assertThat(render("""
                {% for entry in map %}{{ entry.key }}={{ entry.value }};{% endfor %}
                """, Map.of("map", map))).isEqualTo("a=1;b=2;");
    }

    @Test
    void forIteratesArrays() throws Exception {
        assertThat(render("""
                {% for n in numbers %}{{ n }}{% endfor %}
                """, Map.of("numbers", new int[]{1, 2, 3}))).isEqualTo("123");
    }

    @Test
    void breakOutsideALoopOrSwitchFailsAtParseTime() {
        PebbleEngine engine = engine("{% break %}", Map.of());

        assertThatThrownBy(() -> engine.getTemplate("page.peb"))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("break tag is only allowed");
    }

    @Test
    void continueOutsideALoopFailsAtParseTime() {
        PebbleEngine engine = engine("{% continue %}", Map.of());

        assertThatThrownBy(() -> engine.getTemplate("page.peb"))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("continue tag is only allowed");
    }

    @Test
    void languageCanBeDisabled() {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb",
                        "{% switch role %}{% case \"x\" %}x{% endswitch %}")))
                .languageExtensions(false)
                .build();

        assertThatThrownBy(() -> engine.getTemplate("page.peb"))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("switch");
    }

    @Test
    void sandboxAllowsTheLanguageTagsByDefault() throws Exception {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", """
                        {% for n in numbers %}{% if n == 2 %}{% continue %}{% endif %}\
                        {% switch n %}{% case 3 %}{% break %}{% default %}{{ n }}{% endswitch %}\
                        {% endfor %}
                        """)))
                .sandbox(SandboxPolicy.strict())
                .build();
        StringWriter writer = new StringWriter();

        engine.getTemplate("page.peb").evaluate(writer,
                Map.of("numbers", List.of(1, 2, 3, 4)));

        assertThat(writer.toString()).isEqualTo("14");
    }

    @Test
    void sandboxAllowsTheLanguageFollowUps() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", """
                        {% for key, value in map %}{{ key }}={{ value }};{% endfor %}\
                        {% with {"role": "owner"} only %}{% switch role %}{% case "admin", "owner" %}A{% endswitch %}{% endwith %}
                        """)))
                .sandbox(SandboxPolicy.strict())
                .build();
        StringWriter writer = new StringWriter();

        engine.getTemplate("page.peb").evaluate(writer, Map.of("map", map));

        assertThat(writer.toString()).isEqualTo("a=1;b=2;A");
    }

    private static String render(String template, Map<String, Object> model) throws Exception {
        PebbleEngine engine = engine(template, model);
        StringWriter writer = new StringWriter();
        engine.getTemplate("page.peb").evaluate(writer, model);
        return writer.toString();
    }

    private static PebbleEngine engine(String template, Map<String, Object> model) {
        return PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", template)))
                .build();
    }
}
