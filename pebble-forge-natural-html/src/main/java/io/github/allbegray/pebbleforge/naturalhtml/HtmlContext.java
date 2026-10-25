package io.github.allbegray.pebbleforge.naturalhtml;

/**
 * The HTML context that determines an expression's escaping strategy.
 */
public enum HtmlContext {

    /**
     * Text content.
     */
    TEXT,

    /**
     * Text content in a raw-text element such as {@code script} or {@code style}.
     */
    RCDATA,

    /**
     * A quoted or unquoted attribute value.
     */
    ATTRIBUTE_VALUE,

    /**
     * An attribute value that holds a URL.
     */
    URL_ATTRIBUTE_VALUE
}
