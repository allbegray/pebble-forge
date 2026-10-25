package io.github.allbegray.pebbleforge.inheritance;

import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.node.ExtendsNode;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.ExtendsTokenParser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

/**
 * Delegates to Pebble's extends parser and wraps the resulting node.
 */
final class ForgeExtendsTokenParser implements TokenParser {

    private final ExtendsTokenParser delegate = new ExtendsTokenParser();

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        RenderableNode node = delegate.parse(token, parser);
        if (node instanceof ExtendsNode extendsNode) {
            return new ForgeExtendsNode(extendsNode.getLineNumber(),
                    extendsNode.getParentExpression());
        }
        return node;
    }

    @Override
    public String getTag() {
        return "extends";
    }
}
