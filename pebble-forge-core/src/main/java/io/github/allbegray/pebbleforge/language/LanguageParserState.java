package io.github.allbegray.pebbleforge.language;

/**
 * Tracks lexical loop and switch nesting while templates are parsed.
 */
final class LanguageParserState {

    private static final ThreadLocal<Integer> LOOP_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> BREAKABLE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private LanguageParserState() {
    }

    static int loopDepth() {
        return LOOP_DEPTH.get();
    }

    static int breakableDepth() {
        return BREAKABLE_DEPTH.get();
    }

    static void setLoopDepth(int depth) {
        LOOP_DEPTH.set(depth);
    }

    static void setBreakableDepth(int depth) {
        BREAKABLE_DEPTH.set(depth);
    }
}
