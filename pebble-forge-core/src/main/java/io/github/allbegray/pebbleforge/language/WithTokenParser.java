package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.error.ParserException;
import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

/**
 * Parses with blocks that add scoped variables.
 */
final class WithTokenParser implements TokenParser {

    @Override
    public String getTag() {
        return "with";
    }

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        TokenStream stream = parser.getStream();
        int lineNumber = token.getLineNumber();

        stream.next();
        Expression<?> values = null;
        boolean isolated = false;
        if (stream.current().test(Token.Type.NAME, "only")) {
            stream.next();
            isolated = true;
        } else if (!stream.current().test(Token.Type.EXECUTE_END)) {
            values = parser.getExpressionParser().parseExpression();
            if (stream.current().test(Token.Type.NAME, "only")) {
                stream.next();
                isolated = true;
            }
            if (stream.current().test(Token.Type.PUNCTUATION, "=")) {
                throw new ParserException(
                        null,
                        "The with tag takes a map expression, for example { \"name\": value }",
                        lineNumber,
                        stream.getFilename());
            }
        }
        stream.expect(Token.Type.EXECUTE_END);

        BodyNode body = parser.subparse(tkn -> tkn.test(Token.Type.NAME, "endwith"));

        if (stream.current().getValue() == null) {
            throw new ParserException(
                    null,
                    "Unexpected end of template. Pebble was looking for the \"endwith\" tag",
                    stream.current().getLineNumber(),
                    stream.getFilename());
        }

        stream.next();
        stream.expect(Token.Type.EXECUTE_END);
        return new WithNode(lineNumber, values, body, isolated);
    }
}
