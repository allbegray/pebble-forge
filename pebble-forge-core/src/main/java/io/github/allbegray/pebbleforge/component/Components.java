package io.github.allbegray.pebbleforge.component;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for template-defined components.
 */
public final class Components {

    private final String root;
    private final String suffix;
    private final int maxDepth;

    private Components(String root, String suffix, int maxDepth) {
        this.root = root;
        this.suffix = suffix;
        this.maxDepth = maxDepth;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Components defaults() {
        return builder().build();
    }

    public String root() {
        return root;
    }

    public String suffix() {
        return suffix;
    }

    public int maxDepth() {
        return maxDepth;
    }

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
        List<String> segments = List.of(name.split("/", -1));
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Component name contains an invalid segment: " + name);
            }
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                boolean ok = Character.isLetterOrDigit(c) || c == '_' || c == '-';
                if (!ok) {
                    throw new IllegalArgumentException("Component name contains an invalid character: " + c);
                }
            }
        }
        String path = root.isEmpty() ? name : root + "/" + name;
        return path + suffix;
    }

    public static final class Builder {

        private String root = "components";
        private String suffix = ".peb";
        private int maxDepth = 16;

        public Builder root(String root) {
            Objects.requireNonNull(root, "root");
            String value = root.trim();
            while (value.startsWith("/")) {
                value = value.substring(1);
            }
            while (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            if (value.contains("..") || value.contains("\\")) {
                throw new IllegalArgumentException("Component root must be a relative directory: " + root);
            }
            this.root = value;
            return this;
        }

        public Builder suffix(String suffix) {
            Objects.requireNonNull(suffix, "suffix");
            if (!suffix.isEmpty() && !suffix.startsWith(".")) {
                throw new IllegalArgumentException("Component suffix must start with '.': " + suffix);
            }
            this.suffix = suffix;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            if (maxDepth < 1) {
                throw new IllegalArgumentException("maxDepth must be at least 1");
            }
            this.maxDepth = maxDepth;
            return this;
        }

        public Components build() {
            return new Components(root, suffix, maxDepth);
        }
    }
}
