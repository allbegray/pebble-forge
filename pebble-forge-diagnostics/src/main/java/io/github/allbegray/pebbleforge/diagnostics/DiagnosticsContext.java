package io.github.allbegray.pebbleforge.diagnostics;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DiagnosticsContext {

    private static final ThreadLocal<Deque<Frame>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private DiagnosticsContext() {
    }

    static void push(Frame frame) {
        STACK.get().addLast(frame);
    }

    static void pop() {
        Deque<Frame> frames = STACK.get();
        if (!frames.isEmpty()) {
            frames.removeLast();
        }
        if (frames.isEmpty()) {
            STACK.remove();
        }
    }

    static void attach(RuntimeException error) {
        if (findTrace(error, new HashSet<>()) != null) {
            return;
        }
        Deque<Frame> frames = STACK.get();
        if (frames.isEmpty()) {
            return;
        }
        error.addSuppressed(new Trace(List.copyOf(frames)));
    }

    static List<Frame> frames(Throwable error) {
        Trace trace = findTrace(error, new HashSet<>());
        return trace == null ? List.of() : trace.frames;
    }

    private static Trace findTrace(Throwable error, Set<Throwable> seen) {
        if (error == null || !seen.add(error)) {
            return null;
        }
        for (Throwable suppressed : error.getSuppressed()) {
            if (suppressed instanceof Trace trace) {
                return trace;
            }
            Trace nested = findTrace(suppressed, seen);
            if (nested != null) {
                return nested;
            }
        }
        if (error.getCause() != null && error.getCause() != error) {
            return findTrace(error.getCause(), seen);
        }
        return null;
    }

    record Frame(String caller, int line, String target) {
    }

    static final class Trace extends RuntimeException {

        private final List<Frame> frames;

        private Trace(List<Frame> frames) {
            super(null, null, false, false);
            this.frames = List.copyOf(frames);
        }
    }
}
