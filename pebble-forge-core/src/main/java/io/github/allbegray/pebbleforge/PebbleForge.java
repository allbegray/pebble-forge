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
 * Builds a PebbleEngine with the optional Pebble Forge extensions.
 */
public final class PebbleForge {

    private PebbleForge() {
    }

    public static Builder engine() {
        return new Builder();
    }

    public static final class Builder {

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

        public Builder loader(Loader<?> loader) {
            this.loader = Objects.requireNonNull(loader, "loader");
            return this;
        }

        public Builder components(Components components) {
            this.components = Objects.requireNonNull(components, "components");
            return this;
        }

        public Builder sandbox(SandboxPolicy sandbox) {
            this.sandbox = Objects.requireNonNull(sandbox, "sandbox");
            return this;
        }

        public Builder policies(SandboxPolicyResolver policies) {
            this.policies = Objects.requireNonNull(policies, "policies");
            return this;
        }

        public Builder limitsResolver(SandboxLimitsResolver limitsResolver) {
            this.limitsResolver = Objects.requireNonNull(limitsResolver, "limitsResolver");
            return this;
        }

        public Builder languageExtensions(boolean enabled) {
            this.language = enabled;
            return this;
        }

        public Builder devMode(boolean devMode) {
            this.devMode = devMode;
            return this;
        }

        public Builder extension(Extension... extensions) {
            for (Extension extension : extensions) {
                this.extensions.add(Objects.requireNonNull(extension, "extension"));
            }
            return this;
        }

        public Builder strictVariables(boolean strictVariables) {
            this.strictVariables = strictVariables;
            return this;
        }

        public Builder autoEscaping(boolean autoEscaping) {
            this.autoEscaping = autoEscaping;
            return this;
        }

        public Builder maxRenderedChars(int maxRenderedChars) {
            if (maxRenderedChars < -1) {
                throw new IllegalArgumentException("maxRenderedChars must be -1 or greater");
            }
            this.maxRenderedChars = maxRenderedChars;
            return this;
        }

        public Builder cacheActive(boolean cacheActive) {
            this.cacheActive = cacheActive;
            return this;
        }

        public Builder defaultLocale(Locale defaultLocale) {
            this.defaultLocale = defaultLocale;
            return this;
        }

        public Builder greedyMatchMethod(boolean greedyMatchMethod) {
            this.greedyMatchMethod = greedyMatchMethod;
            return this;
        }

        public Builder methodAccessValidator(MethodAccessValidator methodAccessValidator) {
            this.methodAccessValidator = Objects.requireNonNull(methodAccessValidator, "methodAccessValidator");
            return this;
        }

        public Builder executorService(ExecutorService executorService) {
            this.executorService = Objects.requireNonNull(executorService, "executorService");
            return this;
        }

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
