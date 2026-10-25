package io.github.allbegray.pebbleforge.naturalhtml;

import java.util.List;

/**
 * A rewritten Natural HTML template.
 *
 * @param templateName the template name
 * @param originalSource the source as read from the loader
 * @param generatedSource the source after expression rewriting
 * @param sourceMap the map from generated offsets to original offsets
 * @param sites the rewritten expressions
 */
public record NaturalHtmlAnalysis(
        String templateName,
        String originalSource,
        String generatedSource,
        SourceMap sourceMap,
        List<NaturalHtmlSite> sites) {

    /**
     * Creates an immutable analysis.
     *
     * @throws NullPointerException if {@code sites} is null
     */
    public NaturalHtmlAnalysis {
        sites = List.copyOf(sites);
    }
}
