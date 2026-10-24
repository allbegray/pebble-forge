package io.github.allbegray.pebbleforge.language;

/**
 * Thrown by a continue tag and caught by the nearest loop.
 */
final class ContinueSignal extends RuntimeException {

    static final ContinueSignal INSTANCE = new ContinueSignal();

    private ContinueSignal() {
        super(null, null, false, false);
    }
}
