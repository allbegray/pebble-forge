package io.github.allbegray.pebbleforge.spring.security;

import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;

import java.util.List;

/**
 * Adds the Spring Security helper functions to a sandbox policy.
 */
public final class SpringSecurityFunctions {

    public static final List<String> NAMES = List.of(
            SpringSecurityExtension.IS_AUTHENTICATED,
            SpringSecurityExtension.IS_ANONYMOUS,
            SpringSecurityExtension.IS_FULLY_AUTHENTICATED,
            SpringSecurityExtension.IS_REMEMBER_ME,
            SpringSecurityExtension.HAS_ROLE,
            SpringSecurityExtension.HAS_ANY_ROLE,
            SpringSecurityExtension.HAS_AUTHORITY,
            SpringSecurityExtension.HAS_ANY_AUTHORITY,
            SpringSecurityExtension.AUTHORITIES,
            SpringSecurityExtension.USERNAME,
            SpringSecurityExtension.CSRF_TOKEN,
            SpringSecurityExtension.CSRF_PARAMETER_NAME,
            SpringSecurityExtension.CSRF_HEADER_NAME);

    private SpringSecurityFunctions() {
    }

    public static SandboxPolicy.Builder allow(SandboxPolicy.Builder builder) {
        return builder.allowFunctions(NAMES.toArray(String[]::new));
    }
}
