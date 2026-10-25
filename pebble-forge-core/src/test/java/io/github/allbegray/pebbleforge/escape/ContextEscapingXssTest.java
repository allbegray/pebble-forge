package io.github.allbegray.pebbleforge.escape;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContextEscapingXssTest {

    private static final String HTML_TEXT = "{{ value }}";
    private static final String HTML_ATTRIBUTE = "{{ value | escape('html_attr') }}";
    private static final String URL = "{{ value | escape('url') }}";
    private static final String JAVASCRIPT = "{{ value | escape('js') }}";
    private static final String CSS = "{{ value | escape('css') }}";

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void neutralizesTheBreakoutToken(Fixture fixture) throws IOException {
        String output = render(fixture.template(), fixture.input());

        for (String safe : fixture.safeMarkers()) {
            assertThat(output)
                    .as("%s must contain %s", fixture.label(), safe)
                    .contains(safe);
        }
        for (String forbidden : fixture.forbidden()) {
            assertThat(output)
                    .as("%s must not contain %s", fixture.label(), forbidden)
                    .doesNotContain(forbidden);
        }
        for (String pattern : fixture.forbiddenPatterns()) {
            assertThat(output)
                    .as("%s must not match %s", fixture.label(), pattern)
                    .doesNotContainPattern(pattern);
        }
    }

    private static Stream<Fixture> fixtures() {
        return Stream.of(
                new Fixture("html script tag", HTML_TEXT,
                        "<script>alert(1)</script>",
                        List.of("&lt;script&gt;"), List.of("<script>", "</script>")),
                new Fixture("html img onerror", HTML_TEXT,
                        "<img src=x onerror=alert(1)>",
                        List.of("&lt;img"), List.of("<img")),
                new Fixture("double quote attribute breakout", HTML_ATTRIBUTE,
                        "\" onmouseover=\"alert(1)",
                        List.of("&quot;&#32;onmouseover&#61;"),
                        List.of("\" onmouseover=")),
                new Fixture("single quote attribute breakout", HTML_ATTRIBUTE,
                        "' onfocus='alert(1)",
                        List.of("&#39;&#32;onfocus&#61;"),
                        List.of("' onfocus=")),
                new Fixture("backtick attribute breakout", HTML_ATTRIBUTE,
                        "` onmouseover=alert(1)",
                        List.of("&#96;&#32;onmouseover&#61;"),
                        List.of("` onmouseover=")),
                new Fixture("entity attribute breakout", HTML_ATTRIBUTE,
                        "&quot;",
                        List.of("&amp;quot;"), List.of("&quot;")),
                new Fixture("javascript scheme", URL,
                        "javascript:alert(1)", List.of("#"), List.of("javascript")),
                new Fixture("mixed case javascript scheme", URL,
                        "JaVaScRiPt:alert(1)", List.of("#"), List.of("JaVaScRiPt")),
                new Fixture("tab split javascript scheme", URL,
                        "java\tscript:alert(1)", List.of("#"), List.of("script")),
                new Fixture("data scheme", URL,
                        "data:text/html,<script>alert(1)</script>",
                        List.of("#"), List.of("data:", "<script>")),
                new Fixture("vbscript scheme", URL,
                        "vbscript:msgbox(1)", List.of("#"), List.of("vbscript")),
                new Fixture("url query payload", URL,
                        "https://e/?q=<script>&x=\"y\"",
                        List.of("&lt;script&gt;", "&quot;"),
                        List.of("<script>", "\"")),
                new Fixture("script terminator", JAVASCRIPT,
                        "</script><script>alert(1)</script>",
                        List.of("<\\/script>"), List.of("</script>")),
                new Fixture("script quote breakout", JAVASCRIPT,
                        "\";alert(1);//",
                        List.of("\\\";alert(1)"), List.of(),
                        List.of("(?<!\\\\)\"")),
                new Fixture("script line separators", JAVASCRIPT,
                        "a\u2028b\u2029c",
                        List.of("\\u2028", "\\u2029"), List.of("\u2028", "\u2029")),
                new Fixture("style terminator", CSS,
                        "</style><script>alert(1)</script>",
                        List.of("\\<\\/style\\>"), List.of("</style>", "</script>")),
                new Fixture("css javascript url", CSS,
                        "url(javascript:alert(1))",
                        List.of("url\\(", "\\3A "), List.of("url(", "javascript:")),
                new Fixture("css expression", CSS,
                        "expression(alert(1))",
                        List.of("expression\\("), List.of("expression("))
        );
    }

    private static String render(String template, String value) throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", template)))
                .build();
        StringWriter writer = new StringWriter();
        engine.getTemplate("page.peb").evaluate(writer, Map.of("value", value));
        return writer.toString();
    }

    private record Fixture(String label, String template, String input,
                           List<String> safeMarkers, List<String> forbidden,
                           List<String> forbiddenPatterns) {

        Fixture(String label, String template, String input,
                List<String> safeMarkers, List<String> forbidden) {
            this(label, template, input, safeMarkers, forbidden, List.of());
        }
    }
}
