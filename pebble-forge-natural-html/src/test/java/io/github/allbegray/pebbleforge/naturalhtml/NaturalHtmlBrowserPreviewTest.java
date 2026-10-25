package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.pebbletemplates.pebble.PebbleEngine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalHtmlBrowserPreviewTest {

    private static final String TEMPLATE = "preview.html";

    private static final String SOURCE = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <title>{{ title }}</title>
            </head>
            <body>
              <h1>{{ heading }}</h1>
              <a href="/users/{{ id }}" title="{{ tooltip }}">{{ label }}</a>
              <p>Static text stays static.</p>
            </body>
            </html>
            """;

    @Test
    void originalSourceStaysReadableByAnHtmlParser() {
        Parser parser = Parser.htmlParser().setTrackPosition(true).setTrackErrors(100);
        Document document = parser.parseInput(SOURCE, TEMPLATE);

        assertThat(document.parser().getErrors()).isEmpty();
        assertThat(document.title()).isEqualTo("{{ title }}");
        assertThat(document.selectFirst("h1").text()).isEqualTo("{{ heading }}");
        assertThat(document.selectFirst("a").attr("href")).isEqualTo("/users/{{ id }}");
        assertThat(document.selectFirst("a").attr("title")).isEqualTo("{{ tooltip }}");
        assertThat(document.selectFirst("a").text()).isEqualTo("{{ label }}");

        for (Element element : document.getAllElements()) {
            assertThat(element.tagName()).doesNotContain("{");
            for (Attribute attribute : element.attributes()) {
                assertThat(attribute.getKey()).doesNotContain("{");
            }
        }
    }

    @Test
    void renderedOutputParsesWithoutInjectedMarkup() throws IOException {
        String rendered = render(Map.of(
                "title", "<script>alert(1)</script>",
                "heading", "<img src=x onerror=alert(1)>",
                "id", "42\"><script>alert(1)</script>",
                "tooltip", "\" onmouseover=\"alert(1)",
                "label", "<b>hello</b>"));

        assertThat(rendered).doesNotContain("<script", "<img", "onmouseover=");

        Document document = Jsoup.parse(rendered);
        assertThat(document.select("script")).isEmpty();
        assertThat(document.select("img")).isEmpty();
        assertThat(document.select("[onmouseover]")).isEmpty();
        assertThat(document.select("[onerror]")).isEmpty();
        assertThat(document.title()).isEqualTo("<script>alert(1)</script>");
        assertThat(document.selectFirst("h1").text()).isEqualTo("<img src=x onerror=alert(1)>");
        assertThat(document.selectFirst("a").text()).isEqualTo("<b>hello</b>");
        assertThat(document.selectFirst("a").hasAttr("onmouseover")).isFalse();
        assertThat(document.selectFirst("a").attr("title"))
                .isEqualTo("\" onmouseover=\"alert(1)");
        assertThat(document.selectFirst("a").attr("href"))
                .startsWith("/users/42");

        for (Element element : document.getAllElements()) {
            for (Attribute attribute : element.attributes()) {
                assertThat(attribute.getKey()).doesNotStartWith("on");
            }
        }
    }

    @Test
    void renderedOutputSurvivesAFileRoundTrip(@TempDir Path tempDir) throws IOException {
        String rendered = render(Map.of(
                "title", "<script>alert(1)</script>",
                "heading", "Safe",
                "id", "7",
                "tooltip", "\" onmouseover=\"alert(1)",
                "label", "Profile"));
        Path file = tempDir.resolve("rendered.html");

        Files.writeString(file, rendered, StandardCharsets.UTF_8);

        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(rendered);
        Document fromFile = Jsoup.parse(file, "UTF-8");
        assertThat(fromFile.title()).isEqualTo("<script>alert(1)</script>");
        assertThat(fromFile.select("script")).isEmpty();
        assertThat(fromFile.select("[onmouseover]")).isEmpty();
        assertThat(fromFile.location()).isNotBlank();
    }

    private static String render(Map<String, Object> model) throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(NaturalHtml.loader(new MapLoader(Map.of(TEMPLATE, SOURCE))))
                .build();
        StringWriter writer = new StringWriter();
        engine.getTemplate(TEMPLATE).evaluate(writer, model);
        return writer.toString();
    }
}
