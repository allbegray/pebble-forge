package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.node.BlockNode;
import io.pebbletemplates.pebble.template.Block;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.io.IOException;
import java.io.Writer;

final class PolicyAwareBlockNode extends BlockNode {

    private final String owner;

    PolicyAwareBlockNode(BlockNode delegate, String owner) {
        super(delegate.getLineNumber(), delegate.getName(), delegate.getBody());
        this.owner = owner;
    }

    @Override
    public Block getBlock() {
        Block delegate = super.getBlock();
        return new Block() {

            @Override
            public String getName() {
                return delegate.getName();
            }

            @Override
            public void evaluate(PebbleTemplateImpl self, Writer writer,
                    EvaluationContextImpl context) throws IOException {
                PolicyContext.runWith(
                        owner, () -> delegate.evaluate(self, writer, context));
            }
        };
    }
}
