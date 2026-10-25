package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaturalHtmlSourceMapTest {

    private static final String TEMPLATE = "page.html";

    @Test
    void mapsGeneratedOffsetsBackToOriginalOffsets() {
        String source = "<p>{{ name }}</p>";
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, source);
        SourceMap sourceMap = analysis.sourceMap();

        assertThat(sourceMap.originalOffset(0)).isZero();
        assertThat(sourceMap.originalOffset(5)).isEqualTo(5);
        assertThat(sourceMap.originalOffset(analysis.generatedSource().indexOf("name")))
                .isEqualTo(source.indexOf("name"));
        assertThat(sourceMap.originalOffset(analysis.generatedSource().length()))
                .isEqualTo(source.length());
    }

    @Test
    void mapsInsertedCharactersToTheirOriginalAnchor() {
        String source = "<p>{{ name }}</p>";
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, source);
        SourceMap sourceMap = analysis.sourceMap();
        int openingParenthesis = analysis.generatedSource().indexOf('(');
        int closingParenthesis = analysis.generatedSource().indexOf(") | escape");

        assertThat(openingParenthesis).isEqualTo(source.indexOf("name"));
        assertThat(sourceMap.originalOffset(openingParenthesis))
                .isEqualTo(source.indexOf("name"));
        assertThat(sourceMap.originalPosition(openingParenthesis))
                .isEqualTo(new SourcePosition(source.indexOf("name"), 1,
                        source.indexOf("name") + 1));
        assertThat(sourceMap.originalPosition(closingParenthesis))
                .isEqualTo(new SourcePosition(source.indexOf(" }}"), 1,
                        source.indexOf(" }}") + 1));
    }

    @Test
    void mapsTwoSitesOnOneLine() {
        String source = "<p>{{ a }} and {{ b }}</p>";
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, source);
        SourceMap sourceMap = analysis.sourceMap();

        assertThat(analysis.sites()).hasSize(2);
        NaturalHtmlSite second = analysis.sites().get(1);
        int secondStart = source.indexOf("{{", source.indexOf("{{") + 2);

        assertThat(second.startOffset()).isEqualTo(secondStart);
        assertThat(second.start()).isEqualTo(new SourcePosition(secondStart, 1, secondStart + 1));
        assertThat(sourceMap.originalOffset(analysis.generatedSource().indexOf("b)")))
                .isEqualTo(source.indexOf("b"));
        assertThat(sourceMap.originalOffset(analysis.generatedSource().indexOf("(b)")))
                .isEqualTo(source.indexOf("b"));
    }

    @Test
    void mapsAnExpressionAfterAnEmoji() {
        String source = "<p>\uD83D\uDE00 {{ name }}</p>";
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, source);
        int start = source.indexOf("{{");

        assertThat(analysis.sites()).hasSize(1);
        assertThat(analysis.sites().get(0).startOffset()).isEqualTo(start);
        assertThat(analysis.sites().get(0).start().line()).isEqualTo(1);
        assertThat(analysis.sites().get(0).start().column()).isEqualTo(start + 1);
        assertThat(analysis.generatedSource()).contains("\uD83D\uDE00");
        assertThat(lineCount(analysis.generatedSource()))
                .isEqualTo(lineCount(source));
        assertThat(analysis.sourceMap().originalOffset(
                analysis.generatedSource().indexOf("name")))
                .isEqualTo(source.indexOf("name"));
    }

    @Test
    void mapsCrlfSourceAcrossLines() {
        String source = "<p>one\r\ntwo {{ name }}</p>";
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, source);
        NaturalHtmlSite site = analysis.sites().get(0);
        int lineTwoStart = source.indexOf("\r\n") + 2;

        assertThat(site.start().line()).isEqualTo(2);
        assertThat(site.start().column()).isEqualTo(site.startOffset() - lineTwoStart + 1);
        assertThat(lineCount(analysis.generatedSource())).isEqualTo(lineCount(source));
        assertThat(numberOfNewlines(analysis.generatedSource()))
                .isEqualTo(numberOfNewlines(source));
    }

    @Test
    void generatedLineCountEqualsOriginalForEveryRewrite() {
        String source = """
                <!doctype html>
                <html>
                  <body>
                    <h1>{{ title }}</h1>
                    <p title="{{ tooltip }}">{{ body }}</p>
                    <a href="/u/{{ id }}">profile</a>
                  </body>
                </html>
                """;
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, source);

        assertThat(analysis.sites()).hasSize(4);
        assertThat(analysis.generatedSource().lines().count())
                .isEqualTo(source.lines().count());
        assertThat(numberOfNewlines(analysis.generatedSource()))
                .isEqualTo(numberOfNewlines(source));
    }

    @Test
    void missingVariableKeepsItsLineThroughTheEngine() {
        String source = """
                <p>one</p>
                <p>two</p>
                <p>{{ missing }}</p>
                """;
        PebbleEngine engine = PebbleForge.engine()
                .loader(NaturalHtml.loader(new MapLoader(Map.of(TEMPLATE, source))))
                .build();

        Throwable error = catchThrowable(() ->
                engine.getTemplate(TEMPLATE).evaluate(new StringWriter(), Map.of()));

        assertThat(error).isInstanceOf(PebbleException.class);
        assertThat(((PebbleException) error).getLineNumber()).isEqualTo(3);
    }

    @Test
    void identityForStaticSource() {
        String source = "<p>static</p>\n<p>still static</p>";
        SourceMap sourceMap = NaturalHtml.analyze(TEMPLATE, source).sourceMap();

        for (int offset = 0; offset <= source.length(); offset++) {
            assertThat(sourceMap.originalOffset(offset)).isEqualTo(offset);
        }
    }

    @Test
    void rejectsOffsetsOutsideTheGeneratedSource() {
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, "<p>{{ name }}</p>");
        SourceMap sourceMap = analysis.sourceMap();
        int generatedLength = analysis.generatedSource().length();

        assertThatThrownBy(() -> sourceMap.originalOffset(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sourceMap.originalOffset(generatedLength + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Throwable catchThrowable(ThrowingRunnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Throwable error) {
            return error;
        }
    }

    private static int lineCount(String source) {
        return numberOfNewlines(source) + 1;
    }

    private static int numberOfNewlines(String source) {
        int count = 0;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '\r') {
                if (index + 1 < source.length() && source.charAt(index + 1) == '\n') {
                    index++;
                }
                count++;
            } else if (character == '\n') {
                count++;
            }
        }
        return count;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
