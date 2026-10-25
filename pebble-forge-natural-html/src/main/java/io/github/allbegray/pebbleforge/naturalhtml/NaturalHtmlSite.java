package io.github.allbegray.pebbleforge.naturalhtml;

/**
 * One rewritten expression.
 *
 * <p>{@code startOffset} and {@code endOffset} span the {@code {{ ... }}} tag in the original
 * source, including any trim markers. {@code expression} is the trimmed expression text between
 * the delimiters.
 *
 * @param startOffset the start offset of the tag in the original source
 * @param endOffset the end offset of the tag in the original source
 * @param start the position of the tag start
 * @param context the HTML context of the expression
 * @param elementName the enclosing element name, or {@code null}
 * @param attributeName the attribute name, or {@code null}
 * @param expression the trimmed expression text
 * @param escapeStrategy the Pebble escaping strategy used for the expression
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
