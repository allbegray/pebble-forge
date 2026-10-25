package io.github.allbegray.pebbleforge.escape;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextEscapingTemplateTest {

    @Test
    void rendersHtmlTextUrlAttributeJsAndCssInOneTemplate() throws IOException {
        PebbleEngine engine = forgeEngine("""
                <p title="{{ title | escape('html_attr') }}">{{ text }}</p>
                <a href="{{ link | escape('url') }}">link</a>
                <script>const label = "{{ script | escape('js') }}";</script>
                <style>.note { background: {{ style | escape('css') }}; }</style>\
                """);

        String output = render(engine, Map.of(
                "text", "<script>alert(1)</script>",
                "title", "\" onmouseover=\"alert(1)",
                "link", "javascript:alert(1)",
                "script", "</script>",
                "style", "url(javascript:alert(1))"));

        assertThat(output).isEqualTo("""
                <p title="&quot;&#32;onmouseover&#61;&quot;alert(1)">&lt;script&gt;alert(1)&lt;/script&gt;</p>
                <a href="#">link</a>
                <script>const label = "<\\/script>";</script>
                <style>.note { background: url\\(javascript\\3A alert\\(1\\)\\); }</style>\
                """);
    }

    @Test
    void autoEscapingUsesTheBuiltInHtmlStrategy() throws IOException {
        PebbleEngine engine = forgeEngine("{{ value }}");

        assertThat(render(engine, Map.of("value", "<b>&\"'")))
                .isEqualTo("&lt;b&gt;&amp;&quot;&#39;");
    }

    @Test
    void explicitHtmlAttributeAndUrlEscapeOnce() throws IOException {
        PebbleEngine engine = forgeEngine(
                "{{ value | escape('html_attr') }}|{{ link | escape('url') }}");

        assertThat(render(engine, Map.of(
                "value", "\" onmouseover=\"x",
                "link", "javascript:alert(1)")))
                .isEqualTo("&quot;&#32;onmouseover&#61;&quot;x|#");
    }

    @Test
    void plainPebbleFailsWithoutRegistration() {
        PebbleEngine engine = new PebbleEngine.Builder()
                .loader(new MapLoader(Map.of("page.peb", "{{ value | escape('html_attr') }}")))
                .build();
        PebbleTemplate template = engine.getTemplate("page.peb");

        assertThatThrownBy(() -> template.evaluate(new StringWriter(), Map.of("value", "x")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Unknown escaping strategy [html_attr]");
    }

    @Test
    void registerIsIdempotentOnAPlainBuilder() throws IOException {
        PebbleEngine.Builder builder = new PebbleEngine.Builder()
                .loader(new MapLoader(Map.of("page.peb", "{{ value | escape('html_attr') }}")));
        ContextEscaping.register(builder);
        ContextEscaping.register(builder);

        assertThat(render(builder.build(), Map.of("value", "<b>"))).isEqualTo("&lt;b&gt;");
    }

    @Test
    void nullPrintsNothing() throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb",
                        "{{ value | escape('html_attr') }}|{{ value | escape('url') }}")))
                .strictVariables(false)
                .build();
        Map<String, Object> model = new HashMap<>();
        model.put("value", null);

        assertThat(render(engine, model)).isEqualTo("|");
    }

    @Test
    void aFilterBeforeTheContextEscapeEscapesOnce() throws IOException {
        PebbleEngine engine = forgeEngine("{{ value | upper | escape('html_attr') }}");

        assertThat(render(engine, Map.of("value", "<b&>"))).isEqualTo("&lt;B&amp;&gt;");
    }

    @Test
    void anUnknownStrategyFailsClosed() {
        PebbleEngine engine = forgeEngine("{{ value | escape('nope') }}");
        PebbleTemplate template = engine.getTemplate("page.peb");

        assertThatThrownBy(() -> template.evaluate(new StringWriter(), Map.of("value", "x")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Unknown escaping strategy [nope]");
    }

    private static PebbleEngine forgeEngine(String template) {
        return PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", template)))
                .build();
    }

    private static String render(PebbleEngine engine, Map<String, Object> model) throws IOException {
        StringWriter writer = new StringWriter();
        engine.getTemplate("page.peb").evaluate(writer, model);
        return writer.toString();
    }
}
