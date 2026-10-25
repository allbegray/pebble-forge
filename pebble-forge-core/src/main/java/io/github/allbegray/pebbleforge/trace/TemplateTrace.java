package io.github.allbegray.pebbleforge.trace;

import io.pebbletemplates.pebble.template.EvaluationContextImpl;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks include and inheritance frames for the current render and attaches
 * them to render failures.
 */
public final class TemplateTrace {

    private static final ThreadLocal<Deque<Frame>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private static final ThreadLocal<Integer> INCLUDE_NESTING = ThreadLocal.withInitial(() -> 0);

    private static final ThreadLocal<Integer> RUN_DEPTH = ThreadLocal.withInitial(() -> 0);

    private TemplateTrace() {
    }

    public static void push(Frame frame) {
        STACK.get().addLast(Objects.requireNonNull(frame, "frame"));
    }

    /**
     * Returns the current stack size, to be used with {@link #popTo(int)}.
     */
    public static int mark() {
        return STACK.get().size();
    }

    /**
     * Removes every frame above {@code mark}. Values below zero are treated as
     * zero, and values above the current size are ignored.
     */
    public static void popTo(int mark) {
        int target = Math.max(0, mark);
        Deque<Frame> frames = STACK.get();
        while (frames.size() > target) {
            frames.removeLast();
        }
        if (frames.isEmpty()) {
            STACK.remove();
        }
    }

    /**
     * Counts only include frames; inheritance frames do not consume include
     * depth.
     */
    public static int includeDepth() {
        int depth = 0;
        for (Frame frame : STACK.get()) {
            if (frame.kind() == Kind.INCLUDE) {
                depth++;
            }
        }
        return depth;
    }

    /**
     * Returns a snapshot of the current frames, in chronological order.
     */
    public static List<Frame> currentFrames() {
        return List.copyOf(STACK.get());
    }

    /**
     * Drops every frame, including frames left behind by a render that did not
     * use {@link #run(IOAction)}.
     */
    public static void reset() {
        STACK.remove();
    }

    /**
     * Clears frames from an earlier top-level render when a new template root
     * is entered. Parent evaluation and templates entered through
     * {@code include} keep the existing chain.
     */
    public static void resetIfNewRender(EvaluationContextImpl context) {
        if (context.getHierarchy().getChild() != null || INCLUDE_NESTING.get() > 0) {
            return;
        }
        reset();
    }

    public static void enterInclude() {
        INCLUDE_NESTING.set(INCLUDE_NESTING.get() + 1);
    }

    public static void exitInclude() {
        int nesting = INCLUDE_NESTING.get() - 1;
        if (nesting <= 0) {
            INCLUDE_NESTING.remove();
        } else {
            INCLUDE_NESTING.set(nesting);
        }
    }

    /**
     * Attaches the current frames to the error once. Repeated calls, or calls
     * after an inner node has attached a trace, are no-ops.
     */
    public static void attach(RuntimeException error) {
        if (error == null || findTrace(error, new HashSet<>()) != null) {
            return;
        }
        Deque<Frame> frames = STACK.get();
        if (frames.isEmpty()) {
            return;
        }
        error.addSuppressed(new Trace(List.copyOf(frames)));
    }

    /**
     * Returns the frames attached to the error, walking suppressed exceptions
     * and causes, in chronological order.
     */
    public static List<Frame> frames(Throwable error) {
        Trace trace = findTrace(error, new HashSet<>());
        return trace == null ? List.of() : trace.frames;
    }

    /**
     * Runs a render while guaranteeing the trace stack is restored, attaching
     * the frames to any runtime failure.
     *
     * <p>Pebble evaluates an extended parent only after the child root node
     * returns, so callers must wrap the whole evaluate call to capture
     * parent-body failures.
     */
    public static void run(IOAction action) throws IOException {
        Objects.requireNonNull(action, "action");
        int depth = RUN_DEPTH.get();
        int mark;
        if (depth == 0) {
            reset();
            mark = 0;
        } else {
            mark = mark();
        }
        RUN_DEPTH.set(depth + 1);
        try {
            action.run();
        } catch (RuntimeException error) {
            attach(error);
            throw error;
        } finally {
            popTo(mark);
            if (depth == 0) {
                RUN_DEPTH.remove();
            } else {
                RUN_DEPTH.set(depth);
            }
        }
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

    public enum Kind {
        INCLUDE,
        EXTENDS
    }

    public record Frame(Kind kind, String template, int line, String target) {
    }

    @FunctionalInterface
    public interface IOAction {

        void run() throws IOException;
    }

    private static final class Trace extends RuntimeException {

        private final List<Frame> frames;

        private Trace(List<Frame> frames) {
            super(null, null, false, false);
            this.frames = List.copyOf(frames);
        }
    }
}
