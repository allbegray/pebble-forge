package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.component.ComponentNode;
import io.github.allbegray.pebbleforge.component.PropsNode;
import io.github.allbegray.pebbleforge.include.IncludeNodeView;
import io.github.allbegray.pebbleforge.language.ForNode;
import io.github.allbegray.pebbleforge.language.SwitchNode;
import io.github.allbegray.pebbleforge.language.WithNode;
import io.pebbletemplates.pebble.extension.AbstractNodeVisitor;
import io.pebbletemplates.pebble.node.ArgumentsNode;
import io.pebbletemplates.pebble.node.BlockNode;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.ExtendsNode;
import io.pebbletemplates.pebble.node.IfNode;
import io.pebbletemplates.pebble.node.MacroNode;
import io.pebbletemplates.pebble.node.NamedArgumentNode;
import io.pebbletemplates.pebble.node.Node;
import io.pebbletemplates.pebble.node.PositionalArgumentNode;
import io.pebbletemplates.pebble.node.SetNode;
import io.pebbletemplates.pebble.node.TestInvocationExpression;
import io.pebbletemplates.pebble.node.expression.ArrayExpression;
import io.pebbletemplates.pebble.node.expression.BinaryExpression;
import io.pebbletemplates.pebble.node.expression.BlockFunctionExpression;
import io.pebbletemplates.pebble.node.expression.ContextVariableExpression;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.expression.FilterExpression;
import io.pebbletemplates.pebble.node.expression.FilterInvocationExpression;
import io.pebbletemplates.pebble.node.expression.FunctionOrMacroInvocationExpression;
import io.pebbletemplates.pebble.node.expression.GetAttributeExpression;
import io.pebbletemplates.pebble.node.expression.LiteralStringExpression;
import io.pebbletemplates.pebble.node.expression.MapExpression;
import io.pebbletemplates.pebble.node.expression.TernaryExpression;
import io.pebbletemplates.pebble.node.expression.UnaryExpression;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import io.pebbletemplates.pebble.utils.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ContractVisitor extends AbstractNodeVisitor {

    private final TemplateScan scan;
    private final Set<Node> consumed = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Deque<ScopeFrame> scopes = new ArrayDeque<>();
    private final Deque<FlowFrame> flows = new ArrayDeque<>();
    private final Deque<String> blocks = new ArrayDeque<>();

    ContractVisitor(TemplateScan scan, PebbleTemplateImpl template) {
        super(template);
        this.scan = scan;
        scopes.addLast(new ScopeFrame(true));
        flows.addLast(new FlowFrame(true, false));
    }

    @Override
    public void visit(SetNode node) {
        scan(node.getValue());
        declare(node.getName());
    }

    @Override
    public void visit(MacroNode node) {
        scanArguments(node.getArgs());
        consumed.add(node.getArgs());

        scopes.addLast(new ScopeFrame(true));
        flows.addLast(new FlowFrame(false, true));
        for (String argument : node.getMacro().getArgumentNames()) {
            declare(argument);
        }
        scanBody(node.getBody());
        flows.removeLast();
        scopes.removeLast();
        consumed.add(node.getBody());
    }

    @Override
    public void visit(BodyNode node) {
        if (consumed.remove(node)) {
            return;
        }
        super.visit(node);
    }

    @Override
    public void visit(BlockNode node) {
        scan.blockDeclarations.computeIfAbsent(node.getName(),
                name -> new LinkedHashSet<>());
        scan.blockLines.put(node.getName(), node.getLineNumber());
        FlowFrame outer = flows.getLast();
        Set<String> outerNames = Set.copyOf(outer.names);
        blocks.addLast(node.getName());
        FlowFrame frame = new FlowFrame(outer.templateLevel, outer.closed, outer.names);
        flows.addLast(frame);
        scanBody(node.getBody());
        flows.removeLast();
        blocks.removeLast();
        consumed.add(node.getBody());
        if (frame.templateLevel) {
            Set<String> declared = new LinkedHashSet<>(frame.names);
            declared.removeAll(outerNames);
            scan.blockDeclarations.get(node.getName()).addAll(declared);
        }
    }

    @Override
    public void visit(IfNode node) {
        FlowFrame entry = flows.getLast();
        List<Set<String>> exits = new ArrayList<>();
        for (Pair<Expression<?>, BodyNode> branch : node.getConditionsWithBodies()) {
            scan(branch.getLeft());
            pushBranch(entry);
            scanBody(branch.getRight());
            exits.add(Set.copyOf(flows.getLast().names));
            popBranch();
        }
        if (node.getElseBody() != null) {
            pushBranch(entry);
            scanBody(node.getElseBody());
            exits.add(Set.copyOf(flows.getLast().names));
            popBranch();
        } else {
            exits.add(Set.copyOf(entry.names));
        }
        joinBranches(exits);
    }

    @Override
    public void visit(ExtendsNode node) {
        Expression<?> parent = node.getParentExpression();
        String literal = parent instanceof LiteralStringExpression expression
                ? expression.getValue()
                : null;
        String resolved = null;
        String resolutionError = null;
        if (literal != null) {
            try {
                resolved = getTemplate().resolveRelativePath(literal);
            } catch (RuntimeException error) {
                resolutionError = PebbleErrors.messageOf(error);
            }
            if (resolved == null) {
                resolved = literal;
            }
        }
        scan.parent = new TemplateScan.ParentCall(literal, resolved, resolutionError,
                literal == null, node.getLineNumber());
        super.visit(node);
    }

    @Override
    public void visit(Node node) {
        if (consumed.remove(node)) {
            return;
        }
        if (node instanceof SwitchNode switchNode) {
            scanSwitch(switchNode);
            return;
        }
        if (node instanceof IncludeNodeView include) {
            recordInclude(include);
            return;
        }
        if (node instanceof ContextVariableExpression variable) {
            FlowFrame flow = flows.getLast();
            scan.reads.add(new TemplateScan.VariableRead(variable.getName(),
                    variable.getLineNumber(), currentBlock(), Set.copyOf(flow.names),
                    flow.closed, currentComplete()));
            return;
        }
        if (node instanceof GetAttributeExpression attribute) {
            recordAttributeRead(attribute);
            return;
        }
        if (node instanceof FilterExpression filter) {
            scan(filter.getLeftExpression());
            scan(filter.getRightExpression());
            return;
        }
        if (node instanceof FunctionOrMacroInvocationExpression function) {
            scanArguments(function.getArguments());
            return;
        }
        if (node instanceof FilterInvocationExpression filter) {
            scanArguments(filter.getArgs());
            return;
        }
        if (node instanceof BinaryExpression<?> binary) {
            scan(binary.getLeftExpression());
            scan(binary.getRightExpression());
            return;
        }
        if (node instanceof UnaryExpression unary) {
            scan(unary.getChildExpression());
            return;
        }
        if (node instanceof TernaryExpression ternary) {
            scan(ternary.getExpression1());
            scan(ternary.getExpression2());
            scan(ternary.getExpression3());
            return;
        }
        if (node instanceof MapExpression map) {
            for (var entry : map.getEntries().entrySet()) {
                scan(entry.getKey());
                scan(entry.getValue());
            }
            return;
        }
        if (node instanceof ArrayExpression array) {
            for (Expression<?> value : array.getValues()) {
                scan(value);
            }
            return;
        }
        if (node instanceof TestInvocationExpression test) {
            scanArguments(test.getArgs());
            return;
        }
        if (node instanceof BlockFunctionExpression block) {
            scan(block.getBlockNameExpression());
            return;
        }
        if (node instanceof ComponentNode component) {
            recordComponent(component);
            return;
        }
        if (node instanceof PropsNode props) {
            scan.declareProps(props.getProps());
            for (PropsNode.Prop prop : props.getProps()) {
                declare(prop.name());
            }
            return;
        }
        if (node instanceof ForNode forNode) {
            scanFor(forNode);
            return;
        }
        if (node instanceof WithNode with) {
            scanWith(with);
            return;
        }
    }

    private void scanFor(ForNode node) {
        scan(node.getIterable());
        consumed.add(node.getIterable());

        FlowFrame outer = flows.getLast();
        flows.addLast(new FlowFrame(false, outer.closed, outer.names));
        scopes.addLast(new ScopeFrame(false));
        if (node.getKeyVariableName() != null) {
            declare(node.getKeyVariableName());
        }
        declare(node.getIterationVariable());
        declare("loop");
        scanBody(node.getBody());
        scopes.removeLast();
        flows.removeLast();
        consumed.add(node.getBody());

        if (node.getElseBody() != null) {
            flows.addLast(new FlowFrame(false, outer.closed, outer.names));
            scopes.addLast(new ScopeFrame(false));
            scanBody(node.getElseBody());
            scopes.removeLast();
            flows.removeLast();
            consumed.add(node.getElseBody());
        }
    }

    private void scanWith(WithNode with) {
        Expression<?> values = with.getValuesExpression();
        scan(values);
        if (values != null) {
            consumed.add(values);
        }

        FlowFrame outer = flows.getLast();
        Set<String> keys = new LinkedHashSet<>();
        boolean dynamicKeys = false;
        if (values instanceof MapExpression map) {
            for (Expression<?> key : map.getEntries().keySet()) {
                if (key instanceof LiteralStringExpression literal) {
                    keys.add(literal.getValue());
                } else {
                    dynamicKeys = true;
                }
            }
        } else if (values != null) {
            dynamicKeys = true;
        }

        FlowFrame frame = with.isIsolated()
                ? new FlowFrame(false, true)
                : new FlowFrame(false, outer.closed, outer.names);
        flows.addLast(frame);
        scopes.addLast(new ScopeFrame(with.isIsolated()));
        if (dynamicKeys) {
            scopes.getLast().complete = false;
        }
        for (String key : keys) {
            declare(key);
        }
        scanBody(with.getBody());
        scopes.removeLast();
        flows.removeLast();
        consumed.add(with.getBody());
    }

    private void scanSwitch(SwitchNode node) {
        FlowFrame entry = flows.getLast();
        scan(node.getSubject());
        consumed.add(node.getSubject());

        List<Set<String>> exits = new ArrayList<>();
        for (SwitchNode.Case entryCase : node.getCases()) {
            for (Expression<?> value : entryCase.values()) {
                scan(value);
                consumed.add(value);
            }
            pushBranch(entry);
            scanBody(entryCase.body());
            consumed.add(entryCase.body());
            exits.add(Set.copyOf(flows.getLast().names));
            popBranch();
        }
        if (node.getDefaultBody() != null) {
            pushBranch(entry);
            scanBody(node.getDefaultBody());
            consumed.add(node.getDefaultBody());
            exits.add(Set.copyOf(flows.getLast().names));
            popBranch();
        } else {
            exits.add(Set.copyOf(entry.names));
        }
        joinBranches(exits);
    }

    private void pushBranch(FlowFrame entry) {
        flows.addLast(new FlowFrame(entry.templateLevel, entry.closed, entry.names));
        scopes.addLast(new ScopeFrame(false));
    }

    private void popBranch() {
        scopes.removeLast();
        flows.removeLast();
    }

    private void joinBranches(List<Set<String>> exits) {
        if (exits.isEmpty()) {
            return;
        }
        Set<String> joined = new LinkedHashSet<>(exits.get(0));
        for (int index = 1; index < exits.size(); index++) {
            joined.retainAll(exits.get(index));
        }
        flows.getLast().names.clear();
        flows.getLast().names.addAll(joined);
        scopes.getLast().names.addAll(joined);
    }

    private void recordAttributeRead(GetAttributeExpression outermost) {
        List<GetAttributeExpression> chain = new ArrayList<>();
        Expression<?> current = outermost;
        while (current instanceof GetAttributeExpression attribute) {
            chain.add(attribute);
            current = attribute.getNode();
        }

        boolean complete = true;
        List<String> path = new ArrayList<>();
        for (int index = chain.size() - 1; index >= 0; index--) {
            Expression<?> name = chain.get(index).getAttributeNameExpression();
            if (name instanceof LiteralStringExpression literal) {
                path.add(literal.getValue());
            } else {
                complete = false;
            }
        }

        if (current instanceof ContextVariableExpression root) {
            FlowFrame flow = flows.getLast();
            scan.attributeReads.add(new TemplateScan.AttributeRead(root.getName(), path,
                    outermost.getLineNumber(), currentBlock(), complete,
                    Set.copyOf(flow.names), flow.closed));
        }
        for (GetAttributeExpression attribute : chain) {
            scan(attribute.getAttributeNameExpression());
            scanArguments(attribute.getArgumentsNode());
        }
        scan(current);
    }

    private void recordInclude(IncludeNodeView include) {
        Expression<?> target = include.getTargetExpression();
        String literal = target instanceof LiteralStringExpression expression
                ? expression.getValue()
                : null;

        String resolved = null;
        String resolutionError = null;
        if (literal != null) {
            try {
                resolved = getTemplate().resolveRelativePath(literal);
            } catch (RuntimeException error) {
                resolutionError = PebbleErrors.messageOf(error);
            }
            if (resolved == null) {
                resolved = literal;
            }
        }

        Set<String> visibleAtCall = Set.copyOf(currentVisibleNames());
        boolean scopeIncomplete = !currentComplete();
        Set<String> withKeys = new LinkedHashSet<>();
        boolean dynamicWithKeys = false;

        Map<Expression<?>, Expression<?>> with = include.getWithArguments();
        if (with != null) {
            for (Map.Entry<Expression<?>, Expression<?>> entry : with.entrySet()) {
                if (entry.getKey() instanceof LiteralStringExpression key) {
                    withKeys.add(key.getValue());
                } else {
                    dynamicWithKeys = true;
                }
            }
        }

        TemplateScan.IncludeTarget includeTarget = literal == null
                ? new TemplateScan.DynamicTarget()
                : new TemplateScan.StaticTarget(resolved);

        scan.includes.add(new TemplateScan.IncludeEdge(includeTarget, literal,
                resolutionError, withKeys, dynamicWithKeys, visibleAtCall,
                scopeIncomplete, include.getLineNumber(), currentBlock()));
    }

    private void recordComponent(ComponentNode component) {
        Expression<?> nameExpression = component.getComponentNameExpression();
        String staticName = nameExpression instanceof LiteralStringExpression literal
                ? literal.getValue()
                : null;
        Set<String> props = new LinkedHashSet<>();
        boolean dynamicProps = false;
        MapExpression propsExpression = component.getPropsExpression();
        if (propsExpression != null) {
            for (Expression<?> key : propsExpression.getEntries().keySet()) {
                if (key instanceof LiteralStringExpression literal) {
                    props.add(literal.getValue());
                } else {
                    dynamicProps = true;
                }
            }
        }
        scan.componentCalls.add(new TemplateScan.ComponentCall(staticName, Set.copyOf(props),
                staticName == null, dynamicProps, component.getLineNumber(), currentBlock()));
    }

    private void declare(String name) {
        flows.getLast().names.add(name);
        scopes.getLast().names.add(name);
    }

    /**
     * Publishes the final root flow names as this template's cross-template
     * summary. Called once after the root node is accepted.
     */
    void finish() {
        scan.rootDeclarations.clear();
        scan.rootDeclarations.addAll(flows.getLast().names);
    }

    private String currentBlock() {
        return blocks.isEmpty() ? null : blocks.getLast();
    }

    private Set<String> currentVisibleNames() {
        Set<String> names = new LinkedHashSet<>();
        for (ScopeFrame frame : scopes) {
            if (frame.reset) {
                names.clear();
            }
            names.addAll(frame.names);
        }
        return names;
    }

    private boolean currentComplete() {
        boolean complete = true;
        for (ScopeFrame frame : scopes) {
            if (frame.reset) {
                complete = frame.complete;
            } else if (!frame.complete) {
                complete = false;
            }
        }
        return complete;
    }

    private void scanBody(BodyNode body) {
        if (body != null) {
            body.accept(this);
        }
    }

    private void scanArguments(ArgumentsNode arguments) {
        if (arguments == null) {
            return;
        }
        if (arguments.getNamedArgs() != null) {
            for (NamedArgumentNode argument : arguments.getNamedArgs()) {
                scan(argument.getValueExpression());
            }
        }
        if (arguments.getPositionalArgs() != null) {
            for (PositionalArgumentNode argument : arguments.getPositionalArgs()) {
                scan(argument.getValueExpression());
            }
        }
    }

    private void scan(Expression<?> expression) {
        if (expression != null) {
            expression.accept(this);
        }
    }

    static final class ScopeFrame {

        private final Set<String> names = new LinkedHashSet<>();
        private final boolean reset;
        private boolean complete = true;

        ScopeFrame(boolean reset) {
            this.reset = reset;
        }
    }

    static final class FlowFrame {

        private final Set<String> names = new LinkedHashSet<>();
        private final boolean templateLevel;
        private final boolean closed;

        FlowFrame(boolean templateLevel, boolean closed) {
            this.templateLevel = templateLevel;
            this.closed = closed;
        }

        FlowFrame(boolean templateLevel, boolean closed, Collection<String> visible) {
            this(templateLevel, closed);
            names.addAll(visible);
        }
    }
}
