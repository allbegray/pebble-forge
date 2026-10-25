package io.github.allbegray.pebbleforge.sandbox;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerTemplatePolicyTest {

    @Test
    void appliesFilterAllowlistsByTemplate() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.strict();
        SandboxPolicy adminPolicy = SandboxPolicy.builder().allowRawOutput().build();
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", "{{ value | raw }}",
                        "admin/page.peb", "{{ value | raw }}"),
                resolver(publicPolicy, adminPolicy));

        assertThatThrownBy(() -> render(engine, "public/page.peb", Map.of("value", "<b>x</b>")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Filter [raw] is not allowed");
        assertThat(render(engine, "admin/page.peb", Map.of("value", "<b>x</b>")))
                .isEqualTo("<b>x</b>");
    }

    @Test
    void appliesTagAllowlistsByTemplate() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.strict();
        SandboxPolicy adminPolicy = SandboxPolicy.builder().allowTags("macro").build();
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", "{% macro badge() %}x{% endmacro %}{{ badge() }}",
                        "admin/page.peb", "{% macro badge() %}x{% endmacro %}{{ badge() }}"),
                resolver(publicPolicy, adminPolicy));

        assertThatThrownBy(() -> engine.getTemplate("public/page.peb").evaluate(new StringWriter()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Tag [macro] is not allowed");
        StringWriter writer = new StringWriter();
        engine.getTemplate("admin/page.peb").evaluate(writer);
        assertThat(writer.toString()).isEqualTo("x");
    }

    @Test
    void appliesMethodAccessByTemplateInEitherRenderOrder() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder()
                .methodAccessValidator((object, method) ->
                        !method.getName().equals("toUpperCase"))
                .build();
        SandboxPolicy adminPolicy = SandboxPolicy.strict();
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", "{{ \"x\".toUpperCase() }}",
                        "admin/page.peb", "{{ \"x\".toUpperCase() }}"),
                resolver(publicPolicy, adminPolicy));

        assertThat(render(engine, "admin/page.peb", Map.of())).isEqualTo("X");
        assertThatThrownBy(() -> render(engine, "public/page.peb", Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("toUpperCase");
    }

    @Test
    void appliesFunctionAllowlistsByTemplate() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.strict();
        SandboxPolicy adminPolicy = SandboxPolicy.builder().denyFunctions("max").build();
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", "{{ max(1, 2) }}",
                        "admin/page.peb", "{{ max(1, 2) }}"),
                resolver(publicPolicy, adminPolicy));

        assertThat(render(engine, "public/page.peb", Map.of())).isEqualTo("2");
        assertThatThrownBy(() -> render(engine, "admin/page.peb", Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Function [max] is not allowed");
    }

    @Test
    void includedTemplatesUseTheirOwnPolicy() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.strict();
        SandboxPolicy adminPolicy = SandboxPolicy.builder().allowRawOutput().build();
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", "{% include \"admin/raw.peb\" %}",
                        "admin/raw.peb", "{{ value | raw }}"),
                resolver(publicPolicy, adminPolicy));

        assertThat(render(engine, "public/page.peb", Map.of("value", "<b>x</b>")))
                .isEqualTo("<b>x</b>");
    }

    @Test
    void keepsDefinedWorkingWhenOnlyOnePolicyAllowsIt() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder()
                .allowTests("defined")
                .build();
        SandboxPolicy adminPolicy = SandboxPolicy.strict();
        String template = "{{ missing is defined }}";
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of(
                        "public/page.peb", template,
                        "admin/page.peb", template)))
                .policies(resolver(publicPolicy, adminPolicy))
                .strictVariables(true)
                .build();

        assertThat(render(engine, "public/page.peb", Map.of())).isEqualTo("false");
        assertThatThrownBy(() -> render(engine, "admin/page.peb", Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Test [defined] is not allowed");
    }

    @Test
    void keepsDefaultFilterWorkingWhenOnlyOnePolicyAllowsIt() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.strict();
        SandboxPolicy adminPolicy = SandboxPolicy.builder().denyFilters("default").build();
        String template = "{{ missing | default(\"x\") }}";
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of(
                        "public/page.peb", template,
                        "admin/page.peb", template)))
                .policies(resolver(publicPolicy, adminPolicy))
                .strictVariables(true)
                .build();

        assertThat(render(engine, "public/page.peb", Map.of())).isEqualTo("x");
        assertThatThrownBy(() -> render(engine, "admin/page.peb", Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Filter [default] is not allowed");
    }

    @Test
    void embedOverridesUseThePolicyOfTheDeclaringTemplate() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder()
                .allowTags("embed", "block")
                .build();
        SandboxPolicy adminPolicy = SandboxPolicy.builder()
                .allowTags("embed", "block")
                .allowRawOutput()
                .build();
        String page = "{% embed \"admin/base.peb\" %}"
                + "{% block body %}{{ value | raw }}{% endblock %}"
                + "{% endembed %}";
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", page,
                        "admin/page.peb", page,
                        "admin/base.peb", "<div>{% block body %}base{% endblock %}</div>"),
                resolver(publicPolicy, adminPolicy));

        assertThatThrownBy(() -> render(engine, "public/page.peb", Map.of("value", "<b>x</b>")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Filter [raw] is not allowed for template [public/page.peb]");
        assertThat(render(engine, "admin/page.peb", Map.of("value", "<b>x</b>")))
                .isEqualTo("<div><b>x</b></div>");
    }

    @Test
    void extendsBlocksUseThePolicyOfTheDeclaringTemplate() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder()
                .allowTags("extends", "block")
                .build();
        SandboxPolicy adminPolicy = SandboxPolicy.builder()
                .allowTags("extends", "block")
                .allowRawOutput()
                .build();
        String page = "{% extends \"admin/layout.peb\" %}"
                + "{% block content %}{{ value | raw }}{% endblock %}";
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", page,
                        "admin/page.peb", page,
                        "admin/layout.peb", "<html>{% block content %}base{% endblock %}</html>"),
                resolver(publicPolicy, adminPolicy));

        assertThatThrownBy(() -> render(engine, "public/page.peb", Map.of("value", "<b>x</b>")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Filter [raw] is not allowed for template [public/page.peb]");
        assertThat(render(engine, "admin/page.peb", Map.of("value", "<b>x</b>")))
                .isEqualTo("<html><b>x</b></html>");
    }

    @Test
    void includedTemplatesInsideBlocksUseTheirOwnPolicy() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder().allowTags("block").build();
        SandboxPolicy adminPolicy = SandboxPolicy.builder().allowRawOutput().build();
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", "{% block content %}"
                                + "{% include \"admin/raw.peb\" %}"
                                + "{% endblock %}",
                        "admin/raw.peb", "{{ value | raw }}"),
                resolver(publicPolicy, adminPolicy));

        assertThat(render(engine, "public/page.peb", Map.of("value", "<b>x</b>")))
                .isEqualTo("<b>x</b>");
    }

    @Test
    void macrosUseThePolicyOfTheDefiningTemplate() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder()
                .allowTags("import", "macro", "block")
                .build();
        SandboxPolicy adminPolicy = SandboxPolicy.builder()
                .allowTags("macro")
                .allowRawOutput()
                .build();
        PebbleEngine engine = engine(
                Map.of(
                        "public/page.peb", "{% import \"admin/macros.peb\" as m %}"
                                + "{% block content %}{{ m.raw(value) }}{% endblock %}",
                        "admin/macros.peb", "{% macro raw(value) %}{{ value | raw }}{% endmacro %}"),
                resolver(publicPolicy, adminPolicy));

        assertThat(render(engine, "public/page.peb", Map.of("value", "<b>x</b>")))
                .isEqualTo("<b>x</b>");
    }

    @Test
    void componentsUseTheirOwnPolicy() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder()
                .allowComponents()
                .allowTags("block")
                .build();
        SandboxPolicy adminPolicy = SandboxPolicy.builder()
                .allowComponents()
                .allowRawOutput()
                .build();
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of(
                        "public/page.peb", "{% block content %}"
                                + "{% component \"raw\" with {\"value\": value} %}"
                                + "{% endcomponent %}{% endblock %}",
                        "components/raw.peb", "{{ value | raw }}")))
                .components(Components.defaults())
                .policies(SandboxPolicyResolver.builder(publicPolicy)
                        .prefix("components/", adminPolicy)
                        .build())
                .build();

        assertThat(render(engine, "public/page.peb", Map.of("value", "<b>x</b>")))
                .isEqualTo("<b>x</b>");
    }

    @Test
    void literalTemplatesUseTheDefaultPolicy() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.strict();
        SandboxPolicy adminPolicy = SandboxPolicy.builder().allowRawOutput().build();
        PebbleEngine engine = engine(Map.of(), resolver(publicPolicy, adminPolicy));
        PebbleTemplate literal = engine.getLiteralTemplate("admin/{{ value | raw }}");

        assertThatThrownBy(() -> literal.evaluate(
                new StringWriter(), Map.of("value", "<b>x</b>")))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Filter [raw] is not allowed");
    }

    @Test
    void usesTheStrictestOutputLimit() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder().maxRenderedChars(10).build();
        SandboxPolicy adminPolicy = SandboxPolicy.builder().maxRenderedChars(100).build();
        PebbleEngine engine = engine(
                Map.of("admin/page.peb", "12345678901234567890"),
                resolver(publicPolicy, adminPolicy));

        assertThatThrownBy(() -> render(engine, "admin/page.peb", Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("more than 10 chars");
    }

    @Test
    void autoEscapesWhenAnyPolicyForcesIt() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder()
                .forceAutoEscaping(true)
                .build();
        SandboxPolicy adminPolicy = SandboxPolicy.builder()
                .forceAutoEscaping(false)
                .build();
        PebbleEngine engine = engine(
                Map.of("admin/page.peb", "{{ value }}"),
                resolver(publicPolicy, adminPolicy));

        assertThat(render(engine, "admin/page.peb", Map.of("value", "<b>x</b>")))
                .isEqualTo("&lt;b&gt;x&lt;/b&gt;");
    }

    @Test
    void exposesGlobalVariablesOnlyWhenEveryPolicyAllowsThem() throws Exception {
        AbstractExtension extension = new AbstractExtension() {
            @Override
            public Map<String, Object> getGlobalVariables() {
                return Map.of("secret", "value");
            }
        };
        SandboxPolicy allowed = SandboxPolicy.builder()
                .allowGlobalVariables("secret")
                .build();
        SandboxPolicy denied = SandboxPolicy.strict();

        PebbleEngine allAllowed = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", "{{ secret }}")))
                .policies(SandboxPolicyResolver.builder(allowed)
                        .prefix("admin/", allowed)
                        .build())
                .extension(extension)
                .strictVariables(true)
                .build();
        assertThat(render(allAllowed, "page.peb", Map.of())).isEqualTo("value");

        PebbleEngine oneDenied = PebbleForge.engine()
                .loader(new MapLoader(Map.of("public/page.peb", "{{ secret }}")))
                .policies(resolver(allowed, denied))
                .extension(extension)
                .strictVariables(true)
                .build();
        assertThatThrownBy(() -> render(oneDenied, "public/page.peb", Map.of()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("secret");
    }

    @Test
    void validatesTemplateNamesWithTheTargetPolicy() throws Exception {
        SandboxPolicy publicPolicy = SandboxPolicy.builder()
                .templateNameValidator(name -> !name.contains("blocked"))
                .build();
        SandboxPolicy adminPolicy = SandboxPolicy.strict();
        PebbleEngine engine = engine(
                Map.of(
                        "public/blocked.peb", "public",
                        "admin/blocked.peb", "admin"),
                resolver(publicPolicy, adminPolicy));

        assertThatThrownBy(() -> engine.getTemplate("public/blocked.peb"))
                .isInstanceOf(LoaderException.class)
                .hasMessageContaining("rejected by policy");
        assertThat(engine.getTemplate("admin/blocked.peb")).isNotNull();
    }

    @Test
    void rejectsWhitespaceInTemplateNames() {
        PebbleEngine engine = engine(
                Map.of(" admin/page.peb", "admin"),
                resolver(SandboxPolicy.strict(), SandboxPolicy.strict()));

        assertThatThrownBy(() -> engine.getTemplate(" admin/page.peb"))
                .isInstanceOf(LoaderException.class)
                .hasMessageContaining("invalid character");
    }

    @Test
    void rejectsConflictingBuilderConfiguration() {
        SandboxPolicy policy = SandboxPolicy.strict();

        assertThatThrownBy(() -> PebbleForge.engine()
                .sandbox(policy)
                .policies(SandboxPolicyResolver.fixed(policy))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not both");

        assertThatThrownBy(() -> PebbleForge.engine()
                .policies(SandboxPolicyResolver.fixed(policy))
                .methodAccessValidator((object, method) -> true)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("methodAccessValidator");
    }

    private static SandboxPolicyResolver resolver(SandboxPolicy publicPolicy,
            SandboxPolicy adminPolicy) {
        return SandboxPolicyResolver.builder(publicPolicy)
                .prefix("admin/", adminPolicy)
                .build();
    }

    private static PebbleEngine engine(Map<String, String> templates,
            SandboxPolicyResolver policies) {
        return PebbleForge.engine()
                .loader(new MapLoader(templates))
                .policies(policies)
                .build();
    }

    private static String render(PebbleEngine engine, String template, Map<String, Object> model)
            throws Exception {
        StringWriter writer = new StringWriter();
        engine.getTemplate(template).evaluate(writer, model);
        return writer.toString();
    }
}
