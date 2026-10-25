package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Extension;
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
 * Applies a policy resolver to a user-provided extension.
 */
public final class SandboxedExtension extends AbstractExtension {

    private final Extension delegate;
    private final SandboxPolicyResolver policies;

    /**
     * Applies one policy to the extension.
     *
     * @param delegate the extension to wrap
     * @param policy the policy to apply
     */
    public SandboxedExtension(Extension delegate, SandboxPolicy policy) {
        this(delegate, SandboxPolicyResolver.fixed(policy));
    }

    /**
     * Applies a policy resolver to the extension.
     *
     * @param delegate the extension to wrap
     * @param policies the policy resolver
     */
    public SandboxedExtension(Extension delegate, SandboxPolicyResolver policies) {
        this.delegate = delegate;
        this.policies = policies;
    }

    @Override
    public Map<String, Filter> getFilters() {
        Map<String, Filter> filters = delegate.getFilters();
        rejectReservedFilters(filters);
        return SandboxPolicyExtensionSupport.filters(filters, policies);
    }

    @Override
    public Map<String, Test> getTests() {
        Map<String, Test> tests = delegate.getTests();
        rejectReservedTests(tests);
        return SandboxPolicyExtensionSupport.tests(tests, policies);
    }

    @Override
    public Map<String, Function> getFunctions() {
        return SandboxPolicyExtensionSupport.functions(delegate.getFunctions(), policies);
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return SandboxPolicyExtensionSupport.tokenParsers(delegate.getTokenParsers(), policies);
    }

    @Override
    public List<BinaryOperator> getBinaryOperators() {
        return List.of();
    }

    @Override
    public List<UnaryOperator> getUnaryOperators() {
        return List.of();
    }

    @Override
    public Map<String, Object> getGlobalVariables() {
        return SandboxPolicyExtensionSupport.globals(delegate.getGlobalVariables(), policies);
    }

    @Override
    public List<NodeVisitorFactory> getNodeVisitors() {
        return List.of();
    }

    @Override
    public List<AttributeResolver> getAttributeResolver() {
        return List.of();
    }

    private static void rejectReservedFilters(Map<String, Filter> filters) {
        if (filters != null && (filters.containsKey("escape")
                || filters.containsKey("default"))) {
            throw new IllegalArgumentException(
                    "User extensions cannot override the escape or default filter");
        }
    }

    private static void rejectReservedTests(Map<String, Test> tests) {
        if (tests != null && tests.containsKey("defined")) {
            throw new IllegalArgumentException(
                    "User extensions cannot override the defined test");
        }
    }
}
