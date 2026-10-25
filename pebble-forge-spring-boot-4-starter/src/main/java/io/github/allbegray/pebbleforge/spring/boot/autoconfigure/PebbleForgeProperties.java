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
    private final Language language = new Language();
    private boolean devMode;

    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public Components getComponents() {
        return components;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public Security getSecurity() {
        return security;
    }

    public Language getLanguage() {
        return language;
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
        private int maxLoopIterations = 100_000;
        private int maxIncludeDepth = 16;
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

        public int getMaxLoopIterations() {
            return maxLoopIterations;
        }

        public void setMaxLoopIterations(int maxLoopIterations) {
            this.maxLoopIterations = maxLoopIterations;
        }

        public int getMaxIncludeDepth() {
            return maxIncludeDepth;
        }

        public void setMaxIncludeDepth(int maxIncludeDepth) {
            this.maxIncludeDepth = maxIncludeDepth;
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

    public static class Language {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
