package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.error.ParserException;
import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

final class ContinueTokenParser implements TokenParser {

    @Override
    public String getTag() {
        return "continue";
    }

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        if (LanguageParserState.loopDepth() == 0) {
            throw new ParserException(
                    null,
                    "The continue tag is only allowed inside a for loop",
                    token.getLineNumber(),
                    parser.getStream().getFilename());
        }
        TokenStream stream = parser.getStream();
        stream.next();
        stream.expect(Token.Type.EXECUTE_END);
        return new ContinueNode(token.getLineNumber());
    }
}
