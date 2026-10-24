package io.github.allbegray.pebbleforge.component;

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

    public ComponentsExtension(Components components) {
        this.components = components;
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return List.of(
                new ComponentTokenParser(components),
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
