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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies a sandbox policy to Pebble's built-in extensions.
 */
public final class SandboxExtensionCustomizer extends ExtensionCustomizer {

    private final SandboxPolicy policy;

    public SandboxExtensionCustomizer(Extension delegate, SandboxPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public Map<String, Filter> getFilters() {
        return filter(super.getFilters(), policy::isFilterAllowed);
    }

    @Override
    public Map<String, Test> getTests() {
        return filter(super.getTests(), policy::isTestAllowed);
    }

    @Override
    public Map<String, Function> getFunctions() {
        return filter(super.getFunctions(), policy::isFunctionAllowed);
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        List<TokenParser> parsers = super.getTokenParsers();
        if (parsers == null) {
            return List.of();
        }
        return parsers.stream()
                .filter(parser -> policy.isTagAllowed(parser.getTag()))
                .toList();
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
        return filter(super.getGlobalVariables(), policy::isGlobalVariableAllowed);
    }

    @Override
    public List<NodeVisitorFactory> getNodeVisitors() {
        return super.getNodeVisitors();
    }

    @Override
    public List<AttributeResolver> getAttributeResolver() {
        return super.getAttributeResolver();
    }

    private static <T> Map<String, T> filter(Map<String, T> values, java.util.function.Predicate<String> allowed) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, T> result = new LinkedHashMap<>();
        values.forEach((name, value) -> {
            if (allowed.test(name)) {
                result.put(name, value);
            }
        });
        return Map.copyOf(result);
    }
}
