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
 * Parses the for tag and allows break and continue in its body.
 */
final class ForTokenParser implements TokenParser {

    private final int maxLoopIterations;

    ForTokenParser(int maxLoopIterations) {
        this.maxLoopIterations = maxLoopIterations;
    }

    @Override
    public String getTag() {
        return "for";
    }

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        TokenStream stream = parser.getStream();
        int lineNumber = token.getLineNumber();

        stream.next();
        String firstVariable = parser.getExpressionParser().parseNewVariableName();
        String keyVariable = null;
        String valueVariable = firstVariable;
        if (stream.current().test(Token.Type.PUNCTUATION, ",")) {
            stream.next();
            keyVariable = firstVariable;
            valueVariable = parser.getExpressionParser().parseNewVariableName();
        }
        stream.expect(Token.Type.NAME, "in");
        Expression<?> iterable = parser.getExpressionParser().parseExpression();
        stream.expect(Token.Type.EXECUTE_END);

        int previousLoopDepth = LanguageParserState.loopDepth();
        int previousBreakableDepth = LanguageParserState.breakableDepth();
        LanguageParserState.setLoopDepth(previousLoopDepth + 1);
        LanguageParserState.setBreakableDepth(previousBreakableDepth + 1);
        BodyNode body;
        try {
            body = parser.subparse(tkn -> tkn.test(Token.Type.NAME, "else", "endfor"));
        } finally {
            LanguageParserState.setLoopDepth(previousLoopDepth);
            LanguageParserState.setBreakableDepth(previousBreakableDepth);
        }

        BodyNode elseBody = null;
        if (stream.current().test(Token.Type.NAME, "else")) {
            stream.next();
            stream.expect(Token.Type.EXECUTE_END);
            elseBody = parser.subparse(tkn -> tkn.test(Token.Type.NAME, "endfor"));
        }

        if (stream.current().getValue() == null) {
            throw new ParserException(
                    null,
                    "Unexpected end of template. Pebble was looking for the \"endfor\" tag",
                    stream.current().getLineNumber(),
                    stream.getFilename());
        }

        stream.next();
        stream.expect(Token.Type.EXECUTE_END);
        return new ForNode(lineNumber, keyVariable, valueVariable, iterable, body, elseBody,
                maxLoopIterations);
    }
}
