package io.github.allbegray.pebbleforge.component;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComponentsTest {

    private static String render(Map<String, String> templates, String templateName,
                                 Map<String, Object> context, Components components)
            throws java.io.IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(templates))
                .components(components)
                .build();
        StringWriter writer = new StringWriter();
        engine.getTemplate(templateName).evaluate(writer, context);
        return writer.toString();
    }

    private static String render(Map<String, String> templates, Map<String, Object> context)
            throws java.io.IOException {
        return render(templates, "page.peb", context, Components.defaults());
    }

    @Test
    void rendersPropsAndDefaultBodySlot() throws java.io.IOException {
        String output = render(
                Map.of(
                        "page.peb", """
                                {% component "card" with {"title": "Forge"} %}
                                  <p>Hello</p>
                                {% endcomponent %}
                                """,
                        "components/card.peb", """
                                <article><h2>{{ title }}</h2><div>{{ slot() }}</div></article>
                                """),
                Map.of());

        assertThat(output).isEqualTo(
                "<article><h2>Forge</h2><div>  <p>Hello</p>\n</div></article>\n");
    }

    @Test
    void namedSlotsRenderInTheCallerContext() throws java.io.IOException {
        String output = render(
                Map.of(
                        "page.peb", """
                                {% component "layout" with {"title": "Inbox"} %}
                                {% slot "header" %}<h1>{{ heading }}</h1>{% endslot %}
                                {% slot %}  <p>{{ unread }} unread</p>  {% endslot %}
                                {% endcomponent %}
                                """,
                        "components/layout.peb", """
                                <header>{{ slot("header") }}</header><main>{{ slot("body") }}</main>
                                """),
                Map.of("heading", "Inbox", "unread", 3));

        assertThat(output).isEqualTo(
                "<header><h1>Inbox</h1></header><main>  <p>3 unread</p>  </main>\n");
    }

    @Test
    void appliesPropDefaultsAndLetsCallersOverrideThem() throws java.io.IOException {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% component "badge" %}{% endcomponent %}
                        {% component "badge" with {"label": "New"} %}{% endcomponent %}
                        """,
                "components/badge.peb", """
                        {% props label = "Default" %}<span>{{ label }}</span>
                        """);

        assertThat(render(templates, Map.of())).isEqualTo("<span>Default</span>\n<span>New</span>\n");
    }

    @Test
    void failsWhenARequiredPropIsMissing() {
        assertThatThrownBy(() -> render(
                Map.of(
                        "page.peb", "{% component \"card\" %}{% endcomponent %}",
                        "components/card.peb", "{% props title %}{{ title }}"),
                Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Missing required prop [title]");
    }

    @Test
    void failsOnDuplicateSlotNames() {
        assertThatThrownBy(() -> render(
                Map.of(
                        "page.peb", """
                                {% component "layout" %}
                                {% slot "body" %}one{% endslot %}
                                {% slot "body" %}two{% endslot %}
                                {% endcomponent %}
                                """,
                        "components/layout.peb", "{{ slot(\"body\") }}"),
                Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Duplicate slot name [body]");
    }

    @Test
    void failsWhenTheDepthLimitIsReached() {
        assertThatThrownBy(() -> render(
                Map.of(
                        "page.peb", "{% component \"tree\" with {\"level\": 3} %}{% endcomponent %}",
                        "components/tree.peb", """
                                {% props level %}
                                {{ level }}
                                {% if level > 0 %}{% component "tree" with {"level": level - 1} %}{% endcomponent %}{% endif %}
                                """),
                "page.peb",
                Map.of(),
                Components.builder().maxDepth(2).build()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Component depth limit of 2 reached");
    }

    @Test
    void composesNestedComponents() throws java.io.IOException {
        String output = render(
                Map.of(
                        "page.peb", "{% component \"page\" with {\"name\": \"Ada\"} %}{% endcomponent %}",
                        "components/page.peb", """
                                <main>{% component "greeting" with {"name": name} %}{% endcomponent %}</main>
                                """,
                        "components/greeting.peb", "<p>Hello {{ name }}</p>"),
                Map.of());

        assertThat(output).isEqualTo("<main><p>Hello Ada</p></main>\n");
    }

    @Test
    void escapesSlotContentExactlyOnce() throws java.io.IOException {
        String output = render(
                Map.of(
                        "page.peb", "{% component \"echo\" %}{{ value }}{% endcomponent %}",
                        "components/echo.peb", "<div>{{ slot() }}</div>"),
                Map.of("value", "<b>bold</b>"));

        assertThat(output).isEqualTo("<div>&lt;b&gt;bold&lt;/b&gt;</div>");
    }

    @Test
    void rendersEachCallWithItsOwnProps() throws java.io.IOException {
        String output = render(
                Map.of(
                        "page.peb", """
                                {% component "card" with {"title": "First"} %}{% endcomponent %}
                                {% component "card" with {"title": "Second"} %}{% endcomponent %}
                                """,
                        "components/card.peb", "<h2>{{ title }}</h2>"),
                Map.of());

        assertThat(output).isEqualTo("<h2>First</h2><h2>Second</h2>");
    }

    @Test
    void rejectsNonStringPropNames() {
        assertThatThrownBy(() -> render(
                Map.of(
                        "page.peb", "{% component \"card\" with {1: \"one\"} %}{% endcomponent %}",
                        "components/card.peb", "{{ slot() }}"),
                Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Component prop names must be strings");
    }

    @Test
    void rejectsPathTraversalInComponentNames() {
        assertThatThrownBy(() -> render(
                Map.of(
                        "page.peb", "{% component \"../secret\" %}{% endcomponent %}",
                        "secret.peb", "secret"),
                Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("invalid segment");
    }
}
