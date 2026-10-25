package io.github.allbegray.pebbleforge.sandbox;

import java.io.IOException;

/**
 * Carries the template currently being resolved on the current thread.
 */
final class PolicyContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private PolicyContext() {
    }

    static String current() {
        return CURRENT.get();
    }

    static String swap(String templateName) {
        String previous = CURRENT.get();
        if (templateName == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(templateName);
        }
        return previous;
    }

    static void restore(String previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    static void runWith(String templateName, IOAction action) throws IOException {
        String previous = swap(templateName);
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    @FunctionalInterface
    interface IOAction {
        void run() throws IOException;
    }
}
