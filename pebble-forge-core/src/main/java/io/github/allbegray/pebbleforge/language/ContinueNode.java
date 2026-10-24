package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.io.Writer;

final class ContinueNode extends AbstractRenderableNode {

    ContinueNode(int lineNumber) {
        super(lineNumber);
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context) {
        throw ContinueSignal.INSTANCE;
    }

    @Override
    public void accept(NodeVisitor visitor) {
    }
}
