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

    public LanguageExtension() {
        this(SandboxLimitsResolver.fixed(SandboxLimits.unlimited()));
    }

    public LanguageExtension(int maxLoopIterations, int maxTotalIterations) {
        this(SandboxLimitsResolver.fixed(SandboxLimits.builder()
                .maxLoopIterations(maxLoopIterations)
                .maxTotalIterations(maxTotalIterations)
                .build()));
    }

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
