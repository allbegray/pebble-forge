package io.github.allbegray.pebbleforge.spring;

import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;

import java.util.List;

/**
 * Adds the Pebble Spring extension functions to a sandbox policy.
 */
public final class SpringWebFunctions {

    /**
     * The Pebble Spring extension function names.
     */
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

    /**
     * Allows every Pebble Spring function in a sandbox policy.
     *
     * @param builder the policy builder
     * @return the same builder
     */
    public static SandboxPolicy.Builder allow(SandboxPolicy.Builder builder) {
        return builder.allowFunctions(NAMES.toArray(String[]::new));
    }
}
