package io.github.allbegray.pebbleforge.include;

import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.expression.MapExpression;

/**
 * Read-only view of an include node.
 */
public interface IncludeNodeView {

    /**
     * Returns the include target expression, which may be a literal or dynamic name.
     */
    Expression<?> getTargetExpression();

    /**
     * Returns the {@code with} map, or {@code null} when the include has none.
     */
    MapExpression getWithExpression();

    /**
     * Returns the line of the include tag.
     */
    int getLineNumber();
}
