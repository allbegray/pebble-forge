package io.github.allbegray.pebbleforge.diagnostics;

import java.util.List;

/**
 * A rendered template error with source frames.
 *
 * @param message the error message
 * @param frames the source frames, from the outermost call to the error
 */
public record DiagnosticReport(String message, List<DiagnosticFrame> frames) {

    /**
     * Creates an immutable report.
     *
     * @throws NullPointerException if {@code frames} is null
     */
    public DiagnosticReport {
        frames = List.copyOf(frames);
    }

    /**
     * Formats the message and every frame.
     *
     * @return the formatted report
     */
    public String format() {
        StringBuilder output = new StringBuilder();
        if (message != null && !message.isBlank()) {
            output.append(message).append(System.lineSeparator());
        }
        for (DiagnosticFrame frame : frames) {
            output.append(frame.format());
        }
        return output.toString().stripTrailing();
    }
}
