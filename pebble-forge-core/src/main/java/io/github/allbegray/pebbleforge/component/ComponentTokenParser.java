package io.github.allbegray.pebbleforge.component;

import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.expression.MapExpression;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

/**
 * Parses {% component "name" with { ... } %} ... {% endcomponent %}.
 */
public final class ComponentTokenParser implements TokenParser {

    private final Components components;

    public ComponentTokenParser(Components components) {
        this.components = components;
    }

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        TokenStream stream = parser.getStream();
        int lineNumber = token.getLineNumber();

        stream.next();

        Expression<?> nameExpression = parser.getExpressionParser().parseExpression();

        MapExpression propsExpression = null;
        if (stream.current().test(Token.Type.NAME, "with")) {
            stream.next();
            Expression<?> expression = parser.getExpressionParser().parseExpression();
            if (!(expression instanceof MapExpression)) {
                throw new ComponentException(
                        "The 'with' argument of a component must be a map",
                        stream.getFilename(),
                        lineNumber);
            }
            propsExpression = (MapExpression) expression;
        }

        stream.expect(Token.Type.EXECUTE_END);

        BodyNode body = parser.subparse(tkn -> tkn.test(Token.Type.NAME, "endcomponent"));

        if (!stream.current().test(Token.Type.NAME, "endcomponent")) {
            throw new ComponentException(
                    "Unexpected end of template. Pebble was looking for the \"endcomponent\" tag",
                    stream.getFilename(),
                    lineNumber);
        }

        stream.next();
        stream.expect(Token.Type.EXECUTE_END);

        return new ComponentNode(lineNumber, nameExpression, propsExpression, body, components);
    }

    @Override
    public String getTag() {
        return "component";
    }
}
