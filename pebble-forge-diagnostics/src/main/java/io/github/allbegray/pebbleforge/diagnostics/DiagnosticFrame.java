package io.github.allbegray.pebbleforge.diagnostics;

import java.util.List;

/**
 * One template source frame in a diagnostic report.
 *
 * @param template the template name
 * @param line the line of the call or error
 * @param label a short description of the frame, or {@code null}
 * @param sourceLines the source lines to show
 * @param errorLineIndex the index of the error line in {@code sourceLines}, or -1
 * @param caretColumn the 1-based column for the caret
 * @param kind the frame kind
 * @param target the included or extended template, or {@code null} for an error frame
 */
public record DiagnosticFrame(String template, int line, String label, List<String> sourceLines,
                              int errorLineIndex, int caretColumn, Kind kind, String target) {

    /**
     * The kind of template frame.
     */
    public enum Kind {

        /**
         * An include frame.
         */
        INCLUDE,

        /**
         * An inheritance frame.
         */
        EXTENDS,

        /**
         * The failing frame.
         */
        ERROR
    }

    /**
     * Creates an immutable frame. A null {@code kind} becomes {@link Kind#ERROR}.
     *
     * @throws NullPointerException if {@code sourceLines} is null
     */
    public DiagnosticFrame {
        sourceLines = List.copyOf(sourceLines);
        kind = kind == null ? Kind.ERROR : kind;
    }

    /**
     * Creates an error frame with no target.
     *
     * @param template the template name
     * @param line the line of the call or error
     * @param label a short description of the frame, or {@code null}
     * @param sourceLines the source lines to show
     * @param errorLineIndex the index of the error line in {@code sourceLines}, or -1
     * @param caretColumn the 1-based column for the caret
     */
    public DiagnosticFrame(String template, int line, String label, List<String> sourceLines,
                           int errorLineIndex, int caretColumn) {
        this(template, line, label, sourceLines, errorLineIndex, caretColumn, Kind.ERROR, null);
    }

    /**
     * Formats the frame with line numbers and a caret under the error column.
     *
     * @return the formatted frame
     */
    public String format() {
        StringBuilder output = new StringBuilder();
        output.append(template == null ? "?" : template);
        if (line > 0) {
            output.append(':').append(line);
        }
        if (label != null && !label.isBlank()) {
            output.append(' ').append(label);
        }
        output.append(System.lineSeparator());

        if (sourceLines.isEmpty() || errorLineIndex < 0 || errorLineIndex >= sourceLines.size()) {
            output.append("  (source unavailable)").append(System.lineSeparator());
            return output.toString();
        }

        int firstLine = line - errorLineIndex;
        int width = String.valueOf(firstLine + sourceLines.size() - 1).length();
        int caret = Math.max(caretColumn, 1);

        for (int index = 0; index < sourceLines.size(); index++) {
            int lineNumber = firstLine + index;
            boolean errorLine = index == errorLineIndex;
            output.append(errorLine ? ">" : " ")
                    .append(' ')
                    .append(String.format("%" + width + "d", lineNumber))
                    .append(" | ")
                    .append(sourceLines.get(index))
                    .append(System.lineSeparator());

            if (errorLine) {
                output.append("  ")
                        .append(" ".repeat(width))
                        .append(" | ")
                        .append(" ".repeat(caret - 1))
                        .append('^')
                        .append(System.lineSeparator());
            }
        }
        return output.toString();
    }
}
