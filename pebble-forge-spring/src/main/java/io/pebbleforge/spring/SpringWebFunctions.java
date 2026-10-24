package io.pebbleforge.spring;

import io.pebbleforge.sandbox.SandboxPolicy;

import java.util.List;

/**
 * Adds the Pebble Spring extension functions to a sandbox policy.
 */
public final class SpringWebFunctions {

    public static final List<String> NAMES = List.of(
            "message",
            "hasErrors",
            "hasGlobalErrors",
            "hasFieldErrors",
            "getAllErrors",
            "getGlobalErrors",
            "getFieldErrors",
            "href");

    private SpringWebFunctions() {
    }

    public static SandboxPolicy.Builder allow(SandboxPolicy.Builder builder) {
        return builder.allowFunctions(NAMES.toArray(String[]::new));
    }
}
