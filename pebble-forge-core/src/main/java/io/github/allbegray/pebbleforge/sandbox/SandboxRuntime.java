package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;

/**
 * Runs template work with the policy owner that declared the current node.
 */
public final class SandboxRuntime {

    private SandboxRuntime() {
    }

    /**
     * Returns the template name that owns the current work.
     *
     * @param self the template being rendered
     * @return the policy owner's template name, or the rendered template name
     */
    public static String currentTemplateName(PebbleTemplate self) {
        String current = PolicyContext.current();
        return current == null ? self.getName() : current;
    }

    /**
     * Runs an action under a template name.
     *
     * @param templateName the template that owns the action
     * @param action the action to run
     * @throws IOException if the action fails
     */
    public static void runWith(String templateName, IOAction action) throws IOException {
        PolicyContext.runWith(templateName, action::run);
    }

    /**
     * An action that writes template output.
     */
    @FunctionalInterface
    public interface IOAction {

        /**
         * Runs the action.
         *
         * @throws IOException if writing fails
         */
        void run() throws IOException;
    }
}
