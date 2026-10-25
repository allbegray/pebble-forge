package io.github.allbegray.pebbleforge.naturalhtml;

/**
 * The HTML context that determines an expression's escaping strategy.
 */
public enum HtmlContext {
    TEXT,
    RCDATA,
    ATTRIBUTE_VALUE,
    URL_ATTRIBUTE_VALUE
}
