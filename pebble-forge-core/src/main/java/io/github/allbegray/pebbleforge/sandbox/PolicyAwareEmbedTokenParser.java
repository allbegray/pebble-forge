package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.error.ParserException;
import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.BlockNode;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.node.expression.Expression;
import io.pebbletemplates.pebble.node.expression.MapExpression;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.BlockTokenParser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors Pebble's embed parser so overridden blocks keep the template that declared them.
 */
final class PolicyAwareEmbedTokenParser implements TokenParser {

    private final TokenParser delegate;
    private final SandboxPolicyResolver policies;
    private final PolicyAwareBlockTokenParser blockParser;

    PolicyAwareEmbedTokenParser(TokenParser delegate, SandboxPolicyResolver policies) {
        this.delegate = delegate;
        this.policies = policies;
        this.blockParser = new PolicyAwareBlockTokenParser(new BlockTokenParser(), policies);
    }

    @Override
    public String getTag() {
        return delegate.getTag();
    }

    @Override
    public RenderableNode parse(Token token, Parser parser) {
        String filename = parser.getStream().getFilename();
        SandboxPolicy policy = SandboxPolicySupport.policyFor(policies, filename);
        if (!policy.isTagAllowed(getTag())) {
            throw new ParserException(
                    null,
                    "Tag [" + getTag() + "] is not allowed for template [" + filename + "]",
                    token.getLineNumber(),
                    filename);
        }

        TokenStream stream = parser.getStream();
        int lineNumber = token.getLineNumber();

        stream.next();
        Expression<?> embedExpression = parser.getExpressionParser().parseExpression();

        Token current = stream.current();
        MapExpression mapExpression = null;
        if (current.getType().equals(Token.Type.NAME)
                && current.getValue().equals("with")) {
            stream.next();
            Expression<?> parsedExpression = parser.getExpressionParser().parseExpression();
            if (parsedExpression instanceof MapExpression map) {
                mapExpression = map;
            } else {
                throw new ParserException(
                        null,
                        String.format("Unexpected expression '%1s'.",
                                parsedExpression.getClass().getCanonicalName()),
                        token.getLineNumber(),
                        stream.getFilename());
            }
        }

        stream.expect(Token.Type.EXECUTE_END);
        List<BlockNode> blocks = parseBlocks(token, parser, stream, filename);
        return new PolicyAwareEmbedNode(lineNumber, embedExpression, mapExpression, blocks);
    }

    private List<BlockNode> parseBlocks(Token token, Parser parser, TokenStream stream,
            String filename) {
        List<BlockNode> blocks = new ArrayList<>();
        while (true) {
            BlockNode node = parseBlock(token, parser, stream, filename);
            if (node == null) {
                break;
            }
            blocks.add(node);
        }
        return blocks;
    }

    private BlockNode parseBlock(Token token, Parser parser, TokenStream stream,
            String filename) {
        if (stream.current().test(Token.Type.TEXT)) {
            Token textToken = stream.expect(Token.Type.TEXT);
            if (textToken.getValue().trim().length() > 0) {
                throw new ParserException(
                        null,
                        "A template that extends another one cannot include content outside "
                                + "blocks. Did you forget to put the content inside a "
                                + "{% block %} tag?",
                        textToken.getLineNumber(),
                        stream.getFilename());
            }
        }

        stream.expect(Token.Type.EXECUTE_START);
        if (stream.current().test(Token.Type.NAME, "endembed")) {
            stream.expect(Token.Type.NAME, "endembed");
            stream.expect(Token.Type.EXECUTE_END);
            return null;
        }

        RenderableNode node = blockParser.parse(token, parser);
        return node instanceof BlockNode block ? block : null;
    }
}
