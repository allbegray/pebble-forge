package io.github.allbegray.pebbleforge.diagnostics;

/**
 * An error that carries a position in the original template source.
 *
 * <p>Implementations report a template name plus a one-based line and column in the source before
 * any rewriting. {@link TemplateDiagnostics} uses those coordinates to build an error frame instead
 * of falling back to a message-only report.
 */
public interface SourceMappedError {

    /**
     * Returns the name of the template the error came from.
     *
     * @return the template name
     */
    String getTemplateName();

    /**
     * Returns the one-based line in the original template.
     *
     * @return the line number
     */
    int getLineNumber();

    /**
     * Returns the one-based column in the original template.
     *
     * @return the column number
     */
    int getColumnNumber();

    /**
     * Returns the error message shown in the report.
     *
     * @return the error message
     */
    String getMessage();
}
