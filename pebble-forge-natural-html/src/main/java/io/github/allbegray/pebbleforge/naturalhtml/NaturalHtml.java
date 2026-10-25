package io.github.allbegray.pebbleforge.naturalhtml;

import io.pebbletemplates.pebble.loader.Loader;

import java.util.Objects;

/**
 * Entry point for Natural HTML templates.
 *
 * <p>A Natural HTML template is an ordinary {@code .html} file whose dynamic values are
 * written as {@code {{ ... }}}. The loader rewrites each expression with a context-specific
 * escaping strategy, so the file stays valid when opened directly in a browser.
 */
public final class NaturalHtml {

    private NaturalHtml() {
    }

    /**
     * Wraps a delegate loader with Natural HTML analysis and rewriting.
     *
     * @param delegate the loader that reads the original templates
     * @return the Natural HTML loader
     */
    public static NaturalHtmlLoader loader(Loader<String> delegate) {
        return new NaturalHtmlLoader(Objects.requireNonNull(delegate, "delegate"));
    }

    /**
     * Analyzes and rewrites a template without building an engine.
     *
     * @param templateName the template name used in errors
     * @param source the original template source
     * @return the analysis
     * @throws NaturalHtmlException when the source is not a safe Natural HTML template
     */
    public static NaturalHtmlAnalysis analyze(String templateName, String source) {
        return NaturalHtmlAnalyzer.analyze(
                Objects.requireNonNull(templateName, "templateName"),
                Objects.requireNonNull(source, "source"));
    }
}
