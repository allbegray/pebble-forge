package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.extension.core.DefaultFilter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

final class PolicyAwareDefaultFilter extends DefaultFilter {

    private final DefaultFilter delegate;
    private final SandboxPolicyResolver policies;

    PolicyAwareDefaultFilter(DefaultFilter delegate, SandboxPolicyResolver policies) {
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
                policies, self, SandboxPolicySupport.Kind.FILTER, "default", lineNumber);
        return delegate.apply(input, args, self, context, lineNumber);
    }
}
