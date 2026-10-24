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

    public List<ContractDiagnostic> diagnostics() {
        return diagnostics;
    }

    public List<ContractDiagnostic> errors() {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == ContractDiagnostic.Severity.ERROR)
                .toList();
    }

    public List<ContractDiagnostic> warnings() {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == ContractDiagnostic.Severity.WARNING)
                .toList();
    }

    public boolean hasErrors() {
        return diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ContractDiagnostic.Severity.ERROR);
    }

    public boolean isClean() {
        return diagnostics.isEmpty();
    }

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
