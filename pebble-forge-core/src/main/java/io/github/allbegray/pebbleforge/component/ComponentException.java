package io.github.allbegray.pebbleforge.component;

import io.pebbletemplates.pebble.error.PebbleException;

/**
 * A component could not be rendered.
 */
public final class ComponentException extends PebbleException {

    /**
     * Creates an exception without a template location.
     *
     * @param message the detail message
     */
    public ComponentException(String message) {
        super(null, message);
    }

    /**
     * Creates an exception with a template location.
     *
     * @param message the detail message
     * @param templateName the template name
     * @param lineNumber the line in the template
     */
    public ComponentException(String message, String templateName, int lineNumber) {
        super(null, message, lineNumber, templateName);
    }
}
