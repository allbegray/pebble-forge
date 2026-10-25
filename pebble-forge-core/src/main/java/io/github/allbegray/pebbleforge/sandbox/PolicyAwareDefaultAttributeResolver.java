package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.attributes.DefaultAttributeResolver;
import io.pebbletemplates.pebble.attributes.ResolvedAttribute;
import io.pebbletemplates.pebble.node.ArgumentsNode;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes member resolution to a per-policy resolver so Pebble's member cache cannot
 * carry an authorization decision across policy boundaries.
 */
public final class PolicyAwareDefaultAttributeResolver implements AttributeResolver {

    private final SandboxPolicyResolver policies;
    private final ConcurrentHashMap<SandboxPolicy, DefaultAttributeResolver> delegates =
            new ConcurrentHashMap<>();

    /**
     * Creates a resolver that selects a delegate per sandbox policy.
     *
     * @param policies the policy resolver
     */
    public PolicyAwareDefaultAttributeResolver(SandboxPolicyResolver policies) {
        this.policies = policies;
    }

    @Override
    public ResolvedAttribute resolve(Object instance, Object attributeNameValue,
            Object[] argumentValues, ArgumentsNode args, EvaluationContextImpl context,
            String filename, int lineNumber) {
        if (instance == null) {
            return null;
        }
        SandboxPolicy policy = SandboxPolicySupport.policyFor(policies, filename);
        DefaultAttributeResolver delegate = delegates.computeIfAbsent(
                policy, ignored -> new DefaultAttributeResolver());
        String previous = PolicyContext.swap(filename);
        try {
            return delegate.resolve(instance, attributeNameValue, argumentValues, args,
                    context, filename, lineNumber);
        } finally {
            PolicyContext.restore(previous);
        }
    }
}
