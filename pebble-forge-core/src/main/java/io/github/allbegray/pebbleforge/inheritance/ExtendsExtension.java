package io.github.allbegray.pebbleforge.inheritance;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;

/**
 * Replaces extends with a trace-aware, cycle-safe version.
 */
public final class ExtendsExtension extends AbstractExtension {

    /**
     * Creates an extends extension.
     */
    public ExtendsExtension() {
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(new ForgeExtendsTokenParser());
    }
}
