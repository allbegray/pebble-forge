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
    private final boolean isolated;

    WithNode(int lineNumber, Expression<?> valuesExpression, BodyNode body, boolean isolated) {
        super(lineNumber);
        this.valuesExpression = valuesExpression;
        this.body = body;
        this.isolated = isolated;
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
        if (isolated) {
            renderIsolated(scope, values, self, writer, context);
            return;
        }

        scope.pushScope(values);
        try {
            body.render(self, writer, context);
        } finally {
            scope.popScope();
        }
    }

    private void renderIsolated(ScopeChain scope, Map<String, Object> values,
            PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context)
            throws IOException {
        Map<String, Object> local = new LinkedHashMap<>(values);
        copyIfVisible(scope, context, local, "__slots");
        copyIfVisible(scope, context, local, "__component");
        copyIfVisible(scope, context, local, "locale");
        copyIfVisible(scope, context, local, "template");
        copyIfVisible(scope, context, local, "_context");

        scope.pushLocalScope();
        try {
            for (Map.Entry<String, Object> entry : local.entrySet()) {
                scope.put(entry.getKey(), entry.getValue());
            }
            body.render(self, writer, context);
        } finally {
            scope.popScope();
        }
    }

    private static void copyIfVisible(ScopeChain scope, EvaluationContextImpl context,
            Map<String, Object> target, String name) {
        if (scope.containsKey(name)) {
            target.put(name, context.getVariable(name));
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

    /**
     * Returns the expression that produces the scoped variables.
     *
     * @return the values expression, or {@code null}
     */
    public Expression<?> getValuesExpression() {
        return valuesExpression;
    }

    /**
     * Returns the body rendered with the extra variables.
     *
     * @return the body
     */
    public BodyNode getBody() {
        return body;
    }

    /**
     * Returns whether the body runs in an isolated scope.
     *
     * @return true when the body cannot see the caller's variables
     */
    public boolean isIsolated() {
        return isolated;
    }
}
