package io.github.allbegray.pebbleforge;

import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.component.ComponentsExtension;
import io.github.allbegray.pebbleforge.language.LanguageExtension;
import io.github.allbegray.pebbleforge.sandbox.SandboxAttributeExtension;
import io.github.allbegray.pebbleforge.sandbox.SandboxExtensionCustomizer;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
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
        private boolean language = true;
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

        public Builder languageExtensions(boolean enabled) {
            this.language = enabled;
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
            if (sandbox != null) {
                requireSandboxSupport();
            }

            Loader<?> effectiveLoader = loader == null ? new ClasspathLoader() : loader;
            if (sandbox != null) {
                effectiveLoader = new SandboxedLoader<>(effectiveLoader, sandbox);
                delegate.methodAccessValidator(sandbox.methodAccessValidator());
                delegate.maxRenderedSize(sandbox.maxRenderedChars());
                delegate.registerExtensionCustomizer(
                        extension -> new SandboxExtensionCustomizer(extension, sandbox));
                delegate.extension(new SandboxAttributeExtension(sandbox));
            } else if (maxRenderedChars >= 0) {
                delegate.maxRenderedSize(maxRenderedChars);
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
            if (methodAccessValidator != null && sandbox == null) {
                delegate.methodAccessValidator(methodAccessValidator);
            }

            if (sandbox != null && sandbox.forceAutoEscaping()) {
                delegate.autoEscaping(true);
            } else if (autoEscaping != null) {
                delegate.autoEscaping(autoEscaping);
            }

            if (components != null) {
                Extension componentExtension = new ComponentsExtension(components);
                delegate.extension(sandbox == null
                        ? componentExtension
                        : new SandboxedExtension(componentExtension, sandbox));
            }

            if (language) {
                Extension languageExtension = new LanguageExtension();
                delegate.extension(sandbox == null
                        ? languageExtension
                        : new SandboxedExtension(languageExtension, sandbox));
            }

            for (Extension extension : extensions) {
                delegate.extension(sandbox == null
                        ? extension
                        : new SandboxedExtension(extension, sandbox));
            }

            return delegate.build();
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
