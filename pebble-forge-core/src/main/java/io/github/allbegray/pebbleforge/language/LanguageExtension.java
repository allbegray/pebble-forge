package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;

/**
 * Adds switch, break, continue, and with tags. It also replaces for with a
 * break/continue-aware version.
 */
public final class LanguageExtension extends AbstractExtension {

    private final int maxLoopIterations;
    private final int maxTotalIterations;

    public LanguageExtension() {
        this(-1, -1);
    }

    public LanguageExtension(int maxLoopIterations) {
        this(maxLoopIterations, -1);
    }

    public LanguageExtension(int maxLoopIterations, int maxTotalIterations) {
        if (maxLoopIterations < -1) {
            throw new IllegalArgumentException("maxLoopIterations must be -1 or greater");
        }
        if (maxTotalIterations < -1) {
            throw new IllegalArgumentException("maxTotalIterations must be -1 or greater");
        }
        this.maxLoopIterations = maxLoopIterations;
        this.maxTotalIterations = maxTotalIterations;
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(
                new ForTokenParser(maxLoopIterations, maxTotalIterations),
                new SwitchTokenParser(),
                new BreakTokenParser(),
                new ContinueTokenParser(),
                new WithTokenParser());
    }
}
