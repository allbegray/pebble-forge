package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.loader.Loader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaturalHtmlLoaderTest {

    private static final String TEMPLATE = "page.html";

    @Test
    void rewritesTextWithHtmlEscape() {
        assertThat(generated("<p>Hello {{ name }}</p>"))
                .isEqualTo("<p>Hello {{ (name) | escape('html') }}</p>");
    }

    @Test
    void rewritesTitleAndTextareaAsRcdata() {
        assertThat(generated("<title>{{ title }}</title>"))
                .isEqualTo("<title>{{ (title) | escape('html') }}</title>");
        assertThat(generated("<textarea>{{ body }}</textarea>"))
                .isEqualTo("<textarea>{{ (body) | escape('html') }}</textarea>");
    }

    @Test
    void usesOppositeQuotesForTheFilterArgument() {
        assertThat(generated("<p title=\"{{ title }}\">x</p>"))
                .isEqualTo("<p title=\"{{ (title) | escape('html_attr') }}\">x</p>");
        assertThat(generated("<p title='{{ title }}'>x</p>"))
                .isEqualTo("<p title='{{ (title) | escape(\"html_attr\") }}'>x</p>");
    }

    @Test
    void usesUrlEscapeForUrlAttributes() {
        assertThat(generated("<a href=\"{{ link }}\">x</a>"))
                .isEqualTo("<a href=\"{{ (link) | escape('url') }}\">x</a>");
        assertThat(generated("<img src=\"{{ src }}\">"))
                .isEqualTo("<img src=\"{{ (src) | escape('url') }}\">");
        assertThat(generated("<form action=\"/submit/{{ id }}\">x</form>"))
                .isEqualTo("<form action=\"/submit/{{ (id) | escape('url') }}\">x</form>");
        assertThat(generated("<a href='/u/{{ id }}'>x</a>"))
                .isEqualTo("<a href='/u/{{ (id) | escape(\"url\") }}'>x</a>");
    }

    @Test
    void keepsRelativeUrlPrefixes() {
        for (String prefix : List.of("/users/", "./", "../", "#", "?")) {
            String template = "<a href=\"" + prefix + "{{ value }}\">x</a>";
            assertThat(generated(template))
                    .as(prefix)
                    .isEqualTo("<a href=\"" + prefix + "{{ (value) | escape('url') }}\">x</a>");
        }
    }

    @Test
    void allowsLiteralTextAfterARelativeUrlExpression() {
        assertThat(generated("<a href=\"/users/{{ id }}/edit\">x</a>"))
                .isEqualTo("<a href=\"/users/{{ (id) | escape('url') }}/edit\">x</a>");
    }

    @Test
    void allowsConcatenatedWholeValueUrlExpressions() {
        assertThat(generated("<a href=\"{{ scheme }}{{ host }}\">x</a>"))
                .isEqualTo("<a href=\"{{ (scheme) | escape('url') }}"
                        + "{{ (host) | escape('url') }}\">x</a>");
    }

    @Test
    void preservesTrimMarkers() {
        assertThat(generated("<p>{{- name -}}</p>"))
                .isEqualTo("<p>{{- (name) | escape('html') -}}</p>");
        assertThat(generated("<p>{{- name }}</p>"))
                .isEqualTo("<p>{{- (name) | escape('html') }}</p>");
    }

    @Test
    void wrapsTheWholeExpressionBeforeAppendingTheFilter() {
        assertThat(generated("<p>{{ name | upper }}</p>"))
                .isEqualTo("<p>{{ (name | upper) | escape('html') }}</p>");
        assertThat(generated("<p>{{ flag ? \"a\" : \"b\" }}</p>"))
                .isEqualTo("<p>{{ (flag ? \"a\" : \"b\") | escape('html') }}</p>");
        assertThat(generated("<p>{{ user.name | default('n/a') }}</p>"))
                .isEqualTo("<p>{{ (user.name | default('n/a')) | escape('html') }}</p>");
    }

    @Test
    void handlesMapLiteralsAndQuotedDelimiters() {
        assertThat(generated("<p>{{ {'a': {'b': 1}} }}</p>"))
                .isEqualTo("<p>{{ ({'a': {'b': 1}}) | escape('html') }}</p>");
        assertThat(generated("<p>{{ map['}}'] }}</p>"))
                .isEqualTo("<p>{{ (map['}}']) | escape('html') }}</p>");
        assertThat(generated("<p>{{ \"}}\" }}</p>"))
                .isEqualTo("<p>{{ (\"}}\") | escape('html') }}</p>");
    }

    @Test
    void rewritesEveryExpressionInOneAttribute() {
        assertThat(generated("<p title=\"{{ a }} and {{ b }}\">x</p>"))
                .isEqualTo("<p title=\"{{ (a) | escape('html_attr') }} and "
                        + "{{ (b) | escape('html_attr') }}\">x</p>");
    }

    @Test
    void returnsStaticHtmlUnchanged() {
        String source = "<!doctype html>\n<html><body><p>static</p></body></html>\n";
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, source);

        assertThat(analysis.generatedSource()).isEqualTo(source);
        assertThat(analysis.sites()).isEmpty();
        assertThat(analysis.sourceMap().originalOffset(0)).isZero();
        assertThat(analysis.sourceMap().originalOffset(source.length())).isEqualTo(source.length());
    }

    @Test
    void exposesSitesInSourceOrder() {
        String source = "<p title=\"{{ title }}\">Hello {{ name }}</p>";
        NaturalHtmlAnalysis analysis = NaturalHtml.analyze(TEMPLATE, source);

        assertThat(analysis.templateName()).isEqualTo(TEMPLATE);
        assertThat(analysis.originalSource()).isEqualTo(source);
        assertThat(analysis.sites()).hasSize(2);

        NaturalHtmlSite first = analysis.sites().get(0);
        assertThat(first.startOffset()).isEqualTo(source.indexOf("{{"));
        assertThat(first.endOffset()).isEqualTo(source.indexOf("}}") + 2);
        assertThat(first.start()).isEqualTo(new SourcePosition(source.indexOf("{{"), 1,
                source.indexOf("{{") + 1));
        assertThat(first.context()).isEqualTo(HtmlContext.ATTRIBUTE_VALUE);
        assertThat(first.elementName()).isEqualTo("p");
        assertThat(first.attributeName()).isEqualTo("title");
        assertThat(first.expression()).isEqualTo("title");
        assertThat(first.escapeStrategy()).isEqualTo("html_attr");

        NaturalHtmlSite second = analysis.sites().get(1);
        assertThat(second.context()).isEqualTo(HtmlContext.TEXT);
        assertThat(second.elementName()).isEqualTo("p");
        assertThat(second.attributeName()).isNull();
        assertThat(second.expression()).isEqualTo("name");
        assertThat(second.escapeStrategy()).isEqualTo("html");
    }

    @Test
    void loaderGetReaderReturnsRewrittenSource() throws IOException {
        NaturalHtmlLoader loader = NaturalHtml.loader(
                new MapLoader(Map.of(TEMPLATE, "<p>{{ name }}</p>")));

        assertThat(read(loader.getReader(TEMPLATE)))
                .isEqualTo("<p>{{ (name) | escape('html') }}</p>");
    }

    @Test
    void readOriginalReturnsTheDelegateSource() {
        NaturalHtmlLoader loader = NaturalHtml.loader(
                new MapLoader(Map.of(TEMPLATE, "<p>{{ name }}</p>")));

        assertThat(loader.readOriginal(TEMPLATE)).isEqualTo("<p>{{ name }}</p>");
    }

    @Test
    void readOriginalRereadsTheDelegate() {
        MutableLoader delegate = new MutableLoader("<p>{{ first }}</p>");
        NaturalHtmlLoader loader = NaturalHtml.loader(delegate);

        assertThat(loader.readOriginal(TEMPLATE)).isEqualTo("<p>{{ first }}</p>");
        delegate.source = "<p>{{ second }}</p>";
        assertThat(loader.readOriginal(TEMPLATE)).isEqualTo("<p>{{ second }}</p>");
    }

    @Test
    void delegatesLoaderConfigurationAndLookup() {
        RecordingLoader delegate = new RecordingLoader(Map.of(TEMPLATE, "<p>x</p>"));
        NaturalHtmlLoader loader = NaturalHtml.loader(delegate);

        loader.setCharset("UTF-8");
        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        assertThat(delegate.charset).isEqualTo("UTF-8");
        assertThat(delegate.prefix).isEqualTo("templates/");
        assertThat(delegate.suffix).isEqualTo(".html");
        assertThat(loader.createCacheKey(TEMPLATE)).isEqualTo(TEMPLATE);
        assertThat(loader.resolveRelativePath("./other.html", TEMPLATE))
                .isEqualTo(delegate.resolveRelativePath("./other.html", TEMPLATE));
        assertThat(loader.resourceExists(TEMPLATE)).isTrue();
        assertThat(loader.resourceExists("missing.html")).isFalse();
        assertThat(loader.delegate()).isSameAs(delegate);
    }

    @Test
    void missingTemplatePropagatesTheDelegateError() {
        NaturalHtmlLoader loader = NaturalHtml.loader(new MapLoader(Map.of()));

        assertThatThrownBy(() -> loader.getReader("missing.html"))
                .isInstanceOf(LoaderException.class)
                .hasMessageContaining("missing.html");
        assertThatThrownBy(() -> loader.readOriginal("missing.html"))
                .isInstanceOf(LoaderException.class)
                .hasMessageContaining("missing.html");
    }

    @Test
    void rendersThroughTheForgeEngine() throws IOException {
        String output = render("<p>{{ name }}</p>", Map.of("name", "<b>&\"'"));

        assertThat(output).isEqualTo("<p>&lt;b&gt;&amp;&quot;&#39;</p>");
    }

    @Test
    void rendersSingleQuotedAttributeThroughTheForgeEngine() throws IOException {
        String output = render("<p title='{{ title }}'>x</p>",
                Map.of("title", "\" onmouseover=\"alert(1)"));

        assertThat(output).isEqualTo(
                "<p title='&quot;&#32;onmouseover&#61;&quot;alert(1)'>x</p>");
    }

    @Test
    void rendersTrimmedNegativeNumbersAndDashDelimitedExpressions() throws IOException {
        assertThat(render("<p>{{-1 }}</p>", Map.of())).isEqualTo("<p>-1</p>");
        assertThat(render("<p>{{ 1 -}}</p>", Map.of())).isEqualTo("<p>1</p>");
        assertThat(render("<p>{{ value-}}</p>", Map.of("value", "v"))).isEqualTo("<p>v</p>");
    }

    private static String generated(String source) {
        return NaturalHtml.analyze(TEMPLATE, source).generatedSource();
    }

    private static String render(String source, Map<String, Object> model) throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(NaturalHtml.loader(new MapLoader(Map.of(TEMPLATE, source))))
                .build();
        return evaluate(engine, model);
    }

    private static String evaluate(PebbleEngine engine, Map<String, Object> model)
            throws IOException {
        StringWriter writer = new StringWriter();
        engine.getTemplate(TEMPLATE).evaluate(writer, model);
        return writer.toString();
    }

    private static String read(Reader reader) throws IOException {
        StringBuilder value = new StringBuilder();
        char[] buffer = new char[512];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            value.append(buffer, 0, read);
        }
        return value.toString();
    }

    private static final class MutableLoader implements Loader<String> {

        private String source;

        private MutableLoader(String source) {
            this.source = source;
        }

        @Override
        public Reader getReader(String cacheKey) {
            return new StringReader(source);
        }

        @Override
        public void setCharset(String charset) {
        }

        @Override
        public void setPrefix(String prefix) {
        }

        @Override
        public void setSuffix(String suffix) {
        }

        @Override
        public String resolveRelativePath(String relativePath, String anchorPath) {
            return relativePath;
        }

        @Override
        public String createCacheKey(String templateName) {
            return templateName;
        }

        @Override
        public boolean resourceExists(String templateName) {
            return true;
        }
    }

    private static final class RecordingLoader implements Loader<String> {

        private final MapLoader delegate;
        private String charset;
        private String prefix;
        private String suffix;

        private RecordingLoader(Map<String, String> templates) {
            this.delegate = new MapLoader(new LinkedHashMap<>(templates));
        }

        @Override
        public Reader getReader(String cacheKey) {
            return delegate.getReader(cacheKey);
        }

        @Override
        public void setCharset(String charset) {
            this.charset = charset;
        }

        @Override
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public String resolveRelativePath(String relativePath, String anchorPath) {
            return delegate.resolveRelativePath(relativePath, anchorPath);
        }

        @Override
        public String createCacheKey(String templateName) {
            return delegate.createCacheKey(templateName);
        }

        @Override
        public boolean resourceExists(String templateName) {
            return delegate.resourceExists(templateName);
        }
    }
}
