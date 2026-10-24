package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.error.ParserException;
import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.node.TextNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses switch blocks with case and default branches.
 */
final class SwitchTokenParser implements TokenParser {

    @Override
    public String getTag() {
        return "switch";
    }

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        TokenStream stream = parser.getStream();
        int lineNumber = token.getLineNumber();

        stream.next();
        Expression<?> subject = parser.getExpressionParser().parseExpression();
        stream.expect(Token.Type.EXECUTE_END);

        BodyNode preamble = parser.subparse(
                tkn -> tkn.test(Token.Type.NAME, "case", "default", "endswitch"));
        if (!isWhitespaceOnly(preamble)) {
            throw new ParserException(
                    null,
                    "A switch body must start with a case or default tag",
                    lineNumber,
                    stream.getFilename());
        }

        List<SwitchNode.Case> cases = new ArrayList<>();
        BodyNode defaultBody = null;
        boolean defaultSeen = false;

        while (true) {
            Token current = stream.current();
            if (current.getValue() == null) {
                throw new ParserException(
                        null,
                        "Unexpected end of template. Pebble was looking for the \"endswitch\" tag",
                        current.getLineNumber(),
                        stream.getFilename());
            }

            if (current.test(Token.Type.NAME, "endswitch")) {
                stream.next();
                stream.expect(Token.Type.EXECUTE_END);
                break;
            }

            if (current.test(Token.Type.NAME, "case")) {
                if (defaultSeen) {
                    throw new ParserException(
                            null,
                            "A case tag cannot appear after the default tag",
                            current.getLineNumber(),
                            stream.getFilename());
                }
                stream.next();
                List<Expression<?>> values = new ArrayList<>();
                values.add(parser.getExpressionParser().parseExpression());
                while (stream.current().test(Token.Type.PUNCTUATION, ",")) {
                    stream.next();
                    values.add(parser.getExpressionParser().parseExpression());
                }
                stream.expect(Token.Type.EXECUTE_END);

                int previousBreakableDepth = LanguageParserState.breakableDepth();
                LanguageParserState.setBreakableDepth(previousBreakableDepth + 1);
                BodyNode body;
                try {
                    body = parser.subparse(
                            tkn -> tkn.test(Token.Type.NAME, "case", "default", "endswitch"));
                } finally {
                    LanguageParserState.setBreakableDepth(previousBreakableDepth);
                }
                cases.add(new SwitchNode.Case(values, body));
                continue;
            }

            if (current.test(Token.Type.NAME, "default")) {
                if (defaultSeen) {
                    throw new ParserException(
                            null,
                            "A switch can only contain one default tag",
                            current.getLineNumber(),
                            stream.getFilename());
                }
                defaultSeen = true;
                stream.next();
                stream.expect(Token.Type.EXECUTE_END);

                int previousBreakableDepth = LanguageParserState.breakableDepth();
                LanguageParserState.setBreakableDepth(previousBreakableDepth + 1);
                try {
                    defaultBody = parser.subparse(
                            tkn -> tkn.test(Token.Type.NAME, "case", "endswitch"));
                } finally {
                    LanguageParserState.setBreakableDepth(previousBreakableDepth);
                }
                continue;
            }

            throw new ParserException(
                    null,
                    "Unexpected tag in a switch: " + current.getValue(),
                    current.getLineNumber(),
                    stream.getFilename());
        }

        return new SwitchNode(lineNumber, subject, cases, defaultBody);
    }

    private static boolean isWhitespaceOnly(BodyNode body) {
        for (RenderableNode child : body.getChildren()) {
            if (child instanceof TextNode text) {
                if (!new String(text.getData()).isBlank()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
