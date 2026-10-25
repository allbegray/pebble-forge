package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

final class PolicyAwareFunction implements Function {

    private final String name;
    private final Function delegate;
    private final SandboxPolicyResolver policies;

    PolicyAwareFunction(String name, Function delegate, SandboxPolicyResolver policies) {
        this.name = name;
        this.delegate = delegate;
        this.policies = policies;
    }

    @Override
    public List<String> getArgumentNames() {
        return delegate.getArgumentNames();
    }

    @Override
    public Object execute(Map<String, Object> args, PebbleTemplate self,
                          EvaluationContext context, int lineNumber) {
        SandboxPolicySupport.requireAllowed(
                policies, self, SandboxPolicySupport.Kind.FUNCTION, name, lineNumber);
        return delegate.execute(args, self, context, lineNumber);
    }
}
