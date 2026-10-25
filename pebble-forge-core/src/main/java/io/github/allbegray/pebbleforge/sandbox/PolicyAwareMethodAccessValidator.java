package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.methodaccess.MethodAccessValidator;

import java.lang.reflect.Method;

/**
 * Selects the method validator for the template currently being resolved.
 */
public final class PolicyAwareMethodAccessValidator implements MethodAccessValidator {

    private final SandboxPolicyResolver policies;

    public PolicyAwareMethodAccessValidator(SandboxPolicyResolver policies) {
        this.policies = policies;
    }

    @Override
    public boolean isMethodAccessAllowed(Object object, Method method) {
        String templateName = PolicyContext.current();
        if (templateName == null) {
            return false;
        }
        SandboxPolicy policy = policies.resolve(templateName);
        return policy.methodAccessValidator().isMethodAccessAllowed(object, method);
    }
}
