package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.extension.AbstractExtension;

import java.util.List;

/**
 * Adds the sandbox attribute resolver before Pebble's default resolver.
 */
public final class SandboxAttributeExtension extends AbstractExtension {

    private final SandboxPolicyResolver policies;

    public SandboxAttributeExtension(SandboxPolicy policy) {
        this(SandboxPolicyResolver.fixed(policy));
    }

    public SandboxAttributeExtension(SandboxPolicyResolver policies) {
        this.policies = policies;
    }

    @Override
    public List<AttributeResolver> getAttributeResolver() {
        return List.of(new SandboxAttributeResolver(policies));
    }
}
