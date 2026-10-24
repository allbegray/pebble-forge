package io.github.allbegray.pebbleforge.diagnostics;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;

/**
 * Replaces the include tag with a version that records the include path on failure.
 */
public final class DiagnosticsExtension extends AbstractExtension {

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(new DiagnosticsIncludeTokenParser());
    }
}
