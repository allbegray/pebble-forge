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

    public ComponentsExtension(Components components) {
        this(components, SandboxLimitsResolver.fixed(SandboxLimits.unlimited()));
    }

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

    public static Extension create(Components components) {
        return new ComponentsExtension(components);
    }
}
