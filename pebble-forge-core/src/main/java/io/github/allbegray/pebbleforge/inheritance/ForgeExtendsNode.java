package io.github.allbegray.pebbleforge.inheritance;

import io.github.allbegray.pebbleforge.trace.TemplateTrace;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.node.ExtendsNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends node with inheritance tracing and cycle detection.
 */
final class ForgeExtendsNode extends ExtendsNode {

    ForgeExtendsNode(int lineNumber, Expression<?> parentExpression) {
        super(lineNumber, parentExpression);
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context) {
        TemplateTrace.resetIfNewRender(context);
        String templateName = (String) getParentExpression().evaluate(self, context);
        if (templateName == null) {
            throw new PebbleException(
                    null,
                    "The parent template name in an extends tag evaluated to NULL. "
                            + "If the template name is static, make sure to wrap it in quotes.",
                    getLineNumber(),
                    self.getName());
        }

        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, self.getName(),
                getLineNumber(), templateName));
        try {
            String resolved = self.resolveRelativePath(templateName);
            if (resolved == null) {
                resolved = templateName;
            }
            rejectCycle(self, resolved);
            self.setParent(context, templateName);
        } catch (RuntimeException error) {
            TemplateTrace.attach(error);
            throw error;
        }
    }

    private void rejectCycle(PebbleTemplateImpl self, String resolvedTarget) {
        List<TemplateTrace.Frame> frames = TemplateTrace.currentFrames();
        List<String> chain = new ArrayList<>();
        List<String> templates = new ArrayList<>();
        boolean cycle = resolvedTarget.equals(self.getName());

        int start = 0;
        for (int index = frames.size() - 1; index >= 0; index--) {
            TemplateTrace.Frame frame = frames.get(index);
            if (frame.kind() == TemplateTrace.Kind.INCLUDE) {
                start = index + 1;
                break;
            }
        }
        for (int index = start; index < frames.size(); index++) {
            TemplateTrace.Frame frame = frames.get(index);
            if (frame.kind() != TemplateTrace.Kind.EXTENDS) {
                continue;
            }
            templates.add(frame.template());
            if (index < frames.size() - 1 && resolvedTarget.equals(frame.template())) {
                cycle = true;
            }
        }
        if (!cycle) {
            return;
        }

        chain.addAll(templates);
        chain.add(resolvedTarget);
        throw new PebbleException(
                null,
                "Extends cycle: " + String.join(" -> ", chain),
                getLineNumber(),
                self.getName());
    }
}
