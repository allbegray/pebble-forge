package io.github.allbegray.pebbleforge.language;

/**
 * Thrown by a break tag and caught by the nearest loop or switch.
 */
final class BreakSignal extends RuntimeException {

    static final BreakSignal INSTANCE = new BreakSignal();

    private BreakSignal() {
        super(null, null, false, false);
    }
}
