package io.github.allbegray.pebbleforge.sandbox;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.extension.Filter;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SandboxTest {

    private static PebbleEngine engine(Map<String, String> templates, SandboxPolicy policy,
                                       Extension... extensions) {
        PebbleForge.Builder builder = PebbleForge.engine()
                .loader(new MapLoader(templates))
                .sandbox(policy);
        if (extensions.length > 0) {
            builder.extension(extensions);
        }
        return builder.build();
    }

    private static String render(Map<String, String> templates, String templateName,
                                 Map<String, Object> context, SandboxPolicy policy)
            throws java.io.IOException {
        PebbleEngine engine = engine(templates, policy);
        StringWriter writer = new StringWriter();
        engine.getTemplate(templateName).evaluate(writer, context);
        return writer.toString();
    }

    private static String renderWithComponents(Map<String, String> templates, String templateName,
                                               Map<String, Object> context, SandboxPolicy policy)
            throws java.io.IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(templates))
                .components(Components.defaults())
                .sandbox(policy)
                .build();
        StringWriter writer = new StringWriter();
        engine.getTemplate(templateName).evaluate(writer, context);
        return writer.toString();
    }

    @Test
    void rendersTemplatesInsideTheAllowlist() throws java.io.IOException {
        String output = render(
                Map.of("page.peb", "{{ name | upper }}"),
                "page.peb",
                Map.of("name", "forge"),
                SandboxPolicy.strict());

        assertThat(output).isEqualTo("FORGE");
    }

    @Test
    void rejectsTagsOutsideTheAllowlist() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{% macro thing() %}x{% endmacro %}{{ thing() }}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb"))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("macro");
    }

    @Test
    void rejectsRawOutputByDefault() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{{ value | raw }}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("value", "<b>x</b>")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("raw");
    }

    @Test
    void allowsRawOutputWhenThePolicyOptsIn() throws java.io.IOException {
        SandboxPolicy policy = SandboxPolicy.builder().allowRawOutput().build();
        String output = render(
                Map.of("page.peb", "{{ value | raw }}"),
                "page.peb",
                Map.of("value", "<b>safe</b>"),
                policy);

        assertThat(output).isEqualTo("<b>safe</b>");
    }

    @Test
    void appliesContextEscapingStrategiesUnderAStrictSandbox() throws java.io.IOException {
        String output = render(
                Map.of("page.peb", """
                        <a href="{{ link | escape('url') }}" title="{{ title | escape('html_attr') }}">{{ text }}</a>\
                        """),
                "page.peb",
                Map.of(
                        "link", "javascript:alert(1)",
                        "title", "\" onmouseover=\"alert(1)",
                        "text", "<b>x</b>"),
                SandboxPolicy.strict());

        assertThat(output).isEqualTo(
                "<a href=\"#\" title=\"&quot;&#32;onmouseover&#61;&quot;alert(1)\">"
                        + "&lt;b&gt;x&lt;/b&gt;</a>");
    }

    @Test
    void blocksClassAccess() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{{ bean.class }}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("bean", new ExposedBean())))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Access to [class] is not allowed");
    }

    @Test
    void blocksGetClassEvenWhenCalledExplicitly() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{{ bean.getClass() }}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("bean", new ExposedBean())))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("getClass");
    }

    @Test
    void blocksPublicFieldAccess() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{{ bean.secret }}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("bean", new ExposedBean())))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Public field access is not allowed");
    }

    @Test
    void blocksStaticPublicFieldAccess() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{{ bean.SECRET }}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("bean", new StaticExposedBean())))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Public field access is not allowed");
    }

    @Test
    void blocksDangerousObjects() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{{ clazz.name }}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("clazz", String.class)))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Access to [name] is not allowed");
    }

    @Test
    void allowsOrdinaryMethods() throws java.io.IOException {
        String output = render(
                Map.of("page.peb", "{{ name.toUpperCase() }}"),
                "page.peb",
                Map.of("name", "forge"),
                SandboxPolicy.strict());

        assertThat(output).isEqualTo("FORGE");
    }

    @Test
    void keepsIncludedTemplatesInsideTheAllowlist() throws java.io.IOException {
        String output = render(
                Map.of(
                        "page.peb", "{% include \"partial.peb\" %}",
                        "partial.peb", "{{ value | upper }}"),
                "page.peb",
                Map.of("value", "ok"),
                SandboxPolicy.strict());

        assertThat(output).isEqualTo("OK");
    }

    @Test
    void escapesDataInsideSandboxedComponentSlots() throws java.io.IOException {
        String output = renderWithComponents(
                Map.of(
                        "page.peb", "{% component \"box\" %}{{ value }}{% endcomponent %}",
                        "components/box.peb", "<div>{{ slot() }}</div>"),
                "page.peb",
                Map.of("value", "<script>alert(1)</script>"),
                SandboxPolicy.strict());

        assertThat(output).isEqualTo(
                "<div>&lt;script&gt;alert(1)&lt;/script&gt;</div>");
    }

    @Test
    void keepsAuthorWrittenMarkupInComponentSlots() throws java.io.IOException {
        String output = renderWithComponents(
                Map.of(
                        "page.peb", """
                                {% component "box" %}<em>{{ value }}</em>{% endcomponent %}
                                """,
                        "components/box.peb", "<div>{{ slot() }}</div>"),
                "page.peb",
                Map.of("value", "<b>x</b>"),
                SandboxPolicy.strict());

        assertThat(output).isEqualTo("<div><em>&lt;b&gt;x&lt;/b&gt;</em></div>");
    }

    @Test
    void disablesRangeToAvoidAllocationBombs() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{% for i in 1..1000000000 %}{{ i }}{% endfor %}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("range");
    }

    @Test
    void rejectsParentTraversal() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{% include \"../secret.peb\" %}"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    void rejectsAbsoluteTemplateNames() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "ok"),
                SandboxPolicy.strict());

        assertThatThrownBy(() -> engine.getTemplate("/etc/passwd"))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Absolute template paths are not allowed");
    }

    @Test
    void enforcesTheOutputLimit() {
        SandboxPolicy policy = SandboxPolicy.builder().maxRenderedChars(32).build();
        PebbleEngine engine = engine(Map.of("page.peb", "{{ value }}"), policy);

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("value", "x".repeat(100))))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Tried to write more than 32 chars");
    }

    @Test
    void enforcesTheLoopIterationLimit() {
        SandboxPolicy policy = SandboxPolicy.builder().maxLoopIterations(2).build();
        PebbleEngine engine = engine(
                Map.of("page.peb", "{% for n in numbers %}{{ n }}{% endfor %}"),
                policy);

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("numbers", List.of(1, 2, 3))))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Loop iteration limit of 2");
    }

    @Test
    void enforcesTheSlotCaptureBudget() {
        SandboxPolicy policy = SandboxPolicy.builder().maxSlotChars(10).build();
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of(
                        "page.peb", """
                                {% component "echo" %}{% slot "a" %}123456{% endslot %}{% slot "b" %}123456{% endslot %}{% endcomponent %}
                                """,
                        "components/echo.peb", """
                                {{ slot("a") }}{{ slot("b") }}
                                """)))
                .components(Components.defaults())
                .sandbox(policy)
                .build();

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("more than 10 chars");
    }

    @Test
    void appliesTemplateNameRules() {
        SandboxPolicy policy = SandboxPolicy.builder()
                .templateNameValidator(name -> name.startsWith("public/"))
                .build();
        PebbleEngine engine = engine(
                Map.of("public/page.peb", "ok", "secret.peb", "secret"),
                policy);

        assertThatThrownBy(() -> engine.getTemplate("secret.peb"))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("rejected by policy");
    }

    @Test
    void filtersUserExtensionsThroughTheSamePolicy() {
        PebbleEngine engine = engine(
                Map.of("page.peb", "{{ name | shout }}"),
                SandboxPolicy.strict(),
                new ShoutExtension());

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter(),
                Map.of("name", "forge")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("shout");
    }

    @Test
    void requiresComponentSupportWhenComponentsAreEnabled() {
        SandboxPolicy policy = SandboxPolicy.builder()
                .allowOnlyTags("if")
                .allowOnlyFunctions("i18n")
                .build();

        assertThatThrownBy(() -> PebbleForge.engine()
                .loader(new MapLoader(Map.of()))
                .components(Components.defaults())
                .sandbox(policy)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowComponents");
    }

    static final class ExposedBean {

        public String secret = "field";
    }

    static final class StaticExposedBean {

        public static final String SECRET = "static-field";
    }

    static final class ShoutExtension extends AbstractExtension {

        @Override
        public Map<String, Filter> getFilters() {
            return Map.of("shout", new Filter() {
                @Override
                public List<String> getArgumentNames() {
                    return List.of();
                }

                @Override
                public Object apply(Object input, Map<String, Object> args,
                                    io.pebbletemplates.pebble.template.PebbleTemplate self,
                                    io.pebbletemplates.pebble.template.EvaluationContext context,
                                    int lineNumber) {
                    return String.valueOf(input).toUpperCase();
                }
            });
        }
    }
}
