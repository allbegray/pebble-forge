package io.github.allbegray.pebbleforge.contracts;

import java.util.Objects;

/**
 * One contract finding.
 *
 * @param severity the finding severity
 * @param template the template name
 * @param line the line in the template, or 0 when the finding has no line
 * @param message the finding message
 */
public record ContractDiagnostic(Severity severity, String template, int line, String message) {

    /**
     * Validates the finding.
     *
     * @throws NullPointerException if a non-null field is null
     */
    public ContractDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(message, "message");
    }

    /**
     * The severity of a finding.
     */
    public enum Severity {

        /**
         * A finding that fails the contract check.
         */
        ERROR,

        /**
         * A finding that does not fail the contract check.
         */
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
