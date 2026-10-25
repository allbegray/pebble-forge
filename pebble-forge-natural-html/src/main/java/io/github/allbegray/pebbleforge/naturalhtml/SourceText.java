package io.github.allbegray.pebbleforge.naturalhtml;

final class SourceText {

    private final String templateName;
    private final String source;
    private final int[] lineStarts;

    SourceText(String templateName, String source) {
        this.templateName = templateName;
        this.source = source;
        this.lineStarts = SourceCoordinates.lineStarts(source);
    }

    String source() {
        return source;
    }

    SourcePosition position(int offset) {
        return SourceCoordinates.position(lineStarts, offset);
    }

    NaturalHtmlException error(int offset, String detail) {
        int clamped = Math.max(0, Math.min(offset, source.length()));
        SourcePosition position = SourceCoordinates.position(lineStarts, clamped);
        return new NaturalHtmlException(
                templateName, position.line(), position.column(), detail);
    }
}
