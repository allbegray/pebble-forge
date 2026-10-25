package io.github.allbegray.pebbleforge.include;

import io.github.allbegray.pebbleforge.sandbox.SandboxLimits;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimitsResolver;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;

/**
 * Replaces include with a depth-limited, trace-aware version.
 */
public final class IncludeExtension extends AbstractExtension {

    private final SandboxLimitsResolver limitsResolver;

    public IncludeExtension() {
        this(SandboxLimitsResolver.fixed(SandboxLimits.unlimited()));
    }

    public IncludeExtension(int maxDepth) {
        this(SandboxLimitsResolver.fixed(SandboxLimits.builder().maxIncludeDepth(maxDepth).build()));
    }

    public IncludeExtension(SandboxLimitsResolver limitsResolver) {
        this.limitsResolver = limitsResolver;
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(new ForgeIncludeTokenParser(limitsResolver));
    }
}
