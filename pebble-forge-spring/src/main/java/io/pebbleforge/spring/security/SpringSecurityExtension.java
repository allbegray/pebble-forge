package io.pebbleforge.spring.security;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exposes the current Spring Security state without handing the template an Authentication object.
 */
public final class SpringSecurityExtension extends AbstractExtension {

    public static final String IS_AUTHENTICATED = "isAuthenticated";
    public static final String HAS_ROLE = "hasRole";
    public static final String HAS_AUTHORITY = "hasAuthority";
    public static final String AUTHORITIES = "authorities";
    public static final String USERNAME = "username";
    public static final String CSRF_TOKEN = "csrfToken";
    public static final String CSRF_PARAMETER_NAME = "csrfParameterName";
    public static final String CSRF_HEADER_NAME = "csrfHeaderName";

    @Override
    public Map<String, Function> getFunctions() {
        return Map.of(
                IS_AUTHENTICATED, new BaseFunction() {
                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self,
                                          EvaluationContext context, int lineNumber) {
                        return isLoggedIn();
                    }
                },
                HAS_ROLE, new BaseFunction("role") {
                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self,
                                          EvaluationContext context, int lineNumber) {
                        String role = stringArg(args, "role");
                        if (role.isEmpty()) {
                            return false;
                        }
                        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        return hasAuthority(authority);
                    }
                },
                HAS_AUTHORITY, new BaseFunction("authority") {
                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self,
                                          EvaluationContext context, int lineNumber) {
                        return hasAuthority(stringArg(args, "authority"));
                    }
                },
                AUTHORITIES, new BaseFunction() {
                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self,
                                          EvaluationContext context, int lineNumber) {
                        return authorities();
                    }
                },
                USERNAME, new BaseFunction() {
                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self,
                                          EvaluationContext context, int lineNumber) {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        return isLoggedIn() && authentication != null ? authentication.getName() : "";
                    }
                },
                CSRF_TOKEN, new BaseFunction() {
                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self,
                                          EvaluationContext context, int lineNumber) {
                        CsrfToken token = csrfToken();
                        return token == null ? "" : token.getToken();
                    }
                },
                CSRF_PARAMETER_NAME, new BaseFunction() {
                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self,
                                          EvaluationContext context, int lineNumber) {
                        CsrfToken token = csrfToken();
                        return token == null ? "_csrf" : token.getParameterName();
                    }
                },
                CSRF_HEADER_NAME, new BaseFunction() {
                    @Override
                    public Object execute(Map<String, Object> args, PebbleTemplate self,
                                          EvaluationContext context, int lineNumber) {
                        CsrfToken token = csrfToken();
                        return token == null ? "X-CSRF-TOKEN" : token.getHeaderName();
                    }
                });
    }

    private static boolean isLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static boolean hasAuthority(String authority) {
        if (authority.isEmpty() || !isLoggedIn()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (granted.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> authorities() {
        if (!isLoggedIn()) {
            return List.of();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            result.add(granted.getAuthority());
        }
        return List.copyOf(result);
    }

    private static CsrfToken csrfToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        Object token = servletAttributes.getRequest().getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            return csrfToken;
        }
        Object legacy = servletAttributes.getRequest().getAttribute("_csrf");
        return legacy instanceof CsrfToken csrfToken ? csrfToken : null;
    }

    private abstract static class BaseFunction implements Function {

        private final List<String> argumentNames;

        private BaseFunction(String... argumentNames) {
            this.argumentNames = List.of(argumentNames);
        }

        @Override
        public final List<String> getArgumentNames() {
            return argumentNames;
        }

        protected final String stringArg(Map<String, Object> args, String name) {
            Object value = args.get(name);
            return value == null ? "" : String.valueOf(value).trim();
        }
    }
}
