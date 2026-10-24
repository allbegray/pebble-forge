package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import io.pebbletemplates.pebble.template.ScopeChain;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders a body with an extra map of scoped variables.
 */
public final class WithNode extends AbstractRenderableNode {

    private final Expression<?> valuesExpression;
    private final BodyNode body;

    WithNode(int lineNumber, Expression<?> valuesExpression, BodyNode body) {
        super(lineNumber);
        this.valuesExpression = valuesExpression;
        this.body = body;
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context)
            throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        if (valuesExpression != null) {
            Object evaluated = valuesExpression.evaluate(self, context);
            if (evaluated != null) {
                if (!(evaluated instanceof Map<?, ?> map)) {
                    throw new PebbleException(
                            null,
                            "The with tag requires a map",
                            getLineNumber(),
                            self.getName());
                }
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        throw new PebbleException(
                                null,
                                "The with tag map keys must be strings",
                                getLineNumber(),
                                self.getName());
                    }
                    values.put(key, entry.getValue());
                }
            }
        }

        ScopeChain scope = context.getScopeChain();
        scope.pushScope(values);
        try {
            body.render(self, writer, context);
        } finally {
            scope.popScope();
        }
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
        if (valuesExpression != null) {
            valuesExpression.accept(visitor);
        }
        body.accept(visitor);
    }

    public Expression<?> getValuesExpression() {
        return valuesExpression;
    }

    public BodyNode getBody() {
        return body;
    }
}
