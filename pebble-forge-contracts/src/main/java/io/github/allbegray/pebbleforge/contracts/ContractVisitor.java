package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.component.ComponentNode;
import io.github.allbegray.pebbleforge.component.PropsNode;
import io.github.allbegray.pebbleforge.language.ForNode;
import io.github.allbegray.pebbleforge.language.WithNode;
import io.pebbletemplates.pebble.extension.AbstractNodeVisitor;
import io.pebbletemplates.pebble.node.ArgumentsNode;
import io.pebbletemplates.pebble.node.MacroNode;
import io.pebbletemplates.pebble.node.NamedArgumentNode;
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

import java.util.LinkedHashSet;
import java.util.Set;

final class ContractVisitor extends AbstractNodeVisitor {

    private final TemplateScan scan;
    private MapExpression withMap;

    ContractVisitor(TemplateScan scan, PebbleTemplateImpl template) {
        super(template);
        this.scan = scan;
    }

    @Override
    public void visit(SetNode node) {
        scan.declaredVariables.add(node.getName());
        super.visit(node);
    }

    @Override
    public void visit(MacroNode node) {
        scan.declaredVariables.addAll(node.getMacro().getArgumentNames());
        super.visit(node);
    }

    @Override
    public void visit(io.pebbletemplates.pebble.node.Node node) {
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
            if (map == withMap) {
                declareWithKeys(map);
                withMap = null;
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
            return;
        }
        if (node instanceof ForNode forNode) {
            if (forNode.getKeyVariableName() != null) {
                scan.declaredVariables.add(forNode.getKeyVariableName());
            }
            scan.declaredVariables.add(forNode.getIterationVariable());
            scan.declaredVariables.add("loop");
            return;
        }
        if (node instanceof WithNode with) {
            withMap = with.getValuesExpression() instanceof MapExpression map ? map : null;
            return;
        }
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

    private void declareWithKeys(MapExpression map) {
        for (Expression<?> key : map.getEntries().keySet()) {
            if (key instanceof LiteralStringExpression literal) {
                scan.declaredVariables.add(literal.getValue());
            }
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
}
