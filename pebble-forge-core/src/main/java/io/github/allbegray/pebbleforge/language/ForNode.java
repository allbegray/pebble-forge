package io.github.allbegray.pebbleforge.language;

import io.github.allbegray.pebbleforge.sandbox.SandboxLimits;
import io.github.allbegray.pebbleforge.sandbox.SandboxRuntime;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimitsResolver;
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

    private final String keyVariableName;
    private final String valueVariableName;
    private final Expression<?> iterableExpression;
    private final BodyNode body;
    private final BodyNode elseBody;
    private final SandboxLimitsResolver limitsResolver;

    ForNode(int lineNumber, String keyVariableName, String valueVariableName,
            Expression<?> iterableExpression, BodyNode body, BodyNode elseBody,
            SandboxLimitsResolver limitsResolver) {
        super(lineNumber);
        this.keyVariableName = keyVariableName;
        this.valueVariableName = valueVariableName;
        this.iterableExpression = iterableExpression;
        this.body = body;
        this.elseBody = elseBody;
        this.limitsResolver = limitsResolver;
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
            int iterations = 0;
            SandboxLimits limits = limitsResolver.resolve(SandboxRuntime.currentTemplateName(self));
            int maxLoopIterations = limits.maxLoopIterations();
            int maxTotalIterations = limits.maxTotalIterations();
            IterationBudget.Counter budget = maxTotalIterations >= 0
                    ? IterationBudget.forScope(context.getScopeChain(), maxTotalIterations)
                    : null;

            while (iterator.hasNext()) {
                if (maxLoopIterations >= 0 && iterations >= maxLoopIterations) {
                    throw new PebbleException(
                            null,
                            "Loop iteration limit of " + maxLoopIterations + " reached",
                            getLineNumber(),
                            self.getName());
                }
                if (budget != null) {
                    budget.consume(self, getLineNumber());
                }
                iterations++;

                if (index == 0 || parallel) {
                    loop = new LoopVariables();
                    loop.first = index == 0;
                    loop.last = !iterator.hasNext();
                    loop.length = length;
                } else if (index == 1) {
                    loop.first = false;
                }

                int currentIndex = index;
                loop.revindex = new LazyRevIndex(currentIndex, length);
                loop.index = index++;
                scope.put("loop", loop);

                Object item = iterator.next();
                if (keyVariableName != null) {
                    if (item instanceof Map.Entry<?, ?> entry) {
                        scope.put(keyVariableName, entry.getKey());
                        scope.put(valueVariableName, entry.getValue());
                    } else {
                        scope.put(keyVariableName, currentIndex);
                        scope.put(valueVariableName, item);
                    }
                } else {
                    scope.put(valueVariableName, item);
                }

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

    /**
     * Returns the name of the loop value variable.
     *
     * @return the value variable name
     */
    public String getIterationVariable() {
        return valueVariableName;
    }

    /**
     * Returns the name of the loop key variable, or {@code null} when the loop has no key.
     *
     * @return the key variable name
     */
    public String getKeyVariableName() {
        return keyVariableName;
    }

    /**
     * Returns the expression that produces the iterable.
     *
     * @return the iterable expression
     */
    public Expression<?> getIterable() {
        return iterableExpression;
    }

    /**
     * Returns the loop body.
     *
     * @return the body
     */
    public BodyNode getBody() {
        return body;
    }

    /**
     * Returns the body rendered when the iterable is empty.
     *
     * @return the else body, or {@code null}
     */
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

    /**
     * The values exposed as {@code loop} inside a for body.
     */
    public static final class LoopVariables {

        /**
         * Creates empty loop variables. A for node fills them during rendering.
         */
        public LoopVariables() {
        }

        private boolean first;
        private boolean last;
        private LazyLength length;
        private int index;
        private LazyRevIndex revindex;

        /**
         * Returns whether this is the first iteration.
         *
         * @return true on the first iteration
         */
        public boolean isFirst() {
            return first;
        }

        /**
         * Returns whether this is the last iteration.
         *
         * @return true on the last iteration
         */
        public boolean isLast() {
            return last;
        }

        /**
         * Returns the number of items in the iterable.
         *
         * @return the length
         */
        public LazyLength getLength() {
            return length;
        }

        /**
         * Returns the zero-based iteration index.
         *
         * @return the index
         */
        public int getIndex() {
            return index;
        }

        /**
         * Returns the index counted from the end of the iterable.
         *
         * @return the reverse index
         */
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
