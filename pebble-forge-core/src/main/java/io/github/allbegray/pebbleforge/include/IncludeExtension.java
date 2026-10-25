package io.github.allbegray.pebbleforge.include;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;

/**
 * Replaces include with a depth-limited, trace-aware version.
 */
public final class IncludeExtension extends AbstractExtension {

    private final int maxDepth;

    public IncludeExtension() {
        this(-1);
    }

    public IncludeExtension(int maxDepth) {
        if (maxDepth < -1) {
            throw new IllegalArgumentException("maxDepth must be -1 or greater");
        }
        this.maxDepth = maxDepth;
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(new ForgeIncludeTokenParser(maxDepth));
    }
}
