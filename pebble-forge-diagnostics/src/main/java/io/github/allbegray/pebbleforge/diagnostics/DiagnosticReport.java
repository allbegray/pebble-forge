package io.github.allbegray.pebbleforge.diagnostics;

import java.util.List;

/**
 * A rendered template error with source frames.
 */
public record DiagnosticReport(String message, List<DiagnosticFrame> frames) {

    public DiagnosticReport {
        frames = List.copyOf(frames);
    }

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
