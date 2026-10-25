package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.pebbletemplates.pebble.PebbleEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalHtmlXssTest {

    private static final String TEMPLATE = "page.html";
    private static final String TEXT = "<p>{{ value }}</p>";
    private static final String ATTRIBUTE = "<p title=\"{{ value }}\">x</p>";
    private static final String SINGLE_ATTRIBUTE = "<p title='{{ value }}'>x</p>";
    private static final String URL = "<a href=\"{{ value }}\">x</a>";
    private static final String PREFIX_URL = "<a href=\"/u/{{ value }}\">x</a>";

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void neutralizesTheBreakoutToken(Case fixture) throws IOException {
        String output = render(fixture.template(), fixture.value());

        for (String required : fixture.required()) {
            assertThat(output)
                    .as("%s must contain %s", fixture.label(), required)
                    .contains(required);
        }
        for (String forbidden : fixture.forbidden()) {
            assertThat(output)
                    .as("%s must not contain %s", fixture.label(), forbidden)
                    .doesNotContain(forbidden);
        }
    }

    private static Stream<Arguments> fixtures() {
        return Stream.of(
                Arguments.of(new Case("text script tag", TEXT,
                        "<script>alert(1)</script>",
                        List.of("&lt;script&gt;"), List.of("<script", "</script>"))),
                Arguments.of(new Case("text image handler", TEXT,
                        "<img src=x onerror=alert(1)>",
                        List.of("&lt;img"), List.of("<img"))),
                Arguments.of(new Case("text closing tag", TEXT,
                        "</p><script>alert(1)</script>",
                        List.of("&lt;/p&gt;"), List.of("</p><script", "<script"))),
                Arguments.of(new Case("double quote attribute breakout", ATTRIBUTE,
                        "\" onmouseover=\"alert(1)",
                        List.of("&quot;&#32;onmouseover&#61;"),
                        List.of("\" onmouseover="))),
                Arguments.of(new Case("single quote attribute breakout", SINGLE_ATTRIBUTE,
                        "' onfocus='alert(1)",
                        List.of("&#39;&#32;onfocus&#61;"),
                        List.of("' onfocus="))),
                Arguments.of(new Case("backtick attribute breakout", ATTRIBUTE,
                        "` onmouseover=alert(1)",
                        List.of("&#96;&#32;onmouseover&#61;"),
                        List.of("` onmouseover="))),
                Arguments.of(new Case("entity attribute breakout", ATTRIBUTE,
                        "&quot;", List.of("&amp;quot;"), List.of("&quot;"))),
                Arguments.of(new Case("tag injection through attribute", ATTRIBUTE,
                        "\"><script>alert(1)</script>",
                        List.of("&quot;&gt;&lt;script&gt;"),
                        List.of("\"><script", "<script"))),
                Arguments.of(new Case("javascript scheme", URL,
                        "javascript:alert(1)", List.of("href=\"#\""), List.of("javascript"))),
                Arguments.of(new Case("mixed case javascript scheme", URL,
                        "JaVaScRiPt:alert(1)", List.of("href=\"#\""), List.of("JaVaScRiPt"))),
                Arguments.of(new Case("tab split javascript scheme", URL,
                        "java\tscript:alert(1)", List.of("href=\"#\""), List.of("script"))),
                Arguments.of(new Case("data scheme", URL,
                        "data:text/html,<script>alert(1)</script>",
                        List.of("href=\"#\""), List.of("data:", "<script"))),
                Arguments.of(new Case("vbscript scheme", URL,
                        "vbscript:msgbox(1)", List.of("href=\"#\""), List.of("vbscript"))),
                Arguments.of(new Case("leading whitespace javascript scheme", URL,
                        "  javascript:alert(1)", List.of("href=\"#\""), List.of("javascript"))),
                Arguments.of(new Case("query payload in a prefix url", PREFIX_URL,
                        "?x=1&y=2\"><script>alert(1)</script>",
                        List.of("/u/?x&#61;1&amp;y&#61;2&quot;&gt;&lt;script&gt;"),
                        List.of("<script", "\"><"))),
                Arguments.of(new Case("query payload in a whole value url", URL,
                        "?x=1&y=2\"><script>alert(1)</script>",
                        List.of("?x&#61;1&amp;y&#61;2&quot;&gt;&lt;script&gt;"),
                        List.of("<script", "\"><"))),
                Arguments.of(new Case("safe absolute value with a payload", URL,
                        "https://example.com/?q=\"><script>alert(1)</script>",
                        List.of("https://example.com/?q&#61;&quot;&gt;&lt;script&gt;"),
                        List.of("<script", "\"><"))));
    }

    private static String render(String template, String value) throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(NaturalHtml.loader(new MapLoader(Map.of(TEMPLATE, template))))
                .build();
        StringWriter writer = new StringWriter();
        engine.getTemplate(TEMPLATE).evaluate(writer, Map.of("value", value));
        return writer.toString();
    }

    private record Case(String label, String template, String value,
                        List<String> required, List<String> forbidden) {
    }
}
