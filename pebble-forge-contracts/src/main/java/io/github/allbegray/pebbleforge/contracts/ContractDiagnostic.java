package io.github.allbegray.pebbleforge.contracts;

import java.util.Objects;

/**
 * One contract finding.
 */
public record ContractDiagnostic(Severity severity, String template, int line, String message) {

    public ContractDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(message, "message");
    }

    public enum Severity {
        ERROR,
        WARNING
    }

    @Override
    public String toString() {
        if (line > 0) {
            return template + ":" + line + ": " + severity + ": " + message;
        }
        return template + ": " + severity + ": " + message;
    }
}
