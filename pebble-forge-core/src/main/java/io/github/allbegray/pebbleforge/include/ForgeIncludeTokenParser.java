package io.github.allbegray.pebbleforge.include;

import io.pebbletemplates.pebble.error.ParserException;
import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.expression.MapExpression;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

final class ForgeIncludeTokenParser implements TokenParser {

    private final int maxDepth;

    ForgeIncludeTokenParser(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        TokenStream stream = parser.getStream();
        int lineNumber = token.getLineNumber();

        stream.next();
        Expression<?> includeExpression = parser.getExpressionParser().parseExpression();

        MapExpression mapExpression = null;
        Token current = stream.current();
        if (current.getType().equals(Token.Type.NAME) && current.getValue().equals("with")) {
            stream.next();
            Expression<?> parsedExpression = parser.getExpressionParser().parseExpression();
            if (parsedExpression instanceof MapExpression map) {
                mapExpression = map;
            } else {
                throw new ParserException(
                        null,
                        "The include with argument must be a map expression",
                        token.getLineNumber(),
                        stream.getFilename());
            }
        }

        stream.expect(Token.Type.EXECUTE_END);
        return new ForgeIncludeNode(lineNumber, includeExpression, mapExpression, maxDepth);
    }

    @Override
    public String getTag() {
        return "include";
    }
}
