package io.pebbleforge.component;

import io.pebbletemplates.pebble.error.PebbleException;

/**
 * A component could not be rendered.
 */
public final class ComponentException extends PebbleException {

    public ComponentException(String message) {
        super(null, message);
    }

    public ComponentException(String message, String templateName, int lineNumber) {
        super(null, message, lineNumber, templateName);
    }
}
