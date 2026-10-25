package io.github.allbegray.pebbleforge.diagnostics;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.diagnostics.testing.MapLoader;
import io.github.allbegray.pebbleforge.trace.TemplateTrace;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.groups.Tuple.tuple;

class TemplateDiagnosticsTest {

    @Test
    void rendersSourceFrameForRenderError() {
        Map<String, String> templates = Map.of("page.peb", """
                first
                second
                {{ missing }}
                """);
        PebbleException error = renderError(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.format())
                .contains("page.peb:3")
                .contains("> 3 | {{ missing }}")
                .contains("^")
                .contains("[missing]");
    }

    @Test
    void rendersIncludeStack() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        first
                        {% include "components/card.peb" %}
                        """,
                "components/card.peb", """
                        card
                        {{ missing }}
                        """);
        PebbleException error = renderError(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("page.peb", "components/card.peb");
        assertThat(report.format())
                .contains("page.peb:2 includes components/card.peb")
                .contains("components/card.peb:2")
                .contains("> 2 | {{ missing }}");
    }

    @Test
    void rendersNestedIncludeStackInOrder() {
        Map<String, String> templates = Map.of(
                "page.peb", "{% include \"a.peb\" %}",
                "a.peb", "{% include \"b.peb\" %}",
                "b.peb", "{{ missing }}");
        PebbleException error = renderError(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("page.peb", "a.peb", "b.peb");
    }

    @Test
    void rendersSourceFrameForParseError() {
        Map<String, String> templates = Map.of("page.peb", "{% if %}");
        PebbleException error = catchThrowableOfType(
                PebbleException.class, () -> engine(templates).getTemplate("page.peb"));

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.format())
                .contains("page.peb:1")
                .contains("{% if %}");
    }

    @Test
    void handlesNonPebbleErrors() {
        DiagnosticReport report = diagnostics(Map.of()).render(new IllegalStateException("boom"));

        assertThat(report.format()).contains("boom");
        assertThat(report.frames()).isEmpty();
    }

    @Test
    void handlesMissingSource() {
        Map<String, String> templates = Map.of("page.peb", "{{ missing }}");
        PebbleException error = renderError(templates);
        DiagnosticReport report = TemplateDiagnostics.builder()
                .loader(new MapLoader(Map.of()))
                .build()
                .render(error);

        assertThat(report.format()).contains("source unavailable");
    }

    @Test
    void limitsContextLines() {
        Map<String, String> templates = Map.of("page.peb", """
                one
                two
                three
                {{ missing }}
                five
                six
                """);
        PebbleException error = renderError(templates);
        DiagnosticReport report = TemplateDiagnostics.builder()
                .loader(new MapLoader(templates))
                .contextLines(0)
                .build()
                .render(error);

        assertThat(report.format())
                .contains("> 4 | {{ missing }}")
                .doesNotContain("three")
                .doesNotContain("five");
    }

    @Test
    void exposesIncludeKindAndTarget() {
        Map<String, String> templates = Map.of(
                "page.peb", "{% include \"components/card.peb\" %}",
                "components/card.peb", "{{ missing }}");
        PebbleException error = renderError(templates);

        DiagnosticFrame frame = diagnostics(templates).render(error).frames().getFirst();

        assertThat(frame.kind()).isEqualTo(DiagnosticFrame.Kind.INCLUDE);
        assertThat(frame.target()).isEqualTo("components/card.peb");
        assertThat(frame.label()).isEqualTo("includes components/card.peb");
    }

    @Test
    void exposesExtendsKindAndTarget() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% extends "layout.peb" %}
                        {% block body %}child{% endblock %}
                        """,
                "layout.peb", "layout\n{{ missing }}");
        PebbleException error = renderErrorTraced(templates);

        DiagnosticFrame frame = diagnostics(templates).render(error).frames().getFirst();

        assertThat(frame.kind()).isEqualTo(DiagnosticFrame.Kind.EXTENDS);
        assertThat(frame.target()).isEqualTo("layout.peb");
        assertThat(frame.label()).isEqualTo("extends layout.peb");
    }

    @Test
    void exposesErrorKindWithoutTargetForTheFailingFrame() {
        Map<String, String> templates = Map.of(
                "page.peb", "{% include \"card.peb\" %}",
                "card.peb", "{{ missing }}");
        PebbleException error = renderError(templates);

        DiagnosticFrame frame = diagnostics(templates).render(error).frames().getLast();

        assertThat(frame.template()).isEqualTo("card.peb");
        assertThat(frame.kind()).isEqualTo(DiagnosticFrame.Kind.ERROR);
        assertThat(frame.target()).isNull();
        assertThat(frame.label()).isNull();
    }

    @Test
    void defaultsLegacyConstructorFramesToErrorKind() {
        DiagnosticFrame frame = new DiagnosticFrame("page.peb", 3, null,
                List.of("{{ missing }}"), 0, 1);

        assertThat(frame.kind()).isEqualTo(DiagnosticFrame.Kind.ERROR);
        assertThat(frame.target()).isNull();
    }

    @Test
    void treatsNullKindAsError() {
        DiagnosticFrame frame = new DiagnosticFrame("page.peb", 3, null,
                List.of("{{ missing }}"), 0, 1, null, null);

        assertThat(frame.kind()).isEqualTo(DiagnosticFrame.Kind.ERROR);
    }

    @Test
    void formatsFramesTheSameForEveryKind() {
        List<String> source = List.of("{{ missing }}");
        DiagnosticFrame legacy = new DiagnosticFrame("page.peb", 3, "includes card.peb",
                source, 0, 1);
        DiagnosticFrame include = new DiagnosticFrame("page.peb", 3, "includes card.peb",
                source, 0, 1, DiagnosticFrame.Kind.INCLUDE, "card.peb");
        DiagnosticFrame extendsFrame = new DiagnosticFrame("page.peb", 3, "includes card.peb",
                source, 0, 1, DiagnosticFrame.Kind.EXTENDS, "layout.peb");
        DiagnosticFrame error = new DiagnosticFrame("page.peb", 3, "includes card.peb",
                source, 0, 1, DiagnosticFrame.Kind.ERROR, null);

        assertThat(include.format()).isEqualTo(legacy.format());
        assertThat(extendsFrame.format()).isEqualTo(legacy.format());
        assertThat(error.format()).isEqualTo(legacy.format());
        assertThat(legacy.format()).isEqualTo(
                "page.peb:3 includes card.peb" + System.lineSeparator()
                        + "> 3 | {{ missing }}" + System.lineSeparator()
                        + "    | ^" + System.lineSeparator());
    }

    @Test
    void rendersExtendsLayoutForAParentBodyError() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% extends "layout.peb" %}
                        {% block body %}child{% endblock %}
                        """,
                "layout.peb", """
                        layout
                        {{ missing }}
                        """);
        PebbleException error = renderErrorTraced(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("page.peb", "layout.peb");
        assertThat(report.format())
                .contains("page.peb:1 extends layout.peb")
                .contains("layout.peb:2")
                .contains("> 2 | {{ missing }}");
    }

    @Test
    void rendersNestedExtendsChainInOrder() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% extends "middle.peb" %}
                        {% block body %}child{% endblock %}
                        """,
                "middle.peb", "{% extends \"base.peb\" %}",
                "base.peb", """
                        base
                        {% block body %}base{% endblock %}
                        {{ missing }}
                        """);
        PebbleException error = renderErrorTraced(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("page.peb", "middle.peb", "base.peb");
        assertThat(report.format())
                .contains("page.peb:1 extends middle.peb")
                .contains("middle.peb:1 extends base.peb")
                .contains("base.peb:3");
    }

    @Test
    void rendersExtendsAndIncludeInChronologicalOrder() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% extends "layout.peb" %}
                        {% block body %}child{% endblock %}
                        """,
                "layout.peb", """
                        before
                        {% include "card.peb" %}
                        {% block body %}base{% endblock %}
                        """,
                "card.peb", "card\n{{ missing }}");
        PebbleException error = renderErrorTraced(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.frames()).extracting(DiagnosticFrame::template,
                        DiagnosticFrame::label)
                .containsExactly(
                        tuple("page.peb", "extends layout.peb"),
                        tuple("layout.peb", "includes card.peb"),
                        tuple("card.peb", null));
    }

    @Test
    void rendersExtendsEdgeForAnErrorInAnOverriddenChildBlock() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% extends "layout.peb" %}
                        {% block body %}{{ missing }}{% endblock %}
                        """,
                "layout.peb", "<html>{% block body %}base{% endblock %}</html>");
        PebbleException error = renderErrorTraced(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.format()).contains("page.peb:1 extends layout.peb");
        assertThat(report.frames().getFirst().label()).isEqualTo("extends layout.peb");
        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("page.peb", "page.peb");
    }

    @Test
    void rendersParentParseFailureWithTheExtendsEdge() {
        Map<String, String> templates = Map.of(
                "page.peb", "{% extends \"broken.peb\" %}",
                "broken.peb", "{% if %}");
        PebbleException error = renderError(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("page.peb", "broken.peb");
        assertThat(report.format()).contains("page.peb:1 extends broken.peb");
    }

    @Test
    void rendersDynamicParentTargetFromTheModel() throws Exception {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% extends layoutName %}
                        {% block body %}child{% endblock %}
                        """,
                "layout.peb", "layout\n{{ missing }}");
        PebbleException error = catchThrowableOfType(PebbleException.class,
                () -> TemplateTrace.run(() -> engine(templates)
                        .getTemplate("page.peb")
                        .evaluate(new StringWriter(), Map.of("layoutName", "layout.peb"))));

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.format()).contains("page.peb:1 extends layout.peb");
        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("page.peb", "layout.peb");
    }

    @Test
    void rendersMissingParentWithoutCrashing() {
        Map<String, String> templates = Map.of(
                "page.peb", "{% extends \"missing.peb\" %}");
        PebbleException error = renderError(templates);

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.frames()).isNotEmpty();
        assertThat(report.format())
                .contains("page.peb:1 extends missing.peb")
                .contains("missing.peb");
    }

    @Test
    void reportsExtendsCyclesWithoutStackOverflow() {
        Map<String, String> templates = Map.of(
                "a.peb", "{% extends \"b.peb\" %}",
                "b.peb", "{% extends \"a.peb\" %}");
        PebbleException error = renderError(templates, "a.peb");

        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(error)
                .hasMessageContaining("Extends cycle")
                .hasMessageNotContaining("StackOverflow");
        assertThat(report.format())
                .contains("a.peb:1 extends b.peb")
                .contains("b.peb:1 extends a.peb");
    }

    @Test
    void successfulExtendsRenderLeavesNoStaleFrames() throws Exception {
        Map<String, String> templates = Map.of(
                "good.peb", """
                        {% extends "layout.peb" %}
                        {% block body %}ok{% endblock %}
                        """,
                "layout.peb", "[{% block body %}base{% endblock %}]",
                "page.peb", "{{ missing }}");
        engine(templates).getTemplate("good.peb").evaluate(new StringWriter());

        PebbleException error = renderErrorTraced(templates);
        DiagnosticReport report = diagnostics(templates).render(error);

        assertThat(report.frames()).extracting(DiagnosticFrame::template)
                .containsExactly("page.peb");
        assertThat(report.format()).doesNotContain("extends");
    }

    private static TemplateDiagnostics diagnostics(Map<String, String> templates) {
        return TemplateDiagnostics.builder()
                .loader(new MapLoader(templates))
                .build();
    }

    private static PebbleException renderError(Map<String, String> templates) {
        return renderError(templates, "page.peb");
    }

    private static PebbleException renderError(Map<String, String> templates, String template) {
        return catchThrowableOfType(
                PebbleException.class,
                () -> engine(templates)
                        .getTemplate(template)
                        .evaluate(new StringWriter()));
    }

    private static PebbleException renderErrorTraced(Map<String, String> templates) {
        return catchThrowableOfType(
                PebbleException.class,
                () -> TemplateTrace.run(() -> engine(templates)
                        .getTemplate("page.peb")
                        .evaluate(new StringWriter())));
    }

    private static PebbleEngine engine(Map<String, String> templates) {
        return PebbleForge.engine()
                .loader(new MapLoader(templates))
                .strictVariables(true)
                .build();
    }
}
