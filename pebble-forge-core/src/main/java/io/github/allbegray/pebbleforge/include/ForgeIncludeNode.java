package io.github.allbegray.pebbleforge.include;

import io.github.allbegray.pebbleforge.sandbox.SandboxLimits;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimitsResolver;
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
 * Include node with depth limiting and failure tracing.
 */
final class ForgeIncludeNode extends AbstractRenderableNode {

    private final Expression<?> includeExpression;
    private final MapExpression mapExpression;
    private final SandboxLimitsResolver limitsResolver;

    ForgeIncludeNode(int lineNumber, Expression<?> includeExpression,
            MapExpression mapExpression, SandboxLimitsResolver limitsResolver) {
        super(lineNumber);
        this.includeExpression = includeExpression;
        this.mapExpression = mapExpression;
        this.limitsResolver = limitsResolver;
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

        SandboxLimits limits = limitsResolver.resolve(self.getName());
        int maxDepth = limits.maxIncludeDepth();
        if (maxDepth >= 0 && IncludeTrace.depth() >= maxDepth) {
            throw new PebbleException(
                    null,
                    "Include depth limit of " + maxDepth + " reached",
                    getLineNumber(),
                    self.getName());
        }

        Map<?, ?> map = Collections.emptyMap();
        if (mapExpression != null) {
            map = mapExpression.evaluate(self, context);
        }

        IncludeTrace.push(new IncludeTrace.IncludeFrame(self.getName(), getLineNumber(),
                templateName));
        try {
            self.includeTemplate(writer, context, templateName, map);
        } catch (RuntimeException error) {
            IncludeTrace.attach(error);
            throw error;
        } finally {
            IncludeTrace.pop();
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
