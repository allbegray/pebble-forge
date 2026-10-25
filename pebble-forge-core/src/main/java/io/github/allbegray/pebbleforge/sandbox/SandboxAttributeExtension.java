package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.extension.AbstractExtension;

import java.util.List;

/**
 * Adds the sandbox attribute resolver before Pebble's default resolver.
 */
public final class SandboxAttributeExtension extends AbstractExtension {

    private final SandboxPolicyResolver policies;

    /**
     * Applies one policy.
     *
     * @param policy the policy to apply
     */
    public SandboxAttributeExtension(SandboxPolicy policy) {
        this(SandboxPolicyResolver.fixed(policy));
    }

    /**
     * Applies a policy resolver.
     *
     * @param policies the policy resolver
     */
    public SandboxAttributeExtension(SandboxPolicyResolver policies) {
        this.policies = policies;
    }

    @Override
    public List<AttributeResolver> getAttributeResolver() {
        return List.of(new SandboxAttributeResolver(policies));
    }
}
