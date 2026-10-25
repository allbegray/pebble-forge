package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

final class PolicyAwareFilter implements Filter {

    private final String name;
    private final Filter delegate;
    private final SandboxPolicyResolver policies;

    PolicyAwareFilter(String name, Filter delegate, SandboxPolicyResolver policies) {
        this.name = name;
        this.delegate = delegate;
        this.policies = policies;
    }

    @Override
    public List<String> getArgumentNames() {
        return delegate.getArgumentNames();
    }

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self,
                        EvaluationContext context, int lineNumber) {
        SandboxPolicySupport.requireAllowed(
                policies, self, SandboxPolicySupport.Kind.FILTER, name, lineNumber);
        return delegate.apply(input, args, self, context, lineNumber);
    }
}
