package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;

/**
 * Runs template work with the policy owner that declared the current node.
 */
public final class SandboxRuntime {

    private SandboxRuntime() {
    }

    public static String currentTemplateName(PebbleTemplate self) {
        String current = PolicyContext.current();
        return current == null ? self.getName() : current;
    }

    public static void runWith(String templateName, IOAction action) throws IOException {
        PolicyContext.runWith(templateName, action::run);
    }

    @FunctionalInterface
    public interface IOAction {
        void run() throws IOException;
    }
}
