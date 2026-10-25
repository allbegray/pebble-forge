package io.github.allbegray.pebbleforge.component;

import io.github.allbegray.pebbleforge.sandbox.SandboxLimits;
import io.github.allbegray.pebbleforge.sandbox.SandboxRuntime;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimitsResolver;
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
    private final SandboxLimitsResolver limitsResolver;

    /**
     * Creates a component node without resource limits.
     *
     * @param lineNumber the line of the component tag
     * @param nameExpression the expression that produces the component name
     * @param propsExpression the props map, or {@code null}
     * @param body the component body
     * @param components the component configuration
     */
    public ComponentNode(
            int lineNumber,
            Expression<?> nameExpression,
            MapExpression propsExpression,
            BodyNode body,
            Components components) {
        this(lineNumber, nameExpression, propsExpression, body, components,
                SandboxLimitsResolver.fixed(SandboxLimits.unlimited()));
    }

    /**
     * Creates a component node.
     *
     * @param lineNumber the line of the component tag
     * @param nameExpression the expression that produces the component name
     * @param propsExpression the props map, or {@code null}
     * @param body the component body
     * @param components the component configuration
     * @param limitsResolver the resolver for per-template resource limits
     */
    public ComponentNode(
            int lineNumber,
            Expression<?> nameExpression,
            MapExpression propsExpression,
            BodyNode body,
            Components components,
            SandboxLimitsResolver limitsResolver) {
        super(lineNumber);
        this.nameExpression = nameExpression;
        this.propsExpression = propsExpression;
        this.body = body;
        this.components = components;
        this.limitsResolver = limitsResolver;
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
        visitor.visit(this);
        nameExpression.accept(visitor);
        if (propsExpression != null) {
            propsExpression.accept(visitor);
        }
        body.accept(visitor);
    }

    /**
     * Returns the expression that produces the component name.
     *
     * @return the name expression
     */
    public Expression<?> getComponentNameExpression() {
        return nameExpression;
    }

    /**
     * Returns the props map expression.
     *
     * @return the props expression, or {@code null}
     */
    public MapExpression getPropsExpression() {
        return propsExpression;
    }

    /**
     * Returns the component body.
     *
     * @return the body
     */
    public BodyNode getBody() {
        return body;
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

        SandboxRuntime.runWith(componentPath,
                () -> self.includeTemplate(writer, context, componentPath, scope));
    }

    private Map<String, Object> captureSlots(
            PebbleTemplateImpl self, EvaluationContextImpl context) throws IOException {
        SlotBudget budget = new SlotBudget(slotLimit(self, context));
        boolean hasExplicitSlots = body.getChildren().stream().anyMatch(SlotNode.class::isInstance);
        if (!hasExplicitSlots) {
            Map<String, Object> slots = new LinkedHashMap<>();
            slots.put("body", new SafeString(renderBody(self, context, body, budget)));
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
                slots.put(slotName, new SafeString(renderBody(self, context, slotNode.getBody(),
                        budget)));
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

    private int slotLimit(PebbleTemplateImpl self, EvaluationContextImpl context) {
        int outputLimit = context.getMaxRenderedSize();
        SandboxLimits limits = limitsResolver.resolve(SandboxRuntime.currentTemplateName(self));
        int componentLimit = components.maxSlotChars();
        int templateLimit = limits.maxSlotChars();
        if (componentLimit < 0) {
            componentLimit = templateLimit;
        } else if (templateLimit >= 0) {
            componentLimit = Math.min(componentLimit, templateLimit);
        }
        if (componentLimit < 0) {
            return outputLimit;
        }
        if (outputLimit < 0) {
            return componentLimit;
        }
        return Math.min(outputLimit, componentLimit);
    }

    private String renderBody(PebbleTemplateImpl self, EvaluationContextImpl context, BodyNode node,
            SlotBudget budget) throws IOException {
        StringWriter out = new StringWriter();
        Writer writer = budget.getMaxRenderedSize() < 0
                ? out
                : LimitedSizeWriter.from(out, budget);
        node.render(self, writer, context);
        return out.toString();
    }

    private static final class SlotBudget implements RenderedSizeContext {

        private final int max;
        private final AtomicInteger written = new AtomicInteger();

        private SlotBudget(int max) {
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
