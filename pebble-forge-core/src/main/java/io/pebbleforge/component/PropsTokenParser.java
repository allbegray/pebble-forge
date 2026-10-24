package io.pebbleforge.component;

import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses {% props name, optional = "value" %}.
 */
public final class PropsTokenParser implements TokenParser {

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        TokenStream stream = parser.getStream();
        int lineNumber = token.getLineNumber();

        stream.next();

        List<PropsNode.Prop> props = new ArrayList<>();
        while (!stream.current().test(Token.Type.EXECUTE_END)) {
            String name = parser.getExpressionParser().parseNewVariableName();
            Expression<?> defaultValue = null;
            if (stream.current().test(Token.Type.PUNCTUATION, "=")) {
                stream.next();
                defaultValue = parser.getExpressionParser().parseExpression();
            }
            props.add(new PropsNode.Prop(name, defaultValue));

            if (stream.current().test(Token.Type.PUNCTUATION, ",")) {
                stream.next();
                continue;
            }
            break;
        }

        stream.expect(Token.Type.EXECUTE_END);

        return new PropsNode(lineNumber, props);
    }

    @Override
    public String getTag() {
        return "props";
    }
}
