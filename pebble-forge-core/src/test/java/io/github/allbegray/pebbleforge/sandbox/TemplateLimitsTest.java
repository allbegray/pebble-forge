package io.github.allbegray.pebbleforge.sandbox;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateLimitsTest {

    @Test
    void appliesLoopLimitsByTemplatePrefix() throws Exception {
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", loopTemplate(),
                        "admin/page.peb", loopTemplate()),
                SandboxLimitsResolver.builder(SandboxLimits.unlimited())
                        .prefix("public/", SandboxLimits.builder().maxLoopIterations(2).build())
                        .prefix("admin/", SandboxLimits.builder().maxLoopIterations(10).build())
                        .build());

        assertThatThrownBy(() -> render(engine, "public/page.peb", Map.of("items", List.of(1, 2, 3))))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Loop iteration limit of 2");
        assertThat(render(engine, "admin/page.peb", Map.of("items", List.of(1, 2, 3))))
                .isEqualTo("123");
    }

    @Test
    void appliesIncludeDepthByTemplatePrefix() throws Exception {
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", "{% include \"public/a.peb\" %}",
                        "public/a.peb", "{% include \"public/b.peb\" %}",
                        "public/b.peb", "b",
                        "admin/page.peb", "{% include \"admin/a.peb\" %}",
                        "admin/a.peb", "{% include \"admin/b.peb\" %}",
                        "admin/b.peb", "b"),
                SandboxLimitsResolver.builder(SandboxLimits.unlimited())
                        .prefix("public/", SandboxLimits.builder().maxIncludeDepth(1).build())
                        .prefix("admin/", SandboxLimits.builder().maxIncludeDepth(3).build())
                        .build());

        assertThatThrownBy(() -> render(engine, "public/page.peb", Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Include depth limit of 1");
        assertThat(render(engine, "admin/page.peb", Map.of())).isEqualTo("b");
    }

    @Test
    void appliesSlotBudgetsByTemplatePrefix() throws Exception {
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", slotTemplate(),
                        "admin/page.peb", slotTemplate(),
                        "components/echo.peb", "{{ slot(\"a\") }}{{ slot(\"b\") }}"),
                SandboxLimitsResolver.builder(SandboxLimits.unlimited())
                        .prefix("public/", SandboxLimits.builder().maxSlotChars(10).build())
                        .prefix("admin/", SandboxLimits.builder().maxSlotChars(100).build())
                        .build());

        assertThatThrownBy(() -> render(engine, "public/page.peb", Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("more than 10 chars");
        assertThat(render(engine, "admin/page.peb", Map.of()).strip())
                .isEqualTo("123456123456");
    }

    private static PebbleEngine engine(Map<String, String> templates, SandboxLimitsResolver limits) {
        return PebbleForge.engine()
                .loader(new MapLoader(templates))
                .components(Components.defaults())
                .sandbox(SandboxPolicy.strict())
                .limitsResolver(limits)
                .build();
    }

    private static String render(PebbleEngine engine, String template, Map<String, Object> model)
            throws Exception {
        StringWriter writer = new StringWriter();
        engine.getTemplate(template).evaluate(writer, model);
        return writer.toString();
    }

    private static String loopTemplate() {
        return "{% for n in items %}{{ n }}{% endfor %}";
    }

    private static String slotTemplate() {
        return """
                {% component "echo" %}{% slot "a" %}123456{% endslot %}{% slot "b" %}123456{% endslot %}{% endcomponent %}
                """;
    }
}
