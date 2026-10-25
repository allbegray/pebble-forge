package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.diagnostics.DiagnosticFrame;
import io.github.allbegray.pebbleforge.diagnostics.DiagnosticReport;
import io.github.allbegray.pebbleforge.diagnostics.TemplateDiagnostics;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class NaturalHtmlDiagnosticsTest {

    private static final String TEMPLATE = "page.html";

    @Test
    void rendersTheOriginalPositionForARejectedAttributeName() {
        Map<String, String> templates = Map.of(TEMPLATE, """
                <section>
                  <p data-{{ name }}="v">x</p>
                </section>
                """);
        MapLoader baseLoader = new MapLoader(templates);
        NaturalHtmlLoader loader = NaturalHtml.loader(baseLoader);

        NaturalHtmlException error = catchThrowableOfType(NaturalHtmlException.class,
                () -> PebbleForge.engine().loader(loader).build().getTemplate(TEMPLATE));

        DiagnosticReport report = TemplateDiagnostics.builder()
                .loader(baseLoader)
                .build()
                .render(error);

        assertThat(report.message()).contains("attribute names");
        assertThat(report.frames()).singleElement().satisfies(frame -> {
            assertThat(frame.kind()).isEqualTo(DiagnosticFrame.Kind.ERROR);
            assertThat(frame.template()).isEqualTo(TEMPLATE);
            assertThat(frame.line()).isEqualTo(2);
            assertThat(frame.caretColumn()).isEqualTo(11);
            assertThat(frame.label()).isNull();
            assertThat(frame.target()).isNull();
            assertThat(frame.sourceLines()).containsExactly(
                    "<section>", "  <p data-{{ name }}=\"v\">x</p>", "</section>");
        });
        assertThat(report.format())
                .contains("> 2 |   <p data-{{ name }}=\"v\">x</p>")
                .contains("    | " + " ".repeat(10) + "^");
    }
}
