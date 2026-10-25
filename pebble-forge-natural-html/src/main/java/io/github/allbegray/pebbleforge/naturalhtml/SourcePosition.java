package io.github.allbegray.pebbleforge.naturalhtml;

/**
 * A one-based line and column plus a zero-based UTF-16 offset in the original source.
 *
 * @param offset the zero-based UTF-16 offset
 * @param line the one-based line
 * @param column the one-based column
 */
public record SourcePosition(int offset, int line, int column) {
}
