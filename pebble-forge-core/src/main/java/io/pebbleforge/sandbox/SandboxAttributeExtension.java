package io.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.extension.AbstractExtension;

import java.util.List;

/**
 * Adds the sandbox attribute resolver before Pebble's default resolver.
 */
public final class SandboxAttributeExtension extends AbstractExtension {

    private final SandboxPolicy policy;

    public SandboxAttributeExtension(SandboxPolicy policy) {
        this.policy = policy;
    }

    @Override
    public List<AttributeResolver> getAttributeResolver() {
        return List.of(new SandboxAttributeResolver(policy));
    }
}
