package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.extension.core.DefinedTest;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

final class PolicyAwareDefinedTest extends DefinedTest {

    private final DefinedTest delegate;
    private final SandboxPolicyResolver policies;

    PolicyAwareDefinedTest(DefinedTest delegate, SandboxPolicyResolver policies) {
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
                policies, self, SandboxPolicySupport.Kind.TEST, "defined", lineNumber);
        return delegate.apply(input, args, self, context, lineNumber);
    }
}
