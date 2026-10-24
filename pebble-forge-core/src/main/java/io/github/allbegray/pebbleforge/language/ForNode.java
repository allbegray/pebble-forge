package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.fornode.LazyLength;
import io.pebbletemplates.pebble.node.fornode.LazyRevIndex;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import io.pebbletemplates.pebble.template.ScopeChain;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * A for loop that understands break and continue signals.
 */
public final class ForNode extends AbstractRenderableNode {

    private final String variableName;
    private final Expression<?> iterableExpression;
    private final BodyNode body;
    private final BodyNode elseBody;

    ForNode(int lineNumber, String variableName, Expression<?> iterableExpression,
            BodyNode body, BodyNode elseBody) {
        super(lineNumber);
        this.variableName = variableName;
        this.iterableExpression = iterableExpression;
        this.body = body;
        this.elseBody = elseBody;
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context)
            throws IOException {
        Object iterableValue = iterableExpression.evaluate(self, context);
        if (iterableValue == null) {
            return;
        }

        Iterable<?> iterable = toIterable(iterableValue);
        if (iterable == null) {
            throw new PebbleException(
                    null,
                    "Not an iterable object. Value = [" + iterableValue + "]",
                    getLineNumber(),
                    self.getName());
        }

        Iterator<?> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            if (elseBody != null) {
                elseBody.render(self, writer, context);
            }
            return;
        }

        ScopeChain scope = context.getScopeChain();
        scope.pushScope();
        try {
            LazyLength length = new LazyLength(iterableValue);
            boolean parallel = context.getExecutorService() != null;
            LoopVariables loop = null;
            int index = 0;

            while (iterator.hasNext()) {
                if (index == 0 || parallel) {
                    loop = new LoopVariables();
                    loop.first = index == 0;
                    loop.last = !iterator.hasNext();
                    loop.length = length;
                } else if (index == 1) {
                    loop.first = false;
                }

                loop.revindex = new LazyRevIndex(index, length);
                loop.index = index++;
                scope.put("loop", loop);
                scope.put(variableName, iterator.next());

                if (!iterator.hasNext()) {
                    loop.last = true;
                }

                try {
                    body.render(self, writer, context);
                } catch (ContinueSignal ignored) {
                } catch (BreakSignal ignored) {
                    break;
                }
            }
        } finally {
            scope.popScope();
        }
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
        iterableExpression.accept(visitor);
        body.accept(visitor);
        if (elseBody != null) {
            elseBody.accept(visitor);
        }
    }

    public String getIterationVariable() {
        return variableName;
    }

    public Expression<?> getIterable() {
        return iterableExpression;
    }

    public BodyNode getBody() {
        return body;
    }

    public BodyNode getElseBody() {
        return elseBody;
    }

    private static Iterable<?> toIterable(Object value) {
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet();
        }
        if (value.getClass().isArray()) {
            return new ArrayIterable(value);
        }
        if (value instanceof Enumeration<?> enumeration) {
            return new EnumerationIterable(enumeration);
        }
        return null;
    }

    public static final class LoopVariables {

        private boolean first;
        private boolean last;
        private LazyLength length;
        private int index;
        private LazyRevIndex revindex;

        public boolean isFirst() {
            return first;
        }

        public boolean isLast() {
            return last;
        }

        public LazyLength getLength() {
            return length;
        }

        public int getIndex() {
            return index;
        }

        public LazyRevIndex getRevindex() {
            return revindex;
        }

        @Override
        public String toString() {
            return "{last=" + last + ", length=" + length + ", index=" + index
                    + ", revindex=" + revindex + ", first=" + first + "}";
        }
    }

    private static final class ArrayIterable implements Iterable<Object> {

        private final Object array;

        private ArrayIterable(Object array) {
            this.array = array;
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<>() {

                private int index;

                @Override
                public boolean hasNext() {
                    return index < Array.getLength(array);
                }

                @Override
                public Object next() {
                    return Array.get(array, index++);
                }
            };
        }
    }

    private static final class EnumerationIterable implements Iterable<Object> {

        private final Enumeration<?> enumeration;

        private EnumerationIterable(Enumeration<?> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return enumeration.hasMoreElements();
                }

                @Override
                public Object next() {
                    return enumeration.nextElement();
                }
            };
        }
    }
}
