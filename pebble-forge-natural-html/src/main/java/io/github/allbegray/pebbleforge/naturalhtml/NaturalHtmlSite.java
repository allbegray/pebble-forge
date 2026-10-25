package io.github.allbegray.pebbleforge.naturalhtml;

/**
 * One rewritten expression.
 *
 * <p>{@code startOffset} and {@code endOffset} span the {@code {{ ... }}} tag in the original
 * source, including any trim markers. {@code expression} is the trimmed expression text between
 * the delimiters.
 */
public record NaturalHtmlSite(
        int startOffset,
        int endOffset,
        SourcePosition start,
        HtmlContext context,
        String elementName,
        String attributeName,
        String expression,
        String escapeStrategy) {
}
