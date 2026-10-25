package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.extension.ExtensionCustomizer;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.NodeVisitorFactory;
import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.operator.BinaryOperator;
import io.pebbletemplates.pebble.operator.UnaryOperator;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.List;
import java.util.Map;

/**
 * Applies a policy resolver to Pebble's built-in extensions.
 */
public final class SandboxExtensionCustomizer extends ExtensionCustomizer {

    private final SandboxPolicyResolver policies;

    public SandboxExtensionCustomizer(Extension delegate, SandboxPolicy policy) {
        this(delegate, SandboxPolicyResolver.fixed(policy));
    }

    public SandboxExtensionCustomizer(Extension delegate, SandboxPolicyResolver policies) {
        super(delegate);
        this.policies = policies;
    }

    @Override
    public Map<String, Filter> getFilters() {
        return SandboxPolicyExtensionSupport.filters(super.getFilters(), policies);
    }

    @Override
    public Map<String, Test> getTests() {
        return SandboxPolicyExtensionSupport.tests(super.getTests(), policies);
    }

    @Override
    public Map<String, Function> getFunctions() {
        return SandboxPolicyExtensionSupport.functions(super.getFunctions(), policies);
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return SandboxPolicyExtensionSupport.tokenParsers(super.getTokenParsers(), policies);
    }

    @Override
    public List<BinaryOperator> getBinaryOperators() {
        return super.getBinaryOperators();
    }

    @Override
    public List<UnaryOperator> getUnaryOperators() {
        return super.getUnaryOperators();
    }

    @Override
    public Map<String, Object> getGlobalVariables() {
        return SandboxPolicyExtensionSupport.globals(super.getGlobalVariables(), policies);
    }

    @Override
    public List<NodeVisitorFactory> getNodeVisitors() {
        return super.getNodeVisitors();
    }

    @Override
    public List<AttributeResolver> getAttributeResolver() {
        return SandboxPolicyExtensionSupport.attributeResolvers(
                super.getAttributeResolver(), policies);
    }
}
