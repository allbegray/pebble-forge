package io.github.allbegray.pebbleforge.naturalhtml;

/**
 * A one-based line and column plus a zero-based UTF-16 offset in the original source.
 */
public record SourcePosition(int offset, int line, int column) {
}
