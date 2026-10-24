package io.pebbleforge.component;

import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

/**
 * Parses {% slot "name" %} ... {% endslot %}.
 */
public final class SlotTokenParser implements TokenParser {

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        TokenStream stream = parser.getStream();
        int lineNumber = token.getLineNumber();

        stream.next();

        Expression<?> nameExpression;
        if (stream.current().test(Token.Type.EXECUTE_END)) {
            nameExpression = null;
        } else {
            nameExpression = parser.getExpressionParser().parseExpression();
        }

        stream.expect(Token.Type.EXECUTE_END);

        BodyNode body = parser.subparse(tkn -> tkn.test(Token.Type.NAME, "endslot"));

        if (!stream.current().test(Token.Type.NAME, "endslot")) {
            throw new ComponentException(
                    "Unexpected end of template. Pebble was looking for the \"endslot\" tag",
                    stream.getFilename(),
                    lineNumber);
        }

        stream.next();
        stream.expect(Token.Type.EXECUTE_END);

        return new SlotNode(lineNumber, nameExpression, body);
    }

    @Override
    public String getTag() {
        return "slot";
    }
}
