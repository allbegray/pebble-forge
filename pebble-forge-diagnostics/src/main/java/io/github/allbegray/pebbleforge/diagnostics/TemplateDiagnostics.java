package io.github.allbegray.pebbleforge.diagnostics;

import io.github.allbegray.pebbleforge.trace.TemplateTrace;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.loader.Loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a source-frame report for a Pebble error.
 */
public final class TemplateDiagnostics {

    private final Loader<?> loader;
    private final int contextLines;

    private TemplateDiagnostics(Builder builder) {
        this.loader = builder.loader;
        this.contextLines = builder.contextLines;
    }

    /**
     * Creates a diagnostics builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a report for a render failure. The report includes the include and extends frames
     * attached to the error plus the failing source line.
     *
     * @param error the render failure
     * @return the report
     */
    public DiagnosticReport render(Throwable error) {
        PebbleException pebble = findPebble(error);
        if (pebble == null) {
            return new DiagnosticReport(error == null ? "Unknown error" : error.toString(),
                    List.of());
        }

        List<DiagnosticFrame> frames = new ArrayList<>();
        for (TemplateTrace.Frame frame : TemplateTrace.frames(error)) {
            boolean extendsFrame = frame.kind() == TemplateTrace.Kind.EXTENDS;
            String label = (extendsFrame ? "extends " : "includes ") + frame.target();
            DiagnosticFrame.Kind kind = extendsFrame
                    ? DiagnosticFrame.Kind.EXTENDS
                    : DiagnosticFrame.Kind.INCLUDE;
            frames.add(frameFor(frame.template(), frame.line(), label, kind, frame.target()));
        }
        int errorLine = pebble.getLineNumber() == null ? 0 : pebble.getLineNumber();
        if (frames.isEmpty() || !sameLocation(frames.getLast(), pebble.getFileName(), errorLine)) {
            frames.add(frameFor(pebble.getFileName(), errorLine, null,
                    DiagnosticFrame.Kind.ERROR, null));
        }

        String message = pebble.getPebbleMessage() != null
                ? pebble.getPebbleMessage()
                : pebble.getMessage();
        return new DiagnosticReport(message, frames);
    }

    private DiagnosticFrame frameFor(String template, int line, String label,
            DiagnosticFrame.Kind kind, String target) {
        if (template == null || line <= 0) {
            return new DiagnosticFrame(template, line, label, List.of(), -1, 1, kind, target);
        }

        List<String> lines = readLines(template);
        if (line > lines.size()) {
            return new DiagnosticFrame(template, line, label, List.of(), -1, 1, kind, target);
        }

        int start = Math.max(1, line - contextLines);
        int end = Math.min(lines.size(), line + contextLines);
        List<String> source = lines.subList(start - 1, end);
        int errorIndex = line - start;
        int caret = firstNonWhitespace(source.get(errorIndex)) + 1;
        return new DiagnosticFrame(template, line, label, source, errorIndex, caret, kind, target);
    }

    private static boolean sameLocation(DiagnosticFrame frame, String template, int line) {
        return Objects.equals(frame.template(), template) && frame.line() == line;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<String> readLines(String template) {
        Loader rawLoader = loader;
        try (Reader reader = rawLoader.getReader(template);
                BufferedReader buffered = new BufferedReader(reader)) {
            return buffered.lines().toList();
        } catch (RuntimeException | IOException e) {
            return List.of();
        }
    }

    private static int firstNonWhitespace(String line) {
        for (int index = 0; index < line.length(); index++) {
            if (!Character.isWhitespace(line.charAt(index))) {
                return index;
            }
        }
        return 0;
    }

    private static PebbleException findPebble(Throwable error) {
        return findPebble(error, new java.util.HashSet<>());
    }

    private static PebbleException findPebble(Throwable error, java.util.Set<Throwable> seen) {
        if (error == null || !seen.add(error)) {
            return null;
        }
        if (error instanceof PebbleException pebble) {
            return pebble;
        }
        for (Throwable suppressed : error.getSuppressed()) {
            PebbleException nested = findPebble(suppressed, seen);
            if (nested != null) {
                return nested;
            }
        }
        if (error.getCause() != null && error.getCause() != error) {
            return findPebble(error.getCause(), seen);
        }
        return null;
    }

    /**
     * Builds a {@link TemplateDiagnostics}.
     */
    public static final class Builder {

        /**
         * Creates an empty builder. Prefer {@link TemplateDiagnostics#builder()}.
         */
        public Builder() {
        }

        private Loader<?> loader;
        private int contextLines = 2;

        /**
         * Sets the loader used to read source lines. Required.
         *
         * @param loader the loader
         * @return this builder
         */
        public Builder loader(Loader<?> loader) {
            this.loader = Objects.requireNonNull(loader, "loader");
            return this;
        }

        /**
         * Sets the number of source lines shown around an error.
         *
         * @param contextLines the context line count
         * @return this builder
         */
        public Builder contextLines(int contextLines) {
            if (contextLines < 0) {
                throw new IllegalArgumentException("contextLines must not be negative");
            }
            this.contextLines = contextLines;
            return this;
        }

        /**
         * Builds the diagnostics service.
         *
         * @return the diagnostics service
         * @throws IllegalStateException if no loader is set
         */
        public TemplateDiagnostics build() {
            if (loader == null) {
                throw new IllegalStateException("A loader is required");
            }
            return new TemplateDiagnostics(this);
        }
    }
}
