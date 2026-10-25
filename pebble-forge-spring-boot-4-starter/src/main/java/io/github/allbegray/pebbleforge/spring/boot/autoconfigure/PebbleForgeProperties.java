package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for Pebble Forge features inside a Spring Boot application.
 */
@ConfigurationProperties("pebble.forge")
public class PebbleForgeProperties {

    private final Components components = new Components();
    private final Sandbox sandbox = new Sandbox();
    private final Security security = new Security();
    private final Language language = new Language();
    private final Diagnostics diagnostics = new Diagnostics();
    private boolean devMode;

    /**
     * Creates empty properties for Spring Boot binding.
     */
    public PebbleForgeProperties() {
    }

    /**
     * Returns whether templates reload on change.
     *
     * @return true when development mode is on
     */
    public boolean isDevMode() {
        return devMode;
    }

    /**
     * Sets whether templates reload on change.
     *
     * @param devMode true to enable development mode
     */
    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    /**
     * Returns the component settings.
     *
     * @return the component settings
     */
    public Components getComponents() {
        return components;
    }

    /**
     * Returns the sandbox settings.
     *
     * @return the sandbox settings
     */
    public Sandbox getSandbox() {
        return sandbox;
    }

    /**
     * Returns the Spring Security settings.
     *
     * @return the security settings
     */
    public Security getSecurity() {
        return security;
    }

    /**
     * Returns the language extension settings.
     *
     * @return the language settings
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Returns the diagnostics settings.
     *
     * @return the diagnostics settings
     */
    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    /**
     * Settings for the component, slot, and props tags.
     */
    public static class Components {

        private boolean enabled = true;
        private String root = "components";
        private String suffix = "";
        private int maxDepth = 16;
        private final Map<String, String> roots = new LinkedHashMap<>();

        /**
         * Creates default component settings.
         */
        public Components() {
        }

        /**
         * Returns whether component tags are enabled.
         *
         * @return true when component tags are enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether component tags are enabled.
         *
         * @param enabled true to enable component tags
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the default component root.
         *
         * @return the default root
         */
        public String getRoot() {
            return root;
        }

        /**
         * Sets the default component root.
         *
         * @param root the default root
         */
        public void setRoot(String root) {
            this.root = root;
        }

        /**
         * Returns the component file suffix.
         *
         * @return the suffix
         */
        public String getSuffix() {
            return suffix;
        }

        /**
         * Sets the component file suffix.
         *
         * @param suffix the suffix
         */
        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        /**
         * Returns the maximum component nesting depth.
         *
         * @return the maximum depth
         */
        public int getMaxDepth() {
            return maxDepth;
        }

        /**
         * Sets the maximum component nesting depth.
         *
         * @param maxDepth the maximum depth
         */
        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        /**
         * Returns namespace to component root.
         *
         * @return the namespaced roots
         */
        public Map<String, String> getRoots() {
            return roots;
        }
    }

    /**
     * Settings for the sandbox.
     */
    public static class Sandbox {

        private boolean enabled = false;
        private int maxRenderedChars = 1_000_000;
        private int maxLoopIterations = 100_000;
        private int maxTotalIterations = 1_000_000;
        private int maxIncludeDepth = 16;
        private int maxSlotChars = 1_000_000;
        private boolean allowRawOutput = false;
        private boolean forceAutoEscaping = true;

        /**
         * Creates default sandbox settings.
         */
        public Sandbox() {
        }

        /**
         * Returns whether the sandbox is enabled.
         *
         * @return true when the sandbox is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the sandbox is enabled.
         *
         * @param enabled true to enable the sandbox
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the maximum rendered output size.
         *
         * @return the limit in characters
         */
        public int getMaxRenderedChars() {
            return maxRenderedChars;
        }

        /**
         * Sets the maximum rendered output size.
         *
         * @param maxRenderedChars the limit in characters
         */
        public void setMaxRenderedChars(int maxRenderedChars) {
            this.maxRenderedChars = maxRenderedChars;
        }

        /**
         * Returns the maximum iterations of one loop.
         *
         * @return the limit
         */
        public int getMaxLoopIterations() {
            return maxLoopIterations;
        }

        /**
         * Sets the maximum iterations of one loop.
         *
         * @param maxLoopIterations the limit
         */
        public void setMaxLoopIterations(int maxLoopIterations) {
            this.maxLoopIterations = maxLoopIterations;
        }

        /**
         * Returns the maximum iterations across all nested loops.
         *
         * @return the limit
         */
        public int getMaxTotalIterations() {
            return maxTotalIterations;
        }

        /**
         * Sets the maximum iterations across all nested loops.
         *
         * @param maxTotalIterations the limit
         */
        public void setMaxTotalIterations(int maxTotalIterations) {
            this.maxTotalIterations = maxTotalIterations;
        }

        /**
         * Returns the maximum nested include depth.
         *
         * @return the limit
         */
        public int getMaxIncludeDepth() {
            return maxIncludeDepth;
        }

        /**
         * Sets the maximum nested include depth.
         *
         * @param maxIncludeDepth the limit
         */
        public void setMaxIncludeDepth(int maxIncludeDepth) {
            this.maxIncludeDepth = maxIncludeDepth;
        }

        /**
         * Returns the maximum slot content length.
         *
         * @return the limit in characters
         */
        public int getMaxSlotChars() {
            return maxSlotChars;
        }

        /**
         * Sets the maximum slot content length.
         *
         * @param maxSlotChars the limit in characters
         */
        public void setMaxSlotChars(int maxSlotChars) {
            this.maxSlotChars = maxSlotChars;
        }

        /**
         * Returns whether the {@code raw} filter is allowed.
         *
         * @return true when raw output is allowed
         */
        public boolean isAllowRawOutput() {
            return allowRawOutput;
        }

        /**
         * Sets whether the {@code raw} filter is allowed.
         *
         * @param allowRawOutput true to allow raw output
         */
        public void setAllowRawOutput(boolean allowRawOutput) {
            this.allowRawOutput = allowRawOutput;
        }

        /**
         * Returns whether auto-escaping is forced.
         *
         * @return true when auto-escaping is forced
         */
        public boolean isForceAutoEscaping() {
            return forceAutoEscaping;
        }

        /**
         * Sets whether auto-escaping is forced.
         *
         * @param forceAutoEscaping true to force auto-escaping
         */
        public void setForceAutoEscaping(boolean forceAutoEscaping) {
            this.forceAutoEscaping = forceAutoEscaping;
        }
    }

    /**
     * Settings for the Spring Security template functions.
     */
    public static class Security {

        private boolean enabled = true;

        /**
         * Creates default security settings.
         */
        public Security() {
        }

        /**
         * Returns whether the security functions are enabled.
         *
         * @return true when the security functions are enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the security functions are enabled.
         *
         * @param enabled true to enable the security functions
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Settings for the language extension.
     */
    public static class Language {

        private boolean enabled = true;

        /**
         * Creates default language settings.
         */
        public Language() {
        }

        /**
         * Returns whether the language tags are enabled.
         *
         * @return true when the language tags are enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the language tags are enabled.
         *
         * @param enabled true to enable the language tags
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Settings for render diagnostics.
     */
    public static class Diagnostics {

        private boolean enabled = true;
        private int contextLines = 2;
        private boolean exposeInErrorAttributes = false;

        /**
         * Creates default diagnostics settings.
         */
        public Diagnostics() {
        }

        /**
         * Returns whether diagnostics are enabled.
         *
         * @return true when diagnostics are enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether diagnostics are enabled.
         *
         * @param enabled true to enable diagnostics
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the number of source lines shown around an error.
         *
         * @return the context line count
         */
        public int getContextLines() {
            return contextLines;
        }

        /**
         * Sets the number of source lines shown around an error.
         *
         * @param contextLines the context line count
         */
        public void setContextLines(int contextLines) {
            this.contextLines = contextLines;
        }

        /**
         * Returns whether diagnostics are added to Spring Boot error attributes.
         *
         * @return true when diagnostics are exposed in error attributes
         */
        public boolean isExposeInErrorAttributes() {
            return exposeInErrorAttributes;
        }

        /**
         * Sets whether diagnostics are added to Spring Boot error attributes.
         *
         * @param exposeInErrorAttributes true to expose diagnostics in error attributes
         */
        public void setExposeInErrorAttributes(boolean exposeInErrorAttributes) {
            this.exposeInErrorAttributes = exposeInErrorAttributes;
        }
    }
}
