package io.github.allbegray.pebbleforge.spring.security;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.github.allbegray.pebbleforge.spring.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringSecurityExtensionTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void exposesTheAuthenticatedUserAndRoles() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("ada", "n/a", "ROLE_ADMIN", "order:read"));

        String output = render(Map.of("page.peb", """
                {{ isAuthenticated() }}|{{ username() }}|{{ hasRole("ADMIN") }}|{{ hasAuthority("order:read") }}
                """), null);

        assertThat(output.strip()).isEqualTo("true|ada|true|true");
    }

    @Test
    void treatsAnonymousUsersAsLoggedOut() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        String output = render(Map.of("page.peb", """
                {{ isAuthenticated() }}|{{ username() }}|{{ hasRole("ADMIN") }}|{{ authorities() | length }}
                """), null);

        assertThat(output.strip()).isEqualTo("false||false|0");
    }

    @Test
    void exposesTheCsrfTokenForForms() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CsrfToken.class.getName(),
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String output = render(Map.of("page.peb", """
                <input type="hidden" name="{{ csrfParameterName() }}" value="{{ csrfToken() }}">
                <meta name="csrf-header" content="{{ csrfHeaderName() }}">
                """), null);

        assertThat(output).isEqualTo("""
                <input type="hidden" name="_csrf" value="token-123">
                <meta name="csrf-header" content="X-CSRF-TOKEN">
                """);
    }

    @Test
    void requiresTheFunctionsToBeAllowedInTheSandbox() {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", "{{ username() }}")))
                .extension(new SpringSecurityExtension())
                .sandbox(SandboxPolicy.strict())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("ada", "n/a", "ROLE_ADMIN"));

        assertThatThrownBy(() -> engine.getTemplate("page.peb").evaluate(new StringWriter()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("username");
    }

    @Test
    void allowsTheFunctionsThroughTheSandboxHelper() throws Exception {
        SandboxPolicy policy = SpringSecurityFunctions.allow(SandboxPolicy.builder()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("ada", "n/a", "ROLE_ADMIN"));

        String output = render(Map.of("page.peb", "{{ username() }}"), policy);

        assertThat(output).isEqualTo("ada");
    }

    private static String render(Map<String, String> templates, SandboxPolicy policy)
            throws Exception {
        PebbleForge.Builder builder = PebbleForge.engine()
                .loader(new MapLoader(templates))
                .extension(new SpringSecurityExtension());
        if (policy != null) {
            builder.sandbox(policy);
        }
        PebbleEngine engine = builder.build();
        StringWriter writer = new StringWriter();
        engine.getTemplate("page.peb").evaluate(writer);
        return writer.toString();
    }
}
