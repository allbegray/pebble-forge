package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.error.ParserException;
import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.node.MacroNode;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

final class PolicyAwareMacroTokenParser implements TokenParser {

    private final TokenParser delegate;
    private final SandboxPolicyResolver policies;

    PolicyAwareMacroTokenParser(TokenParser delegate, SandboxPolicyResolver policies) {
        this.delegate = delegate;
        this.policies = policies;
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
        RenderableNode node = delegate.parse(token, parser);
        if (node instanceof MacroNode macro) {
            return new PolicyAwareMacroNode(macro, filename);
        }
        return node;
    }
}
