package io.github.allbegray.pebbleforge.diagnostics;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.expression.MapExpression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

/**
 * Include node that records the include path when a nested render fails.
 */
final class DiagnosticsIncludeNode extends AbstractRenderableNode {

    private final Expression<?> includeExpression;
    private final MapExpression mapExpression;

    DiagnosticsIncludeNode(int lineNumber, Expression<?> includeExpression,
            MapExpression mapExpression) {
        super(lineNumber);
        this.includeExpression = includeExpression;
        this.mapExpression = mapExpression;
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context)
            throws IOException {
        String templateName = (String) includeExpression.evaluate(self, context);
        if (templateName == null) {
            throw new PebbleException(
                    null,
                    "The template name in an include tag evaluated to NULL. "
                            + "If the template name is static, make sure to wrap it in quotes.",
                    getLineNumber(),
                    self.getName());
        }

        Map<?, ?> map = Collections.emptyMap();
        if (mapExpression != null) {
            map = mapExpression.evaluate(self, context);
        }

        DiagnosticsContext.push(new DiagnosticsContext.Frame(self.getName(), getLineNumber(),
                templateName));
        try {
            self.includeTemplate(writer, context, templateName, map);
        } catch (RuntimeException error) {
            DiagnosticsContext.attach(error);
            throw error;
        } finally {
            DiagnosticsContext.pop();
        }
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
        includeExpression.accept(visitor);
        if (mapExpression != null) {
            mapExpression.accept(visitor);
        }
    }
}
