package io.github.allbegray.pebbleforge.include;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks nested include frames and attaches them to render failures.
 */
public final class IncludeTrace {

    private static final ThreadLocal<Deque<IncludeFrame>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private IncludeTrace() {
    }

    static void push(IncludeFrame frame) {
        STACK.get().addLast(frame);
    }

    static void pop() {
        Deque<IncludeFrame> frames = STACK.get();
        if (!frames.isEmpty()) {
            frames.removeLast();
        }
        if (frames.isEmpty()) {
            STACK.remove();
        }
    }

    static int depth() {
        return STACK.get().size();
    }

    static void attach(RuntimeException error) {
        if (findTrace(error, new HashSet<>()) != null) {
            return;
        }
        Deque<IncludeFrame> frames = STACK.get();
        if (frames.isEmpty()) {
            return;
        }
        error.addSuppressed(new Trace(List.copyOf(frames)));
    }

    public static List<IncludeFrame> frames(Throwable error) {
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

    public record IncludeFrame(String caller, int line, String target) {
    }

    private static final class Trace extends RuntimeException {

        private final List<IncludeFrame> frames;

        private Trace(List<IncludeFrame> frames) {
            super(null, null, false, false);
            this.frames = List.copyOf(frames);
        }
    }
}
