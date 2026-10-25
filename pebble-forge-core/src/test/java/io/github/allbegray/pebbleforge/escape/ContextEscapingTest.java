package io.github.allbegray.pebbleforge.escape;

import io.pebbletemplates.pebble.extension.escaper.EscapingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContextEscapingTest {

    private static final EscapingStrategy HTML_ATTRIBUTE = ContextEscaping.htmlAttribute();
    private static final EscapingStrategy URL = ContextEscaping.url();

    @Test
    void htmlAttributeReturnsNullForNull() {
        assertThat(HTML_ATTRIBUTE.escape(null)).isNull();
    }

    @Test
    void urlReturnsNullForNull() {
        assertThat(URL.escape(null)).isNull();
    }

    @Test
    void htmlAttributeEscapesAllMarkupCharacters() {
        assertThat(HTML_ATTRIBUTE.escape("&<>\"'"))
                .isEqualTo("&amp;&lt;&gt;&quot;&#39;");
    }

    @Test
    void htmlAttributeEscapesQuotePayloads() {
        assertThat(HTML_ATTRIBUTE.escape("\" onmouseover=\"alert(1)"))
                .isEqualTo("&quot;&#32;onmouseover&#61;&quot;alert(1)");
        assertThat(HTML_ATTRIBUTE.escape("' onfocus='alert(1)"))
                .isEqualTo("&#39;&#32;onfocus&#61;&#39;alert(1)");
    }

    @Test
    void htmlAttributeEscapesAttributeDelimitersAndWhitespace() {
        assertThat(HTML_ATTRIBUTE.escape(" \t\n\f\r`="))
                .isEqualTo("&#32;&#9;&#10;&#12;&#13;&#96;&#61;");
    }

    @Test
    void htmlAttributeEscapesEveryAsciiControlCharacter() {
        for (int code = 0; code < 0x20; code++) {
            String expected = switch (code) {
                case 0x00 -> "&#65533;";
                case 0x09 -> "&#9;";
                case 0x0a -> "&#10;";
                case 0x0c -> "&#12;";
                case 0x0d -> "&#13;";
                default -> "&#" + code + ";";
            };
            assertThat(HTML_ATTRIBUTE.escape(String.valueOf((char) code)))
                    .as("control 0x%02X", code)
                    .isEqualTo(expected);
        }
        assertThat(HTML_ATTRIBUTE.escape("\u007f")).isEqualTo("&#127;");
    }

    @Test
    void htmlAttributeEscapesExistingEntitiesOnce() {
        assertThat(HTML_ATTRIBUTE.escape("&amp;")).isEqualTo("&amp;amp;");
        assertThat(HTML_ATTRIBUTE.escape("&#32;")).isEqualTo("&amp;#32;");
        assertThat(HTML_ATTRIBUTE.escape("&quot;")).isEqualTo("&amp;quot;");
    }

    @Test
    void htmlAttributeLeavesNormalUnicodeAlone() {
        assertThat(HTML_ATTRIBUTE.escape("héllo→世界😀")).isEqualTo("héllo→世界😀");
    }

    @Test
    void urlAllowsRelativeUrls() {
        assertThat(URL.escape("/path/to?a=b")).isEqualTo("/path/to?a&#61;b");
        assertThat(URL.escape("//host/path")).isEqualTo("//host/path");
        assertThat(URL.escape("?q=1")).isEqualTo("?q&#61;1");
        assertThat(URL.escape("#frag")).isEqualTo("#frag");
        assertThat(URL.escape("")).isEmpty();
        assertThat(URL.escape(":foo")).isEqualTo(":foo");
    }

    @Test
    void urlAllowsSafeSchemes() {
        assertThat(URL.escape("http://example.com/")).isEqualTo("http://example.com/");
        assertThat(URL.escape("https://example.com/?q=1"))
                .isEqualTo("https://example.com/?q&#61;1");
        assertThat(URL.escape("HTTP://EXAMPLE.COM/")).isEqualTo("HTTP://EXAMPLE.COM/");
        assertThat(URL.escape("HtTpS://example.com/")).isEqualTo("HtTpS://example.com/");
        assertThat(URL.escape("mailto:user@example.com"))
                .isEqualTo("mailto:user@example.com");
        assertThat(URL.escape("mailto:user@example.com?subject=<x>"))
                .isEqualTo("mailto:user@example.com?subject&#61;&lt;x&gt;");
        assertThat(URL.escape("tel:+1-555-0100")).isEqualTo("tel:+1-555-0100");
        assertThat(URL.escape("tel:+1 555")).isEqualTo("tel:+1&#32;555");
    }

    @ParameterizedTest
    @MethodSource("unsafeUrls")
    void urlRejectsUnsafeSchemes(String value) {
        assertThat(URL.escape(value)).isEqualTo("#");
    }

    @Test
    void urlEscapesTheWholeValueForAnAttribute() {
        assertThat(URL.escape("https://e/?q=<script>&x=\"y\""))
                .isEqualTo("https://e/?q&#61;&lt;script&gt;&amp;x&#61;&quot;y&quot;");
        assertThat(URL.escape("https://e/?q='x'&a=b"))
                .isEqualTo("https://e/?q&#61;&#39;x&#39;&amp;a&#61;b");
    }

    @Test
    void urlTrimsOnlyForSchemeDetection() {
        assertThat(URL.escape(" https://example.com/"))
                .isEqualTo("&#32;https://example.com/");
        assertThat(URL.escape("https://example.com/ "))
                .isEqualTo("https://example.com/&#32;");
        assertThat(URL.escape("ht\ttps://example.com/"))
                .isEqualTo("ht&#9;tps://example.com/");
        assertThat(URL.escape(" \tjavascript:alert(1)\n")).isEqualTo("#");
    }

    @Test
    void urlTreatsADelimiterBeforeAColonAsRelative() {
        assertThat(URL.escape("/a:b")).isEqualTo("/a:b");
        assertThat(URL.escape("?a:b")).isEqualTo("?a:b");
        assertThat(URL.escape("#a:b")).isEqualTo("#a:b");
        assertThat(URL.escape("//host:8080/x")).isEqualTo("//host:8080/x");
    }

    @Test
    void urlEscapesRelativeValuesForAnAttribute() {
        assertThat(URL.escape("/a b?c=d")).isEqualTo("/a&#32;b?c&#61;d");
    }

    private static Stream<String> unsafeUrls() {
        return Stream.of(
                "javascript:alert(1)",
                "JaVaScRiPt:alert(1)",
                "java\tscript:alert(1)",
                "java\nscript:alert(1)",
                "java\rscript:alert(1)",
                "java\fscript:alert(1)",
                "javascript\u0000:alert(1)",
                "data:text/html,<script>alert(1)</script>",
                "vbscript:msgbox(1)",
                "file:///etc/passwd",
                "blob:https://example.com/id",
                "about:blank",
                "ftp://example.com/",
                "  javascript:alert(1)  ",
                "\tjavascript:alert(1)",
                '\u0000' + "javascript:alert(1)",
                ((char) 1) + "javascript:alert(1)",
                ((char) 0x0B) + "javascript:alert(1)",
                "java script:alert(1)",
                "javascript :alert(1)"
        );
    }
}
