package io.github.allbegray.pebbleforge.naturalhtml;

/**
 * A template rejection with a position in the original source.
 */
public final class NaturalHtmlException extends RuntimeException {

    private final String templateName;
    private final int lineNumber;
    private final int columnNumber;

    NaturalHtmlException(String templateName, int lineNumber, int columnNumber, String detail) {
        super(templateName + ":" + lineNumber + ":" + columnNumber + ": " + detail);
        this.templateName = templateName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public String getTemplateName() {
        return templateName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }
}
