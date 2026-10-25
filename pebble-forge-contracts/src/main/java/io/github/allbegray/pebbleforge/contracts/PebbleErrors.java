package io.github.allbegray.pebbleforge.contracts;

import io.pebbletemplates.pebble.error.PebbleException;

final class PebbleErrors {

    private PebbleErrors() {
    }

    static String messageOf(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message;
    }

    static int lineOf(PebbleException error) {
        Integer line = error.getLineNumber();
        return line == null ? 0 : line;
    }
}
