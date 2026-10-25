package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.component.ComponentNode;
import io.github.allbegray.pebbleforge.component.PropsNode;
import io.github.allbegray.pebbleforge.include.IncludeNodeView;
import io.github.allbegray.pebbleforge.language.ForNode;
import io.github.allbegray.pebbleforge.language.WithNode;
import io.pebbletemplates.pebble.extension.AbstractNodeVisitor;
import io.pebbletemplates.pebble.node.ArgumentsNode;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.ExtendsNode;
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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class ContractVisitor extends AbstractNodeVisitor {

    private final TemplateScan scan;
    private final Set<Node> consumed = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Deque<ScopeFrame> scopes = new ArrayDeque<>();

    ContractVisitor(TemplateScan scan, PebbleTemplateImpl template) {
        super(template);
        this.scan = scan;
        scopes.addLast(new ScopeFrame(true));
    }

    @Override
    public void visit(SetNode node) {
        declare(node.getName());
        super.visit(node);
    }

    @Override
    public void visit(MacroNode node) {
        scanArguments(node.getArgs());
        consumed.add(node.getArgs());

        scopes.addLast(new ScopeFrame(true));
        for (String argument : node.getMacro().getArgumentNames()) {
            declare(argument);
        }
        scanBody(node.getBody());
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
    public void visit(ExtendsNode node) {
        Expression<?> parent = node.getParentExpression();
        String literal = parent instanceof LiteralStringExpression expression
                ? expression.getValue()
                : null;
        String resolutionError = null;
        if (literal != null) {
            try {
                getTemplate().resolveRelativePath(literal);
            } catch (RuntimeException error) {
                resolutionError = PebbleErrors.messageOf(error);
            }
        }
        scan.parent = new TemplateScan.ParentCall(literal, resolutionError,
                literal == null, node.getLineNumber());
        super.visit(node);
    }

    @Override
    public void visit(Node node) {
        if (consumed.remove(node)) {
            return;
        }
        if (node instanceof IncludeNodeView include) {
            recordInclude(include);
            return;
        }
        if (node instanceof ContextVariableExpression variable) {
            scan.reads.add(new TemplateScan.VariableRead(variable.getName(),
                    variable.getLineNumber()));
            return;
        }
        if (node instanceof GetAttributeExpression attribute) {
            scan(attribute.getNode());
            scan(attribute.getAttributeNameExpression());
            scanArguments(attribute.getArgumentsNode());
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

        scopes.addLast(new ScopeFrame(false));
        if (node.getKeyVariableName() != null) {
            declare(node.getKeyVariableName());
        }
        declare(node.getIterationVariable());
        declare("loop");
        scanBody(node.getBody());
        scopes.removeLast();
        consumed.add(node.getBody());

        if (node.getElseBody() != null) {
            scanBody(node.getElseBody());
            consumed.add(node.getElseBody());
        }
    }

    private void scanWith(WithNode with) {
        Expression<?> values = with.getValuesExpression();
        scan(values);
        if (values != null) {
            consumed.add(values);
        }

        scopes.addLast(new ScopeFrame(with.isIsolated()));
        ScopeFrame frame = scopes.getLast();
        if (values instanceof MapExpression map) {
            for (Expression<?> key : map.getEntries().keySet()) {
                if (key instanceof LiteralStringExpression literal) {
                    declare(literal.getValue());
                } else {
                    frame.complete = false;
                }
            }
        } else if (values != null) {
            frame.complete = false;
        }
        scanBody(with.getBody());
        scopes.removeLast();
        consumed.add(with.getBody());
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
                scopeIncomplete, include.getLineNumber()));
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
                staticName == null, dynamicProps, component.getLineNumber()));
    }

    private void declare(String name) {
        scan.declaredVariables.add(name);
        scopes.getLast().names.add(name);
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
}
