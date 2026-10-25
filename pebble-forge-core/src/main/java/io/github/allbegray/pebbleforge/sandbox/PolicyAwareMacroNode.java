package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.node.MacroNode;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.Macro;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.util.List;
import java.util.Map;

final class PolicyAwareMacroNode extends MacroNode {

    private final String owner;

    PolicyAwareMacroNode(MacroNode delegate, String owner) {
        super(delegate.getName(), delegate.getArgs(), delegate.getBody());
        this.owner = owner;
    }

    @Override
    public Macro getMacro() {
        Macro delegate = super.getMacro();
        return new Macro() {

            @Override
            public List<String> getArgumentNames() {
                return delegate.getArgumentNames();
            }

            @Override
            public String getName() {
                return delegate.getName();
            }

            @Override
            public String call(PebbleTemplateImpl self, EvaluationContextImpl context,
                    Map<String, Object> args) {
                String previous = PolicyContext.swap(owner);
                try {
                    return delegate.call(self, context, args);
                } finally {
                    PolicyContext.restore(previous);
                }
            }
        };
    }
}
