package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for Pebble Forge features inside a Spring Boot application.
 */
@ConfigurationProperties("pebble.forge")
public class PebbleForgeProperties {

    private final Components components = new Components();
    private final Sandbox sandbox = new Sandbox();
    private final Security security = new Security();

    public Components getComponents() {
        return components;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public Security getSecurity() {
        return security;
    }

    public static class Components {

        private boolean enabled = true;
        private String root = "components";
        private String suffix = "";
        private int maxDepth = 16;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }
    }

    public static class Sandbox {

        private boolean enabled = false;
        private int maxRenderedChars = 1_000_000;
        private boolean allowRawOutput = false;
        private boolean forceAutoEscaping = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxRenderedChars() {
            return maxRenderedChars;
        }

        public void setMaxRenderedChars(int maxRenderedChars) {
            this.maxRenderedChars = maxRenderedChars;
        }

        public boolean isAllowRawOutput() {
            return allowRawOutput;
        }

        public void setAllowRawOutput(boolean allowRawOutput) {
            this.allowRawOutput = allowRawOutput;
        }

        public boolean isForceAutoEscaping() {
            return forceAutoEscaping;
        }

        public void setForceAutoEscaping(boolean forceAutoEscaping) {
            this.forceAutoEscaping = forceAutoEscaping;
        }
    }

    public static class Security {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
