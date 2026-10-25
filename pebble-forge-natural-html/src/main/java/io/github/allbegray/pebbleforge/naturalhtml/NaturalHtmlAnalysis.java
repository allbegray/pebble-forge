package io.github.allbegray.pebbleforge.naturalhtml;

import java.util.List;

/**
 * A rewritten Natural HTML template.
 */
public record NaturalHtmlAnalysis(
        String templateName,
        String originalSource,
        String generatedSource,
        SourceMap sourceMap,
        List<NaturalHtmlSite> sites) {

    public NaturalHtmlAnalysis {
        sites = List.copyOf(sites);
    }
}
