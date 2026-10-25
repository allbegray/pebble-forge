package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.node.BlockNode;
import io.pebbletemplates.pebble.node.EmbedNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.expression.MapExpression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class PolicyAwareEmbedNode extends EmbedNode {

    private final Expression<?> includeExpression;
    private final MapExpression mapExpression;
    private final List<BlockNode> nodes;

    PolicyAwareEmbedNode(int lineNumber, Expression<?> includeExpression,
            MapExpression mapExpression, List<BlockNode> nodes) {
        super(lineNumber, includeExpression, mapExpression, nodes);
        this.includeExpression = includeExpression;
        this.mapExpression = mapExpression;
        this.nodes = nodes;
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context)
            throws IOException {
        String templateName = (String) includeExpression.evaluate(self, context);
        if (templateName == null) {
            throw new PebbleException(
                    null,
                    "The template name in an embed tag evaluated to NULL. If the template name "
                            + "is static, make sure to wrap it in quotes.",
                    getLineNumber(),
                    self.getName());
        }

        Map<?, ?> map = Collections.emptyMap();
        if (mapExpression != null) {
            map = mapExpression.evaluate(self, context);
        }

        String resolved = self.resolveRelativePath(templateName);
        String previous = PolicyContext.swap(resolved);
        try {
            self.embedTemplate(getLineNumber(), writer, context, templateName, map, nodes);
        } finally {
            PolicyContext.restore(previous);
        }
    }
}
