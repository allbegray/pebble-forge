package io.github.allbegray.pebbleforge.language;

import io.github.allbegray.pebbleforge.sandbox.SandboxLimits;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimitsResolver;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;

/**
 * Adds switch, break, continue, and with tags. It also replaces for with a
 * break/continue-aware version.
 */
public final class LanguageExtension extends AbstractExtension {

    private final SandboxLimitsResolver limitsResolver;

    /**
     * Creates an extension without resource limits.
     */
    public LanguageExtension() {
        this(SandboxLimitsResolver.fixed(SandboxLimits.unlimited()));
    }

    /**
     * Creates an extension with loop limits.
     *
     * @param maxLoopIterations the maximum iterations of one loop
     * @param maxTotalIterations the maximum iterations across all nested loops
     */
    public LanguageExtension(int maxLoopIterations, int maxTotalIterations) {
        this(SandboxLimitsResolver.fixed(SandboxLimits.builder()
                .maxLoopIterations(maxLoopIterations)
                .maxTotalIterations(maxTotalIterations)
                .build()));
    }

    /**
     * Creates an extension that resolves limits per template.
     *
     * @param limitsResolver the limits resolver
     */
    public LanguageExtension(SandboxLimitsResolver limitsResolver) {
        this.limitsResolver = limitsResolver;
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(
                new ForTokenParser(limitsResolver),
                new SwitchTokenParser(),
                new BreakTokenParser(),
                new ContinueTokenParser(),
                new WithTokenParser());
    }
}
