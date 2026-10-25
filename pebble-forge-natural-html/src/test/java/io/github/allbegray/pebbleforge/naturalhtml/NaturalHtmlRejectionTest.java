package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.pebbletemplates.pebble.PebbleEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaturalHtmlRejectionTest {

    private static final String TEMPLATE = "page.html";

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedTemplates")
    void rejectsTheTemplateWithLineAndColumn(String label, String source, String fragment) {
        assertThatThrownBy(() -> NaturalHtml.analyze(TEMPLATE, source))
                .as(label)
                .isInstanceOf(NaturalHtmlException.class)
                .hasMessageContaining(TEMPLATE + ":")
                .hasMessageContaining(fragment)
                .hasMessageMatching("(?s).*page\\.html:\\d+:\\d+:.*");
    }

    private static Stream<Arguments> rejectedTemplates() {
        return Stream.of(
                Arguments.of("script content", "<script>var x = {{ value }};</script>",
                        "<script> is not supported"),
                Arguments.of("unclosed script", "<script>{{ value }}",
                        "<script> is not supported"),
                Arguments.of("script attribute", "<script src=\"{{ url }}\"></script>",
                        "<script> is not supported"),
                Arguments.of("style content", "<style>.x { color: {{ value }}; }</style>",
                        "<style> is not supported"),
                Arguments.of("unclosed style", "<style>{{ value }}",
                        "<style> is not supported"),
                Arguments.of("html comment", "<!-- {{ value }} -->",
                        "HTML comments"),
                Arguments.of("bogus comment", "<?xml {{ value }}?>", "HTML comments"),
                Arguments.of("tag name", "<h{{ n }}>x</h{{ n }}>", "tag names"),
                Arguments.of("tag name suffix", "<h1{{ n }}>x</h1>", "tag names"),
                Arguments.of("attribute name", "<p {{ attr }}=\"v\">x</p>",
                        "attribute names"),
                Arguments.of("attribute name prefix", "<p data-{{ name }}=\"v\">x</p>",
                        "attribute names"),
                Arguments.of("duplicate attribute", "<p title=\"{{ a }}\" title=\"{{ b }}\">x</p>",
                        "inside an HTML tag"),
                Arguments.of("attribute on an end tag", "<p></p x=\"{{ y }}\">",
                        "Expression is not in an HTML text"),
                Arguments.of("tag name on an end tag", "<p></p{{ x }}>",
                        "Expression is not in an HTML text"),
                Arguments.of("unquoted attribute", "<p class={{ value }}>x</p>",
                        "Unquoted attribute values"),
                Arguments.of("unquoted attribute without spaces", "<p class={{value}}>x</p>",
                        "Unquoted attribute values"),
                Arguments.of("immediately after tag open", "<{{ value }}>",
                        "immediately after < or </"),
                Arguments.of("immediately after end tag open", "</{{ value }}>",
                        "immediately after < or </"),
                Arguments.of("event handler", "<button onclick=\"{{ handler }}\">x</button>",
                        "Attribute [onclick]"),
                Arguments.of("event handler on body", "<body onload=\"{{ handler }}\">x</body>",
                        "Attribute [onload]"),
                Arguments.of("style attribute", "<p style=\"{{ style }}\">x</p>",
                        "Attribute [style]"),
                Arguments.of("srcdoc attribute", "<div srcdoc=\"{{ doc }}\">x</div>",
                        "Attribute [srcdoc]"),
                Arguments.of("srcset attribute", "<img srcset=\"{{ set }}\">",
                        "Attribute [srcset]"),
                Arguments.of("imagesrcset attribute", "<img imagesrcset=\"{{ set }}\">",
                        "Attribute [imagesrcset]"),
                Arguments.of("ping attribute", "<a ping=\"{{ u }}\">x</a>",
                        "Attribute [ping]"),
                Arguments.of("archive attribute", "<div archive=\"{{ u }}\">x</div>",
                        "Attribute [archive]"),
                Arguments.of("meta", "<meta content=\"{{ x }}\">", "<meta> is not supported"),
                Arguments.of("base", "<base href=\"{{ x }}\">", "<base> is not supported"),
                Arguments.of("iframe", "<iframe src=\"{{ x }}\"></iframe>",
                        "<iframe> is not supported"),
                Arguments.of("unclosed iframe", "<iframe>{{ x }}",
                        "<iframe> is not supported"),
                Arguments.of("object", "<object data=\"{{ x }}\"></object>",
                        "<object> is not supported"),
                Arguments.of("embed", "<embed src=\"{{ x }}\">", "<embed> is not supported"),
                Arguments.of("svg", "<svg><text>{{ x }}</text></svg>",
                        "<svg> is not supported"),
                Arguments.of("unclosed svg", "<svg>{{ x }}", "<svg> is not supported"),
                Arguments.of("math", "<math><mi>{{ x }}</mi></math>",
                        "<math> is not supported"),
                Arguments.of("template", "<template><p>{{ x }}</p></template>",
                        "<template> is not supported"),
                Arguments.of("unclosed template", "<template>{{ x }}",
                        "<template> is not supported"),
                Arguments.of("noscript", "<body><noscript>{{ x }}</noscript></body>",
                        "<noscript> is not supported"),
                Arguments.of("xmp", "<xmp>{{ x }}</xmp>", "<xmp> is not supported"),
                Arguments.of("plaintext", "<plaintext>{{ x }}", "<plaintext> is not supported"),
                Arguments.of("noembed", "<noembed>{{ x }}</noembed>",
                        "<noembed> is not supported"),
                Arguments.of("noframes", "<noframes>{{ x }}</noframes>",
                        "<noframes> is not supported"),
                Arguments.of("frameset", "<frameset><frame src=\"{{ x }}\"></frameset>",
                        "<frameset> is not supported"),
                Arguments.of("applet", "<applet>{{ x }}</applet>",
                        "<applet> is not supported"),
                Arguments.of("doctype", "<!doctype {{ x }}>", "doctypes"),
                Arguments.of("cdata", "<![CDATA[{{ x }}]]>", "CDATA sections"),
                Arguments.of("pebble comment", "{# comment #}", "Pebble comments"),
                Arguments.of("pebble comment with expression", "{# {{ x }} #}",
                        "Pebble comments"),
                Arguments.of("if tag", "{% if x %}y{% endif %}", "Pebble tags"),
                Arguments.of("include tag", "{% include 'other' %}", "Pebble tags"),
                Arguments.of("raw filter", "<p>{{ value | raw }}</p>", "[raw]"),
                Arguments.of("explicit escape filter", "<p>{{ value | escape('html') }}</p>",
                        "[escape]"),
                Arguments.of("date filter", "<p>{{ value | date('yyyy') }}</p>", "[date]"),
                Arguments.of("safe filter", "<p>{{ value | safe }}</p>", "[safe]"),
                Arguments.of("slot function", "<p>{{ slot('name') }}</p>", "slot()"),
                Arguments.of("block function", "<p>{{ block('name') }}</p>", "block()"),
                Arguments.of("parent function", "<p>{{ parent() }}</p>", "parent()"),
                Arguments.of("empty expression", "<p>{{ }}</p>", "Empty expression"),
                Arguments.of("empty trimmed expression", "<p>{{- -}}</p>", "Empty expression"),
                Arguments.of("unclosed expression", "<p>{{ value</p>", "Unclosed expression"),
                Arguments.of("unterminated double quoted string", "<p>{{ \"value }}</p>",
                        "Unterminated string"),
                Arguments.of("unterminated single quoted string", "<p>{{ 'value }}</p>",
                        "Unterminated string"),
                Arguments.of("absolute url prefix",
                        "<a href=\"https://example.com/{{ path }}\">x</a>",
                        "URL attribute must start with a relative prefix"),
                Arguments.of("javascript url prefix",
                        "<a href=\"javascript:{{ code }}\">x</a>",
                        "URL attribute must start with a relative prefix"),
                Arguments.of("mailto url prefix",
                        "<a href=\"mailto:{{ address }}\">x</a>",
                        "URL attribute must start with a relative prefix"),
                Arguments.of("protocol relative url prefix",
                        "<a href=\"//example.com/{{ path }}\">x</a>",
                        "URL attribute must start with a relative prefix"),
                Arguments.of("url expression then literal",
                        "<a href=\"{{ base }}/edit\">x</a>",
                        "URL attribute must start with a relative prefix"),
                Arguments.of("quote crossing attribute",
                        "<p title=\"{{ \"x\" }}\">t</p>", "crosses an HTML boundary"),
                Arguments.of("quote crossing text nodes",
                        "<p>{{ x </p><p>}}</p>", "treats as markup"),
                Arguments.of("markup in text expression",
                        "<p>{{ flag ? \"<b>\" : \"x\" }}</p>", "treats as markup"),
                Arguments.of("markup in rcdata expression",
                        "<title>{{ \"</title>\" }}</title>", "treats as markup"),
                Arguments.of("invalid html", "<p>{{ a < b }}</p>", "Invalid HTML"),
                Arguments.of("nested expression", "<p>{{ map[{{ key }}] }}</p>",
                        "Nested {{"));
    }

    @Test
    void reportsTheLineAndColumnOfTheOffendingToken() {
        NaturalHtmlException error = catchError("<p>\n  {{ value | raw }}\n</p>");

        assertThat(error.getTemplateName()).isEqualTo(TEMPLATE);
        assertThat(error.getLineNumber()).isEqualTo(2);
        assertThat(error.getColumnNumber()).isEqualTo(14);
        assertThat(error).hasMessageStartingWith("page.html:2:14:");
    }

    @Test
    void reportsTheLineAndColumnOfAnEmptyExpression() {
        NaturalHtmlException error = catchError("<p>\n{{ }}</p>");

        assertThat(error.getLineNumber()).isEqualTo(2);
        assertThat(error.getColumnNumber()).isEqualTo(1);
    }

    @Test
    void reportsCrlfLineNumbers() {
        NaturalHtmlException error = catchError("<p>one\r\ntwo\r\n{{ value | raw }}</p>");

        assertThat(error.getLineNumber()).isEqualTo(3);
    }

    @Test
    void rejectsThroughTheLoaderAndEngine() {
        NaturalHtmlLoader loader = NaturalHtml.loader(
                new MapLoader(Map.of(TEMPLATE, "<script>{{ value }}</script>")));

        assertThatThrownBy(() -> loader.getReader(TEMPLATE))
                .isInstanceOf(NaturalHtmlException.class);

        PebbleEngine engine = PebbleForge.engine().loader(loader).build();

        assertThatThrownBy(() -> engine.getTemplate(TEMPLATE))
                .isInstanceOf(NaturalHtmlException.class);
    }

    private static NaturalHtmlException catchError(String source) {
        try {
            NaturalHtml.analyze(TEMPLATE, source);
            throw new AssertionError("Expected NaturalHtmlException");
        } catch (NaturalHtmlException error) {
            return error;
        }
    }
}
