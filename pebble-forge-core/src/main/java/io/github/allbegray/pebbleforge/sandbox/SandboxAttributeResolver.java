package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.attributes.ResolvedAttribute;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.node.ArgumentsNode;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;

import java.lang.reflect.Field;

/**
 * Rejects dangerous objects and public field access before Pebble's default resolver runs.
 */
public final class SandboxAttributeResolver implements AttributeResolver {

    private final SandboxPolicyResolver policies;

    public SandboxAttributeResolver(SandboxPolicy policy) {
        this(SandboxPolicyResolver.fixed(policy));
    }

    public SandboxAttributeResolver(SandboxPolicyResolver policies) {
        this.policies = policies;
    }

    @Override
    public ResolvedAttribute resolve(
            Object instance,
            Object attributeNameValue,
            Object[] argumentValues,
            ArgumentsNode args,
            EvaluationContextImpl context,
            String filename,
            int lineNumber) {
        if (instance == null) {
            return null;
        }

        String name = String.valueOf(attributeNameValue);
        SandboxPolicy policy = SandboxPolicySupport.policyFor(policies, filename);
        if (SandboxMethodAccessValidator.isDeniedObject(instance)) {
            throw denied(name, filename, lineNumber);
        }
        if ("class".equals(name) || "metaClass".equals(name)) {
            throw denied(name, filename, lineNumber);
        }
        if (policy.denyPublicFields() && hasPublicField(instance.getClass(), name)) {
            throw new PebbleException(
                    null,
                    "Public field access is not allowed in sandbox mode: " + name,
                    lineNumber,
                    filename);
        }
        return null;
    }

    private static boolean hasPublicField(Class<?> type, String name) {
        try {
            Field field = type.getField(name);
            return field != null;
        } catch (NoSuchFieldException | SecurityException ignored) {
            return false;
        }
    }

    private static PebbleException denied(String name, String filename, int lineNumber) {
        return new PebbleException(
                null,
                "Access to [" + name + "] is not allowed in sandbox mode",
                lineNumber,
                filename);
    }
}
