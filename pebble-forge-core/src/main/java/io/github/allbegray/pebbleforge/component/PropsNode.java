package io.github.allbegray.pebbleforge.component;

import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.util.List;

/**
 * Declares required props and default values inside a component template.
 */
public final class PropsNode extends AbstractRenderableNode {

    public record Prop(String name, Expression<?> defaultValue) {
    }

    private final List<Prop> props;

    public PropsNode(int lineNumber, List<Prop> props) {
        super(lineNumber);
        this.props = List.copyOf(props);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        for (Prop prop : props) {
            if (prop.defaultValue() != null) {
                prop.defaultValue().accept(visitor);
            }
        }
    }

    @Override
    public void render(PebbleTemplateImpl self, java.io.Writer writer, EvaluationContextImpl context) {
        var scope = context.getScopeChain();
        for (Prop prop : props) {
            if (scope.currentScopeContainsVariable(prop.name())) {
                continue;
            }
            if (prop.defaultValue() != null) {
                scope.put(prop.name(), prop.defaultValue().evaluate(self, context));
                continue;
            }
            throw new ComponentException(
                    "Missing required prop [" + prop.name() + "] for component " + self.getName(),
                    self.getName(),
                    getLineNumber());
        }
    }
}
