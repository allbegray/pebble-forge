package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.diagnostics.SourceMappedError;

/**
 * A template rejection with a position in the original source.
 */
public final class NaturalHtmlException extends RuntimeException implements SourceMappedError {

    /**
     * The template name.
     */
    private final String templateName;

    /**
     * The one-based line number.
     */
    private final int lineNumber;

    /**
     * The one-based column number.
     */
    private final int columnNumber;

    NaturalHtmlException(String templateName, int lineNumber, int columnNumber, String detail) {
        super(templateName + ":" + lineNumber + ":" + columnNumber + ": " + detail);
        this.templateName = templateName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Returns the template name.
     *
     * @return the template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Returns the one-based line number.
     *
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the one-based column number.
     *
     * @return the column number
     */
    public int getColumnNumber() {
        return columnNumber;
    }
}
