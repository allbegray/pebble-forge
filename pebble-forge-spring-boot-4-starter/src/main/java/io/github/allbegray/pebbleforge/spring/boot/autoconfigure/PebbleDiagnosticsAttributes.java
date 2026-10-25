package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

/**
 * Names used when a Pebble render failure is recorded on a request or exposed to Boot's
 * error attributes.
 */
public final class PebbleDiagnosticsAttributes {

    /**
     * Request attribute holding the {@code DiagnosticReport} for the current failure.
     */
    public static final String REPORT =
            "io.github.allbegray.pebbleforge.diagnostics.report";

    /**
     * Error attribute key for the structured {@code DiagnosticReport}.
     */
    public static final String MODEL_REPORT = "pebbleDiagnostics";

    /**
     * Error attribute key for the formatted diagnostic text.
     */
    public static final String MODEL_TEXT = "pebbleDiagnosticsText";

    private PebbleDiagnosticsAttributes() {
    }
}
