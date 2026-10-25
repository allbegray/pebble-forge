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

    /**
     * Pushes a frame onto the current render trace.
     *
     * @param frame the frame to push
     */
    public static void push(Frame frame) {
        STACK.get().addLast(Objects.requireNonNull(frame, "frame"));
    }

    /**
     * Returns the current stack size, to be used with {@link #popTo(int)}.
     *
     * @return the current stack size
     */
    public static int mark() {
        return STACK.get().size();
    }

    /**
     * Removes every frame above {@code mark}. Values below zero are treated as
     * zero, and values above the current size are ignored.
     *
     * @param mark the stack size to keep
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
     *
     * @return the include depth
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
     *
     * @return the current frames
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
     *
     * @param context the evaluation context of the template that is starting
     */
    public static void resetIfNewRender(EvaluationContextImpl context) {
        if (context.getHierarchy().getChild() != null || INCLUDE_NESTING.get() > 0) {
            return;
        }
        reset();
    }

    /**
     * Marks the start of an include.
     */
    public static void enterInclude() {
        INCLUDE_NESTING.set(INCLUDE_NESTING.get() + 1);
    }

    /**
     * Marks the end of an include.
     */
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
     *
     * @param error the render failure
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
     *
     * @param error the error to inspect
     * @return the attached frames, or an empty list
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
     *
     * @param action the render action to run
     * @throws IOException if the action fails
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

    /**
     * The kind of template frame.
     */
    public enum Kind {

        /**
         * An include frame.
         */
        INCLUDE,

        /**
         * An inheritance frame.
         */
        EXTENDS
    }

    /**
     * One template call in a render trace.
     *
     * @param kind the frame kind
     * @param template the calling template
     * @param line the line of the call
     * @param target the included or extended template
     */
    public record Frame(Kind kind, String template, int line, String target) {
    }

    /**
     * A render action that can fail with an I/O error.
     */
    @FunctionalInterface
    public interface IOAction {

        /**
         * Runs the action.
         *
         * @throws IOException if the action fails
         */
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
