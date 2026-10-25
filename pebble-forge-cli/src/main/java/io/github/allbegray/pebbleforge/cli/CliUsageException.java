package io.github.allbegray.pebbleforge.cli;

/**
 * A usage or model error. The CLI prints the message and exits with status 2.
 */
final class CliUsageException extends RuntimeException {

    CliUsageException(String message) {
        super(message, null, false, false);
    }
}
