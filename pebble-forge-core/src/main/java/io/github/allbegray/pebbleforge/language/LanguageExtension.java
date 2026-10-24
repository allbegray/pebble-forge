package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;

/**
 * Adds switch, break, continue, and with tags. It also replaces for with a
 * break/continue-aware version.
 */
public final class LanguageExtension extends AbstractExtension {

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(
                new ForTokenParser(),
                new SwitchTokenParser(),
                new BreakTokenParser(),
                new ContinueTokenParser(),
                new WithTokenParser());
    }
}
