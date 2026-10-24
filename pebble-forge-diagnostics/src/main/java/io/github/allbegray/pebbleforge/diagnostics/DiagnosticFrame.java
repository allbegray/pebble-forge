package io.github.allbegray.pebbleforge.diagnostics;

import java.util.List;

/**
 * One template source frame in a diagnostic report.
 */
public record DiagnosticFrame(String template, int line, String label, List<String> sourceLines,
                              int errorLineIndex, int caretColumn) {

    public DiagnosticFrame {
        sourceLines = List.copyOf(sourceLines);
    }

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
