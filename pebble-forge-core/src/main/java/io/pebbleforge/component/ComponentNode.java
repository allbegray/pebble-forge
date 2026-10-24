package io.pebbleforge.component;

import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.TextNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.expression.MapExpression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import io.pebbletemplates.pebble.template.RenderedSizeContext;
import io.pebbletemplates.pebble.utils.LimitedSizeWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Renders a component template with evaluated props and captured slots.
 */
public final class ComponentNode extends AbstractRenderableNode {

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private final Expression<?> nameExpression;
    private final MapExpression propsExpression;
    private final BodyNode body;
    private final Components components;

    public ComponentNode(
            int lineNumber,
            Expression<?> nameExpression,
            MapExpression propsExpression,
            BodyNode body,
            Components components) {
        super(lineNumber);
        this.nameExpression = nameExpression;
        this.propsExpression = propsExpression;
        this.body = body;
        this.components = components;
    }

    @Override
    public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context)
            throws IOException {
        int depth = DEPTH.get();
        if (depth >= components.maxDepth()) {
            throw new ComponentException(
                    "Component depth limit of " + components.maxDepth() + " reached",
                    self.getName(),
                    getLineNumber());
        }
        DEPTH.set(depth + 1);
        try {
            renderComponent(self, writer, context);
        } finally {
            if (depth == 0) {
                DEPTH.remove();
            } else {
                DEPTH.set(depth);
            }
        }
    }

    @Override
    public void accept(NodeVisitor visitor) {
        nameExpression.accept(visitor);
        if (propsExpression != null) {
            propsExpression.accept(visitor);
        }
        body.accept(visitor);
    }

    private void renderComponent(
            PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context) throws IOException {
        Object evaluatedName = nameExpression.evaluate(self, context);
        if (evaluatedName == null) {
            throw new ComponentException(
                    "Component name evaluated to null", self.getName(), getLineNumber());
        }
        String componentName = String.valueOf(evaluatedName);
        String componentPath;
        try {
            componentPath = components.resolvePath(componentName);
        } catch (IllegalArgumentException e) {
            throw new ComponentException(
                    e.getMessage(), self.getName(), getLineNumber());
        }

        Map<String, Object> scope = new LinkedHashMap<>();
        Map<?, ?> props = propsExpression == null
                ? Map.of()
                : propsExpression.evaluate(self, context);
        for (Map.Entry<?, ?> entry : props.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new ComponentException(
                        "Component prop names must be strings, got: " + entry.getKey(),
                        self.getName(),
                        getLineNumber());
            }
            scope.put(key, entry.getValue());
        }

        Map<String, Object> slots = captureSlots(self, context);
        scope.put("__slots", slots);
        scope.put("__component", componentPath);

        self.includeTemplate(writer, context, componentPath, scope);
    }

    private Map<String, Object> captureSlots(
            PebbleTemplateImpl self, EvaluationContextImpl context) throws IOException {
        boolean hasExplicitSlots = body.getChildren().stream().anyMatch(SlotNode.class::isInstance);
        if (!hasExplicitSlots) {
            Map<String, Object> slots = new LinkedHashMap<>();
            slots.put("body", new SafeString(renderBody(self, context, body)));
            return slots;
        }

        Map<String, Object> slots = new LinkedHashMap<>();
        for (var child : body.getChildren()) {
            if (child instanceof SlotNode slotNode) {
                String slotName = slotNode.resolveName(self, context);
                if (slots.containsKey(slotName)) {
                    throw new ComponentException(
                            "Duplicate slot name [" + slotName + "]",
                            self.getName(),
                            slotNode.getLineNumber());
                }
                slots.put(slotName, new SafeString(renderBody(self, context, slotNode.getBody())));
            } else if (child instanceof TextNode textNode) {
                String text = new String(textNode.getData());
                if (!text.isBlank()) {
                    throw new ComponentException(
                            "A component body with named slots can only contain slot tags and whitespace",
                            self.getName(),
                            textNode.getLineNumber());
                }
            } else {
                throw new ComponentException(
                        "A component body with named slots can only contain slot tags and whitespace",
                        self.getName(),
                        getLineNumber());
            }
        }
        return slots;
    }

    private String renderBody(
            PebbleTemplateImpl self, EvaluationContextImpl context, BodyNode node)
            throws IOException {
        StringWriter out = new StringWriter();
        Writer writer = context.getMaxRenderedSize() < 0
                ? out
                : LimitedSizeWriter.from(out, new SizeCap(context.getMaxRenderedSize()));
        node.render(self, writer, context);
        return out.toString();
    }

    private static final class SizeCap implements RenderedSizeContext {

        private final int max;
        private final AtomicInteger written = new AtomicInteger();

        private SizeCap(int max) {
            this.max = max;
        }

        @Override
        public int getMaxRenderedSize() {
            return max;
        }

        @Override
        public int addAndGet(int delta) {
            return written.addAndGet(delta);
        }
    }
}
