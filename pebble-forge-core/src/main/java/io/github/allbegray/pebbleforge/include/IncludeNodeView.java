package io.github.allbegray.pebbleforge.include;

import io.pebbletemplates.pebble.node.expression.Expression;

import java.util.Map;

/**
 * Read-only view of an include node's parsed arguments.
 */
public interface IncludeNodeView {

    /**
     * Returns the include target expression, which may be a literal or dynamic name.
     */
    Expression<?> getTargetExpression();

    /**
     * Returns an immutable copy of the {@code with} arguments, or {@code null}
     * when the include has none.
     */
    Map<Expression<?>, Expression<?>> getWithArguments();

    /**
     * Returns the line of the include tag.
     */
    int getLineNumber();
}
