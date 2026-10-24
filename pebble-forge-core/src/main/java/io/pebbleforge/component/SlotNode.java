package io.pebbleforge.component;

import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.io.IOException;
import java.io.Writer;

/**
 * A named block inside a component call. It only renders when a component captures it.
 */
public final class SlotNode extends AbstractRenderableNode {

    private final Expression<?> nameExpression;
    private final BodyNode body;

    public SlotNode(int lineNumber, Expression<?> nameExpression, BodyNode body) {
        super(lineNumber);
        this.nameExpression = nameExpression;
        this.body = body;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        if (nameExpression != null) {
            nameExpression.accept(visitor);
        }
        body.accept(visitor);
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context) {
        throw new ComponentException(
                "The slot tag is only valid directly inside a component body",
                self.getName(),
                getLineNumber());
    }

    String resolveName(PebbleTemplateImpl self, EvaluationContextImpl context) {
        if (nameExpression == null) {
            return "body";
        }
        Object value = nameExpression.evaluate(self, context);
        if (value == null) {
            throw new ComponentException(
                    "Slot name evaluated to null", self.getName(), getLineNumber());
        }
        return String.valueOf(value);
    }

    public BodyNode getBody() {
        return body;
    }
}
