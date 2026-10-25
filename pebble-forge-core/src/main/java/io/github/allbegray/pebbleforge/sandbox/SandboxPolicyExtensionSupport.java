package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.attributes.DefaultAttributeResolver;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.tokenParser.BlockTokenParser;
import io.pebbletemplates.pebble.tokenParser.EmbedTokenParser;
import io.pebbletemplates.pebble.tokenParser.MacroTokenParser;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SandboxPolicyExtensionSupport {

    private SandboxPolicyExtensionSupport() {
    }

    static Map<String, Filter> filters(Map<String, Filter> source,
            SandboxPolicyResolver policies) {
        Map<String, Filter> result = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, filter) -> {
                if (!SandboxPolicySupport.anyAllow(
                        policies, SandboxPolicySupport.Kind.FILTER, name)) {
                    return;
                }
                result.put(name, SandboxPolicySupport.allAllow(
                        policies, SandboxPolicySupport.Kind.FILTER, name)
                        ? filter
                        : SandboxPolicySupport.wrapFilter(name, filter, policies));
            });
        }
        return Map.copyOf(result);
    }

    static Map<String, Test> tests(Map<String, Test> source,
            SandboxPolicyResolver policies) {
        Map<String, Test> result = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, test) -> {
                if (!SandboxPolicySupport.anyAllow(
                        policies, SandboxPolicySupport.Kind.TEST, name)) {
                    return;
                }
                result.put(name, SandboxPolicySupport.allAllow(
                        policies, SandboxPolicySupport.Kind.TEST, name)
                        ? test
                        : SandboxPolicySupport.wrapTest(name, test, policies));
            });
        }
        return Map.copyOf(result);
    }

    static Map<String, Function> functions(Map<String, Function> source,
            SandboxPolicyResolver policies) {
        Map<String, Function> result = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, function) -> {
                if (!SandboxPolicySupport.anyAllow(
                        policies, SandboxPolicySupport.Kind.FUNCTION, name)) {
                    return;
                }
                result.put(name, SandboxPolicySupport.allAllow(
                        policies, SandboxPolicySupport.Kind.FUNCTION, name)
                        ? function
                        : new PolicyAwareFunction(name, function, policies));
            });
        }
        return Map.copyOf(result);
    }

    static List<TokenParser> tokenParsers(List<TokenParser> source,
            SandboxPolicyResolver policies) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .filter(parser -> SandboxPolicySupport.anyAllow(
                        policies, SandboxPolicySupport.Kind.TAG, parser.getTag()))
                .map(parser -> {
                    if (parser instanceof EmbedTokenParser) {
                        return new PolicyAwareEmbedTokenParser(parser, policies);
                    }
                    if (parser instanceof BlockTokenParser) {
                        return new PolicyAwareBlockTokenParser(parser, policies);
                    }
                    if (parser instanceof MacroTokenParser) {
                        return new PolicyAwareMacroTokenParser(parser, policies);
                    }
                    if (SandboxPolicySupport.allAllow(
                            policies, SandboxPolicySupport.Kind.TAG, parser.getTag())) {
                        return parser;
                    }
                    return new PolicyAwareTokenParser(parser, policies);
                })
                .toList();
    }

    static Map<String, Object> globals(Map<String, Object> source,
            SandboxPolicyResolver policies) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, value) -> {
                if (SandboxPolicySupport.allAllow(
                        policies, SandboxPolicySupport.Kind.GLOBAL, name)) {
                    result.put(name, value);
                }
            });
        }
        return Map.copyOf(result);
    }

    static List<AttributeResolver> attributeResolvers(List<AttributeResolver> source,
            SandboxPolicyResolver policies) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(resolver -> resolver instanceof DefaultAttributeResolver
                        ? new PolicyAwareDefaultAttributeResolver(policies)
                        : resolver)
                .toList();
    }
}
