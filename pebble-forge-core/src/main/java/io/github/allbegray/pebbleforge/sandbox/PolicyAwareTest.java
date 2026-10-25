package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

final class PolicyAwareTest implements Test {

    private final String name;
    private final Test delegate;
    private final SandboxPolicyResolver policies;

    PolicyAwareTest(String name, Test delegate, SandboxPolicyResolver policies) {
        this.name = name;
        this.delegate = delegate;
        this.policies = policies;
    }

    @Override
    public List<String> getArgumentNames() {
        return delegate.getArgumentNames();
    }

    @Override
    public boolean apply(Object input, Map<String, Object> args, PebbleTemplate self,
                         EvaluationContext context, int lineNumber) {
        SandboxPolicySupport.requireAllowed(
                policies, self, SandboxPolicySupport.Kind.TEST, name, lineNumber);
        return delegate.apply(input, args, self, context, lineNumber);
    }
}
