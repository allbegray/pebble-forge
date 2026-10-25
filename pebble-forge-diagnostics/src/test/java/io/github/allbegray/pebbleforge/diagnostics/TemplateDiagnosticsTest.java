package io.github.allbegray.pebbleforge.diagnostics;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.diagnostics.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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

    private static TemplateDiagnostics diagnostics(Map<String, String> templates) {
        return TemplateDiagnostics.builder()
                .loader(new MapLoader(templates))
                .build();
    }

    private static PebbleException renderError(Map<String, String> templates) {
        return catchThrowableOfType(
                PebbleException.class,
                () -> engine(templates)
                        .getTemplate("page.peb")
                        .evaluate(new StringWriter()));
    }

    private static PebbleEngine engine(Map<String, String> templates) {
        return PebbleForge.engine()
                .loader(new MapLoader(templates))
                .strictVariables(true)
                .build();
    }
}
