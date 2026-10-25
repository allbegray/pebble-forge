package io.github.allbegray.pebbleforge;

import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.component.ComponentsExtension;
import io.github.allbegray.pebbleforge.escape.ContextEscaping;
import io.github.allbegray.pebbleforge.include.IncludeExtension;
import io.github.allbegray.pebbleforge.inheritance.ExtendsExtension;
import io.github.allbegray.pebbleforge.language.LanguageExtension;
import io.github.allbegray.pebbleforge.reload.ReloadingLoader;
import io.github.allbegray.pebbleforge.sandbox.SandboxAttributeExtension;
import io.github.allbegray.pebbleforge.sandbox.SandboxExtensionCustomizer;
import io.github.allbegray.pebbleforge.sandbox.PolicyAwareMethodAccessValidator;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimits;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimitsResolver;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicyResolver;
import io.github.allbegray.pebbleforge.sandbox.SandboxedExtension;
import io.github.allbegray.pebbleforge.sandbox.SandboxedLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.attributes.methodaccess.MethodAccessValidator;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Entry point for building a {@link PebbleEngine} with Pebble Forge extensions.
 *
 * <p>The builder enables the include, inheritance, language, and component extensions by default.
 * Pass a {@link SandboxPolicy} to run the same policy for every template, or a
 * {@link SandboxPolicyResolver} to choose a policy per template.
 */
public final class PebbleForge {

    private PebbleForge() {
    }

    /**
     * Creates a builder for a Pebble Forge engine.
     *
     * @return a new builder
     */
    public static Builder engine() {
        return new Builder();
    }

    /**
     * Configures and builds a {@link PebbleEngine}.
     */
    public static final class Builder {

        /**
         * Creates an empty builder. Prefer {@link PebbleForge#engine()}.
         */
        public Builder() {
        }

        private final PebbleEngine.Builder delegate = new PebbleEngine.Builder();
        private final List<Extension> extensions = new ArrayList<>();

        private Loader<?> loader;
        private Components components;
        private SandboxPolicy sandbox;
        private SandboxPolicyResolver policies;
        private SandboxLimitsResolver limitsResolver;
        private boolean language = true;
        private boolean devMode;
        private boolean strictVariables = true;
        private Boolean autoEscaping;
        private int maxRenderedChars = -1;
        private boolean cacheActive = true;
        private Locale defaultLocale;
        private boolean greedyMatchMethod;
        private MethodAccessValidator methodAccessValidator;
        private ExecutorService executorService;

        /**
         * Sets the template loader. The default is a {@link ClasspathLoader}.
         *
         * @param loader the loader to use
         * @return this builder
         */
        public Builder loader(Loader<?> loader) {
            this.loader = Objects.requireNonNull(loader, "loader");
            return this;
        }

        /**
         * Enables the component, slot, and props tags with the given configuration.
         *
         * @param components the component configuration
         * @return this builder
         */
        public Builder components(Components components) {
            this.components = Objects.requireNonNull(components, "components");
            return this;
        }

        /**
         * Applies one sandbox policy to every template.
         *
         * @param sandbox the policy to apply
         * @return this builder
         */
        public Builder sandbox(SandboxPolicy sandbox) {
            this.sandbox = Objects.requireNonNull(sandbox, "sandbox");
            return this;
        }

        /**
         * Resolves a sandbox policy for each template.
         *
         * @param policies the policy resolver
         * @return this builder
         */
        public Builder policies(SandboxPolicyResolver policies) {
            this.policies = Objects.requireNonNull(policies, "policies");
            return this;
        }

        /**
         * Resolves rendering limits for each template.
         *
         * @param limitsResolver the limits resolver
         * @return this builder
         */
        public Builder limitsResolver(SandboxLimitsResolver limitsResolver) {
            this.limitsResolver = Objects.requireNonNull(limitsResolver, "limitsResolver");
            return this;
        }

        /**
         * Enables or disables the {@code for}, {@code switch}, {@code with}, {@code break}, and
         * {@code continue} tags. Enabled by default.
         *
         * @param enabled whether to add the language extension
         * @return this builder
         */
        public Builder languageExtensions(boolean enabled) {
            this.language = enabled;
            return this;
        }

        /**
         * Reloads templates on change when enabled.
         *
         * @param devMode whether to wrap the loader in a {@link ReloadingLoader}
         * @return this builder
         */
        public Builder devMode(boolean devMode) {
            this.devMode = devMode;
            return this;
        }

        /**
         * Adds extensions after the built-in Pebble Forge extensions.
         *
         * @param extensions the extensions to add
         * @return this builder
         */
        public Builder extension(Extension... extensions) {
            for (Extension extension : extensions) {
                this.extensions.add(Objects.requireNonNull(extension, "extension"));
            }
            return this;
        }

        /**
         * Fails rendering on missing variables when enabled. Enabled by default.
         *
         * @param strictVariables whether to use strict variables
         * @return this builder
         */
        public Builder strictVariables(boolean strictVariables) {
            this.strictVariables = strictVariables;
            return this;
        }

        /**
         * Overrides Pebble's auto-escaping setting. A sandbox policy that forces auto-escaping
         * takes precedence.
         *
         * @param autoEscaping whether to enable auto-escaping
         * @return this builder
         */
        public Builder autoEscaping(boolean autoEscaping) {
            this.autoEscaping = autoEscaping;
            return this;
        }

        /**
         * Caps the rendered output size. A sandbox limit can make the cap smaller.
         *
         * @param maxRenderedChars the maximum number of rendered characters, or -1 for no cap
         * @return this builder
         */
        public Builder maxRenderedChars(int maxRenderedChars) {
            if (maxRenderedChars < -1) {
                throw new IllegalArgumentException("maxRenderedChars must be -1 or greater");
            }
            this.maxRenderedChars = maxRenderedChars;
            return this;
        }

        /**
         * Enables the template cache. Enabled by default.
         *
         * @param cacheActive whether to cache templates
         * @return this builder
         */
        public Builder cacheActive(boolean cacheActive) {
            this.cacheActive = cacheActive;
            return this;
        }

        /**
         * Sets the default locale used while rendering.
         *
         * @param defaultLocale the locale, or {@code null} to use the JVM default
         * @return this builder
         */
        public Builder defaultLocale(Locale defaultLocale) {
            this.defaultLocale = defaultLocale;
            return this;
        }

        /**
         * Enables Pebble's greedy method matching.
         *
         * @param greedyMatchMethod whether to use greedy method matching
         * @return this builder
         */
        public Builder greedyMatchMethod(boolean greedyMatchMethod) {
            this.greedyMatchMethod = greedyMatchMethod;
            return this;
        }

        /**
         * Sets the method access validator. Do not combine this method with
         * {@link #policies(SandboxPolicyResolver)}; set the validator on each policy instead.
         *
         * @param methodAccessValidator the validator to use
         * @return this builder
         */
        public Builder methodAccessValidator(MethodAccessValidator methodAccessValidator) {
            this.methodAccessValidator = Objects.requireNonNull(methodAccessValidator, "methodAccessValidator");
            return this;
        }

        /**
         * Sets the executor used for parallel rendering.
         *
         * @param executorService the executor, or {@code null} for sequential rendering
         * @return this builder
         */
        public Builder executorService(ExecutorService executorService) {
            this.executorService = Objects.requireNonNull(executorService, "executorService");
            return this;
        }

        /**
         * Builds the engine.
         *
         * @return the configured engine
         * @throws IllegalStateException if sandbox and policies are both set, or if a method access
         *         validator is combined with policies
         */
        public PebbleEngine build() {
            if (sandbox != null && policies != null) {
                throw new IllegalStateException(
                        "Use sandbox(policy) or policies(resolver), not both");
            }
            if (methodAccessValidator != null && policies != null) {
                throw new IllegalStateException(
                        "methodAccessValidator(...) is not supported with policies(...); "
                                + "set the validator on each SandboxPolicy");
            }
            SandboxPolicyResolver effectivePolicies = policies;
            if (effectivePolicies == null && sandbox != null) {
                effectivePolicies = SandboxPolicyResolver.fixed(sandbox);
            }
            if (sandbox != null) {
                requireSandboxSupport();
            }
            final SandboxPolicyResolver policyResolver = effectivePolicies;

            Loader<?> effectiveLoader = loader == null ? new ClasspathLoader() : loader;
            if (devMode) {
                effectiveLoader = ReloadingLoader.wrap(effectiveLoader);
            }
            if (effectivePolicies != null) {
                effectiveLoader = new SandboxedLoader<>(effectiveLoader, effectivePolicies);
                delegate.methodAccessValidator(new PolicyAwareMethodAccessValidator(effectivePolicies));
                delegate.registerExtensionCustomizer(
                        extension -> new SandboxExtensionCustomizer(extension, policyResolver));
                delegate.extension(new SandboxAttributeExtension(effectivePolicies));
            }
            int outputLimit = effectivePolicies == null
                    ? -1
                    : strictestOutputLimit(effectivePolicies);
            if (maxRenderedChars >= 0) {
                outputLimit = outputLimit < 0 ? maxRenderedChars : Math.min(outputLimit, maxRenderedChars);
            }
            if (outputLimit >= 0) {
                delegate.maxRenderedSize(outputLimit);
            }

            delegate.loader(effectiveLoader);
            delegate.strictVariables(strictVariables);
            delegate.cacheActive(cacheActive);
            delegate.greedyMatchMethod(greedyMatchMethod);
            if (defaultLocale != null) {
                delegate.defaultLocale(defaultLocale);
            }
            if (executorService != null) {
                delegate.executorService(executorService);
            }
            if (methodAccessValidator != null) {
                delegate.methodAccessValidator(methodAccessValidator);
            }

            if (effectivePolicies != null && forcesAutoEscaping(effectivePolicies)) {
                delegate.autoEscaping(true);
            } else if (autoEscaping != null) {
                delegate.autoEscaping(autoEscaping);
            }

            SandboxLimitsResolver effectiveLimits = limitsResolver;
            if (effectiveLimits == null) {
                effectiveLimits = effectivePolicies == null
                        ? SandboxLimitsResolver.fixed(SandboxLimits.unlimited())
                        : name -> SandboxLimits.from(policyResolver.resolve(name));
            }

            if (components != null) {
                Extension componentExtension = new ComponentsExtension(components, effectiveLimits);
                delegate.extension(effectivePolicies == null
                        ? componentExtension
                        : new SandboxedExtension(componentExtension, effectivePolicies));
            }

            Extension includeExtension = new IncludeExtension(effectiveLimits);
            delegate.extension(effectivePolicies == null
                    ? includeExtension
                    : new SandboxedExtension(includeExtension, effectivePolicies));

            Extension extendsExtension = new ExtendsExtension();
            delegate.extension(effectivePolicies == null
                    ? extendsExtension
                    : new SandboxedExtension(extendsExtension, effectivePolicies));

            if (language) {
                Extension languageExtension = new LanguageExtension(effectiveLimits);
                delegate.extension(effectivePolicies == null
                        ? languageExtension
                        : new SandboxedExtension(languageExtension, effectivePolicies));
            }

            for (Extension extension : extensions) {
                delegate.extension(effectivePolicies == null
                        ? extension
                        : new SandboxedExtension(extension, effectivePolicies));
            }

            ContextEscaping.register(delegate);

            return delegate.build();
        }

        private static int strictestOutputLimit(SandboxPolicyResolver policies) {
            int result = -1;
            for (SandboxPolicy policy : policies.policies()) {
                int limit = policy.maxRenderedChars();
                if (limit >= 0 && (result < 0 || limit < result)) {
                    result = limit;
                }
            }
            return result;
        }

        private static boolean forcesAutoEscaping(SandboxPolicyResolver policies) {
            for (SandboxPolicy policy : policies.policies()) {
                if (policy.forceAutoEscaping()) {
                    return true;
                }
            }
            return false;
        }

        private void requireSandboxSupport() {
            if (components == null) {
                return;
            }
            if (!sandbox.isTagAllowed("component")
                    || !sandbox.isTagAllowed("slot")
                    || !sandbox.isTagAllowed("props")
                    || !sandbox.isFunctionAllowed("slot")) {
                throw new IllegalArgumentException(
                        "Sandbox policy must allow the component, slot, and props tags and the slot function. "
                                + "Call SandboxPolicy.Builder.allowComponents().");
            }
        }
    }
}
