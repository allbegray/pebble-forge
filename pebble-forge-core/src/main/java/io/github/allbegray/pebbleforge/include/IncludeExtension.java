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

    /**
     * Creates an extension without a depth limit.
     */
    public IncludeExtension() {
        this(SandboxLimitsResolver.fixed(SandboxLimits.unlimited()));
    }

    /**
     * Creates an extension with an include depth limit.
     *
     * @param maxDepth the maximum nested include depth
     */
    public IncludeExtension(int maxDepth) {
        this(SandboxLimitsResolver.fixed(SandboxLimits.builder().maxIncludeDepth(maxDepth).build()));
    }

    /**
     * Creates an extension that resolves limits per template.
     *
     * @param limitsResolver the limits resolver
     */
    public IncludeExtension(SandboxLimitsResolver limitsResolver) {
        this.limitsResolver = limitsResolver;
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(new ForgeIncludeTokenParser(limitsResolver));
    }
}
