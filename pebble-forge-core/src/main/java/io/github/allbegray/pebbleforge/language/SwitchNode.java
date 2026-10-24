package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import io.pebbletemplates.pebble.utils.OperatorUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Renders the first matching case and does not fall through.
 */
public final class SwitchNode extends AbstractRenderableNode {

    private final Expression<?> subject;
    private final List<Case> cases;
    private final BodyNode defaultBody;

    SwitchNode(int lineNumber, Expression<?> subject, List<Case> cases, BodyNode defaultBody) {
        super(lineNumber);
        this.subject = subject;
        this.cases = List.copyOf(cases);
        this.defaultBody = defaultBody;
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context)
            throws IOException {
        Object subjectValue = subject.evaluate(self, context);
        for (Case entry : cases) {
            for (Expression<?> value : entry.values()) {
                Object caseValue = value.evaluate(self, context);
                if (OperatorUtils.equals(subjectValue, caseValue)) {
                    renderBody(entry.body(), self, writer, context);
                    return;
                }
            }
        }
        if (defaultBody != null) {
            renderBody(defaultBody, self, writer, context);
        }
    }

    private static void renderBody(BodyNode body, PebbleTemplateImpl self, Writer writer,
            EvaluationContextImpl context) throws IOException {
        try {
            body.render(self, writer, context);
        } catch (BreakSignal ignored) {
        }
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
        subject.accept(visitor);
        for (Case entry : cases) {
            for (Expression<?> value : entry.values()) {
                value.accept(visitor);
            }
            entry.body().accept(visitor);
        }
        if (defaultBody != null) {
            defaultBody.accept(visitor);
        }
    }

    public Expression<?> getSubject() {
        return subject;
    }

    public List<Case> getCases() {
        return cases;
    }

    public BodyNode getDefaultBody() {
        return defaultBody;
    }

    public record Case(List<Expression<?>> values, BodyNode body) {

        public Case {
            values = List.copyOf(values);
        }
    }
}
