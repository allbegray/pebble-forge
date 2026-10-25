package io.github.allbegray.pebbleforge.contracts;

import java.util.Comparator;
import java.util.List;

/**
 * The findings from one contract check.
 */
public final class ContractResult {

    private static final Comparator<ContractDiagnostic> ORDER =
            Comparator.comparing(ContractDiagnostic::template)
                    .thenComparingInt(ContractDiagnostic::line)
                    .thenComparing(diagnostic -> diagnostic.severity().ordinal())
                    .thenComparing(ContractDiagnostic::message);

    private final List<ContractDiagnostic> diagnostics;

    ContractResult(List<ContractDiagnostic> diagnostics) {
        this.diagnostics = diagnostics.stream().sorted(ORDER).toList();
    }

    /**
     * Returns every finding, ordered by template, line, severity, and message.
     *
     * @return the findings
     */
    public List<ContractDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns the error findings.
     *
     * @return the errors
     */
    public List<ContractDiagnostic> errors() {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == ContractDiagnostic.Severity.ERROR)
                .toList();
    }

    /**
     * Returns the warning findings.
     *
     * @return the warnings
     */
    public List<ContractDiagnostic> warnings() {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == ContractDiagnostic.Severity.WARNING)
                .toList();
    }

    /**
     * Returns whether the result contains an error.
     *
     * @return true when at least one error is present
     */
    public boolean hasErrors() {
        return diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ContractDiagnostic.Severity.ERROR);
    }

    /**
     * Returns whether the result has no findings.
     *
     * @return true when there are no errors or warnings
     */
    public boolean isClean() {
        return diagnostics.isEmpty();
    }

    /**
     * Formats the findings, one per line.
     *
     * @return the formatted findings
     */
    public String format() {
        StringBuilder output = new StringBuilder();
        for (ContractDiagnostic diagnostic : diagnostics) {
            if (!output.isEmpty()) {
                output.append(System.lineSeparator());
            }
            output.append(diagnostic);
        }
        return output.toString();
    }
}
