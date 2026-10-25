package io.github.allbegray.pebbleforge.escape;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.escaper.EscapingStrategy;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Context-specific escaping strategies for Pebble templates.
 *
 * <p>Pebble's built-in {@code html}, {@code js}, and {@code css} strategies are left untouched.
 */
public final class ContextEscaping {

    /**
     * Name of Pebble's HTML escaping strategy.
     */
    public static final String HTML = "html";

    /**
     * Name of the HTML attribute escaping strategy registered by this class.
     */
    public static final String HTML_ATTRIBUTE = "html_attr";

    /**
     * Name of the URL escaping strategy registered by this class.
     */
    public static final String URL = "url";

    /**
     * Name of Pebble's JavaScript escaping strategy.
     */
    public static final String JAVASCRIPT = "js";

    /**
     * Name of Pebble's CSS escaping strategy.
     */
    public static final String CSS = "css";

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "mailto", "tel");

    private static final EscapingStrategy HTML_ATTRIBUTE_STRATEGY = ContextEscaping::escapeHtmlAttribute;
    private static final EscapingStrategy URL_STRATEGY = ContextEscaping::escapeUrl;

    private ContextEscaping() {
    }

    /**
     * Adds the {@code html_attr} and {@code url} strategies to a Pebble engine builder.
     *
     * <p>Safe to call more than once.
     *
     * @param builder the builder to configure
     */
    public static void register(PebbleEngine.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        builder.addEscapingStrategy(HTML_ATTRIBUTE, HTML_ATTRIBUTE_STRATEGY);
        builder.addEscapingStrategy(URL, URL_STRATEGY);
    }

    /**
     * Escapes a value for an HTML attribute value, quoted or unquoted.
     */
    /**
     * Returns the strategy that escapes a value for an HTML attribute value.
     *
     * @return the HTML attribute strategy
     */
    public static EscapingStrategy htmlAttribute() {
        return HTML_ATTRIBUTE_STRATEGY;
    }

    /**
     * Rejects unsafe URL schemes and escapes the result for an HTML attribute value.
     */
    /**
     * Returns the strategy that rejects unsafe URL schemes and escapes the result for an HTML
     * attribute value.
     *
     * @return the URL strategy
     */
    public static EscapingStrategy url() {
        return URL_STRATEGY;
    }

    private static String escapeHtmlAttribute(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                case ' ' -> out.append("&#32;");
                case '\t' -> out.append("&#9;");
                case '\n' -> out.append("&#10;");
                case '\f' -> out.append("&#12;");
                case '\r' -> out.append("&#13;");
                case '`' -> out.append("&#96;");
                case '=' -> out.append("&#61;");
                case '\0' -> out.append("&#65533;");
                default -> {
                    if (c < 0x20 || c == 0x7f) {
                        out.append("&#").append((int) c).append(';');
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private static String escapeUrl(String value) {
        if (value == null) {
            return null;
        }
        return hasSafeSchemeOrNoScheme(value) ? escapeHtmlAttribute(value) : "#";
    }

    private static boolean hasSafeSchemeOrNoScheme(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isAsciiWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isAsciiWhitespace(value.charAt(end - 1))) {
            end--;
        }

        StringBuilder probe = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char c = value.charAt(i);
            if (c != '\t' && c != '\n' && c != '\r') {
                probe.append(c);
            }
        }

        for (int i = 0; i < probe.length(); i++) {
            char c = probe.charAt(i);
            if (c == '/') {
                return true;
            }
            if (c == '?' || c == '#') {
                return true;
            }
            if (c == ':') {
                if (i == 0) {
                    return true;
                }
                String scheme = probe.substring(0, i).toLowerCase(Locale.ROOT);
                return ALLOWED_SCHEMES.contains(scheme);
            }
        }
        return true;
    }

    private static boolean isAsciiWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }
}
