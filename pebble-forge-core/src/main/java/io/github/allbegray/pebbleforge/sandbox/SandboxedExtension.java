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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies a sandbox policy to a user-provided extension.
 */
public final class SandboxedExtension extends AbstractExtension {

    private final Extension delegate;
    private final SandboxPolicy policy;

    public SandboxedExtension(Extension delegate, SandboxPolicy policy) {
        this.delegate = delegate;
        this.policy = policy;
    }

    @Override
    public Map<String, Filter> getFilters() {
        return filter(delegate.getFilters(), policy::isFilterAllowed);
    }

    @Override
    public Map<String, Test> getTests() {
        return filter(delegate.getTests(), policy::isTestAllowed);
    }

    @Override
    public Map<String, Function> getFunctions() {
        return filter(delegate.getFunctions(), policy::isFunctionAllowed);
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        List<TokenParser> parsers = delegate.getTokenParsers();
        if (parsers == null) {
            return List.of();
        }
        return parsers.stream()
                .filter(parser -> policy.isTagAllowed(parser.getTag()))
                .toList();
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
        return filter(delegate.getGlobalVariables(), policy::isGlobalVariableAllowed);
    }

    @Override
    public List<NodeVisitorFactory> getNodeVisitors() {
        return List.of();
    }

    @Override
    public List<AttributeResolver> getAttributeResolver() {
        return List.of();
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
