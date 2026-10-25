package io.github.allbegray.pebbleforge.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Configuration for template-defined components.
 */
public final class Components {

    private static final Pattern NAMESPACE = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");

    private final Map<String, String> roots;
    private final String suffix;
    private final int maxDepth;
    private final int maxSlotChars;

    private Components(Map<String, String> roots, String suffix, int maxDepth, int maxSlotChars) {
        this.roots = Collections.unmodifiableMap(new LinkedHashMap<>(roots));
        this.suffix = suffix;
        this.maxDepth = maxDepth;
        this.maxSlotChars = maxSlotChars;
    }

    /**
     * Creates a builder with the default root, suffix, and depth.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the default component configuration.
     *
     * @return the default configuration
     */
    public static Components defaults() {
        return builder().build();
    }

    /**
     * Returns the default root, or {@code null} when only namespaced roots are configured.
     *
     * @return the default root
     */
    public String root() {
        return roots.get("");
    }

    /**
     * Returns namespace to root. The default root uses the empty namespace.
     *
     * @return the configured roots
     */
    public Map<String, String> roots() {
        return roots;
    }

    /**
     * Returns the file suffix appended to component paths.
     *
     * @return the suffix, such as {@code .peb}
     */
    public String suffix() {
        return suffix;
    }

    /**
     * Returns the maximum component nesting depth.
     *
     * @return the maximum depth
     */
    public int maxDepth() {
        return maxDepth;
    }

    /**
     * Returns the maximum slot content length, or -1 when unlimited.
     *
     * @return the maximum slot length
     */
    public int maxSlotChars() {
        return maxSlotChars;
    }

    /**
     * Returns a copy with a different slot content limit.
     *
     * @param maxSlotChars the maximum slot content length, or -1 for no limit
     * @return the copy
     */
    public Components withMaxSlotChars(int maxSlotChars) {
        requireValidMaxSlotChars(maxSlotChars);
        return new Components(roots, suffix, maxDepth, maxSlotChars);
    }

    /**
     * Resolves a component name to the template path for its configured root.
     *
     * <p>A name may start with a namespace, such as {@code ui:card}. Names without a namespace
     * use the default root.
     *
     * @param componentName the component name
     * @return the template path
     * @throws IllegalArgumentException if the name is empty, contains an invalid segment, or uses
     *         an unknown namespace
     */
    public String resolvePath(String componentName) {
        Objects.requireNonNull(componentName, "componentName");
        String name = componentName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Component name must not be empty");
        }
        if (name.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Component name must use '/' separators: " + name);
        }
        if (name.startsWith("/")) {
            throw new IllegalArgumentException("Component name must be relative: " + name);
        }

        String namespace = "";
        int colon = name.indexOf(':');
        if (colon >= 0) {
            if (colon != name.lastIndexOf(':')) {
                throw new IllegalArgumentException(
                        "Component name must contain at most one ':' namespace separator: " + name);
            }
            namespace = name.substring(0, colon);
            if (namespace.isEmpty()) {
                throw new IllegalArgumentException(
                        "Component namespace must not be empty: " + name);
            }
            name = name.substring(colon + 1);
            if (name.isEmpty()) {
                throw new IllegalArgumentException(
                        "Component name must not be empty after namespace [" + namespace + "]");
            }
        }

        List<String> segments = List.of(name.split("/", -1));
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(
                        "Component name contains an invalid segment: " + name);
            }
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (!isPathCharacter(c)) {
                    throw new IllegalArgumentException(
                            "Component name contains an invalid character: " + c);
                }
            }
        }

        String root = roots.get(namespace);
        if (root == null) {
            if (namespace.isEmpty()) {
                throw new IllegalArgumentException(
                        "No default component root is configured; "
                                + "use a namespaced component name such as 'ui:card'");
            }
            throw new IllegalArgumentException(
                    "Unknown component namespace [" + namespace + "]; "
                            + "declared namespaces: " + roots.keySet());
        }
        String path = root.isEmpty() ? name : root + "/" + name;
        return path + suffix;
    }

    private static boolean isPathCharacter(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private static String normalizeRoot(String root) {
        Objects.requireNonNull(root, "root");
        String value = root.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.contains("..") || value.contains("\\")) {
            throw new IllegalArgumentException(
                    "Component root must be a relative directory: " + root);
        }
        return value;
    }

    private static void requireValidMaxSlotChars(int maxSlotChars) {
        if (maxSlotChars < -1) {
            throw new IllegalArgumentException("maxSlotChars must be -1 or greater");
        }
    }

    private static void requireDistinctRoots(Map<String, String> roots) {
        Map<String, String> seen = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : roots.entrySet()) {
            String previous = seen.putIfAbsent(entry.getValue(), entry.getKey());
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Component namespaces [" + displayNamespace(previous) + "] and ["
                                + displayNamespace(entry.getKey())
                                + "] resolve to the same root: " + entry.getValue());
            }
        }
    }

    private static String displayNamespace(String namespace) {
        return namespace.isEmpty() ? "\"\" (default)" : namespace;
    }

    /**
     * Builds a {@link Components} configuration.
     */
    public static final class Builder {

        /**
         * Creates an empty builder. Prefer {@link Components#builder()}.
         */
        public Builder() {
        }

        private final Map<String, String> namedRoots = new LinkedHashMap<>();
        private String defaultRoot = "components";
        private boolean defaultRootDeclared;
        private String suffix = ".peb";
        private int maxDepth = 16;
        private int maxSlotChars = -1;

        /**
         * Sets the default root. May be called once.
         *
         * @param root the root directory
         * @return this builder
         */
        public Builder root(String root) {
            if (defaultRootDeclared) {
                throw new IllegalArgumentException(
                        "Default component root is already declared as [" + defaultRoot + "]");
            }
            this.defaultRoot = normalizeRoot(root);
            this.defaultRootDeclared = true;
            return this;
        }

        /**
         * Declares a root for a namespace. A namespace may be declared once.
         *
         * @param namespace the namespace name
         * @param root the root directory
         * @return this builder
         */
        public Builder root(String namespace, String root) {
            Objects.requireNonNull(namespace, "namespace");
            if (namespace.isEmpty()) {
                throw new IllegalArgumentException(
                        "Component namespace must not be empty; "
                                + "use root(String) for the default root");
            }
            if (!NAMESPACE.matcher(namespace).matches()) {
                throw new IllegalArgumentException(
                        "Component namespace must match [A-Za-z][A-Za-z0-9_-]*: " + namespace);
            }
            if (namedRoots.containsKey(namespace)) {
                throw new IllegalArgumentException(
                        "Component namespace [" + namespace + "] is already declared");
            }
            namedRoots.put(namespace, normalizeRoot(root));
            return this;
        }

        /**
         * Sets the file suffix. It must be empty or start with a dot.
         *
         * @param suffix the file suffix
         * @return this builder
         */
        public Builder suffix(String suffix) {
            Objects.requireNonNull(suffix, "suffix");
            if (!suffix.isEmpty() && !suffix.startsWith(".")) {
                throw new IllegalArgumentException("Component suffix must start with '.': " + suffix);
            }
            this.suffix = suffix;
            return this;
        }

        /**
         * Sets the maximum component nesting depth.
         *
         * @param maxDepth the maximum depth, at least 1
         * @return this builder
         */
        public Builder maxDepth(int maxDepth) {
            if (maxDepth < 1) {
                throw new IllegalArgumentException("maxDepth must be at least 1");
            }
            this.maxDepth = maxDepth;
            return this;
        }

        /**
         * Sets the maximum slot content length.
         *
         * @param maxSlotChars the maximum length, or -1 for no limit
         * @return this builder
         */
        public Builder maxSlotChars(int maxSlotChars) {
            requireValidMaxSlotChars(maxSlotChars);
            this.maxSlotChars = maxSlotChars;
            return this;
        }

        /**
         * Builds the configuration. Declaring a namespaced root without calling
         * {@link #root(String)} leaves the configuration without a default root, so
         * unqualified component names fail. Call {@link #root(String)} to keep the
         * conventional {@code components} root alongside namespaced roots.
         *
         * @return the configuration
         */
        public Components build() {
            Map<String, String> effectiveRoots = new LinkedHashMap<>();
            if (defaultRootDeclared || namedRoots.isEmpty()) {
                effectiveRoots.put("", defaultRoot);
            }
            effectiveRoots.putAll(namedRoots);
            requireDistinctRoots(effectiveRoots);
            return new Components(effectiveRoots, suffix, maxDepth, maxSlotChars);
        }
    }
}
