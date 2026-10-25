package io.github.allbegray.pebbleforge.component;

import io.github.allbegray.pebbleforge.sandbox.SandboxLimits;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimitsResolver;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;
import java.util.Map;

/**
 * Registers the component, slot, and props tags and the slot function.
 */
public final class ComponentsExtension extends AbstractExtension {

    private final Components components;
    private final SandboxLimitsResolver limitsResolver;

    /**
     * Creates an extension without resource limits.
     *
     * @param components the component configuration
     */
    public ComponentsExtension(Components components) {
        this(components, SandboxLimitsResolver.fixed(SandboxLimits.unlimited()));
    }

    /**
     * Creates an extension.
     *
     * @param components the component configuration
     * @param limitsResolver the resolver for per-template resource limits
     */
    public ComponentsExtension(Components components, SandboxLimitsResolver limitsResolver) {
        this.components = components;
        this.limitsResolver = limitsResolver;
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(
                new ComponentTokenParser(components, limitsResolver),
                new SlotTokenParser(),
                new PropsTokenParser());
    }

    @Override
    public Map<String, Function> getFunctions() {
        return Map.of("slot", new SlotFunction());
    }

    /**
     * Creates a component extension without resource limits.
     *
     * @param components the component configuration
     * @return the extension
     */
    public static Extension create(Components components) {
        return new ComponentsExtension(components);
    }
}
