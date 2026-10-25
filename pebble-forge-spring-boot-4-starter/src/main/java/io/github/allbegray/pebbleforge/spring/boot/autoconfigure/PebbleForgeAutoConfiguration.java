package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.sandbox.SandboxLimitsResolver;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.github.allbegray.pebbleforge.spring.SpringWebFunctions;
import io.github.allbegray.pebbleforge.spring.security.SpringSecurityExtension;
import io.github.allbegray.pebbleforge.spring.security.SpringSecurityFunctions;
import io.pebbletemplates.boot.autoconfigure.PebbleAutoConfiguration;
import io.pebbletemplates.boot.autoconfigure.PebbleProperties;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.attributes.methodaccess.MethodAccessValidator;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.spring.extension.SpringExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;

import java.util.List;

/**
 * Replaces Pebble's engine bean with one built by Pebble Forge.
 */
@AutoConfiguration(before = PebbleAutoConfiguration.class)
@ConditionalOnClass(PebbleEngine.class)
@EnableConfigurationProperties({PebbleProperties.class, PebbleForgeProperties.class})
public class PebbleForgeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "pebbleLoader")
    public Loader<?> pebbleLoader(PebbleProperties properties) {
        ClasspathLoader loader = new ClasspathLoader();
        loader.setCharset(properties.getCharsetName());
        loader.setPrefix(stripLeadingSlash(properties.getPrefix()));
        loader.setSuffix(properties.getSuffix());
        return loader;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringExtension springExtension(ObjectProvider<MessageSource> messageSource) {
        MessageSource source = messageSource.getIfAvailable(StaticMessageSource::new);
        return new SpringExtension(source);
    }

    @Bean
    @ConditionalOnMissingBean(name = "pebbleEngine")
    public PebbleEngine pebbleEngine(
            PebbleProperties properties,
            Loader<?> pebbleLoader,
            List<Extension> extensions,
            ObjectProvider<MethodAccessValidator> methodAccessValidator,
            ObjectProvider<SandboxPolicy> sandboxPolicy,
            ObjectProvider<SandboxLimitsResolver> limitsResolver,
            PebbleForgeProperties forgeProperties) {

        PebbleForge.Builder builder = PebbleForge.engine()
                .loader(pebbleLoader)
                .cacheActive(properties.getServlet().isCache())
                .strictVariables(properties.isStrictVariables())
                .greedyMatchMethod(properties.isGreedyMatchMethod())
                .languageExtensions(forgeProperties.getLanguage().isEnabled())
                .devMode(forgeProperties.isDevMode());

        if (pebbleLoader instanceof ClasspathLoader) {
            // The official view resolver only strips the leading slash when it sees a raw
            // ClasspathLoader. Sandboxing wraps the loader, so normalize the property once here.
            properties.setPrefix(stripLeadingSlash(properties.getPrefix()));
        }

        if (properties.getDefaultLocale() != null) {
            builder.defaultLocale(properties.getDefaultLocale());
        }
        methodAccessValidator.ifAvailable(builder::methodAccessValidator);
        limitsResolver.ifAvailable(builder::limitsResolver);

        if (!extensions.isEmpty()) {
            builder.extension(extensions.toArray(Extension[]::new));
        }

        PebbleForgeProperties.Components components = forgeProperties.getComponents();
        if (components.isEnabled()) {
            builder.components(Components.builder()
                    .root(components.getRoot())
                    .suffix(components.getSuffix())
                    .maxDepth(components.getMaxDepth())
                    .build());
        }

        SandboxPolicy policy = sandboxPolicy.getIfAvailable();
        if (policy == null && forgeProperties.getSandbox().isEnabled()) {
            policy = buildSandboxPolicy(forgeProperties);
        }
        if (policy != null) {
            builder.sandbox(policy);
        }

        return builder.build();
    }

    private static SandboxPolicy buildSandboxPolicy(PebbleForgeProperties properties) {
        PebbleForgeProperties.Sandbox sandbox = properties.getSandbox();
        SandboxPolicy.Builder builder = SandboxPolicy.builder()
                .maxRenderedChars(sandbox.getMaxRenderedChars())
                .maxLoopIterations(sandbox.getMaxLoopIterations())
                .maxTotalIterations(sandbox.getMaxTotalIterations())
                .maxIncludeDepth(sandbox.getMaxIncludeDepth())
                .maxSlotChars(sandbox.getMaxSlotChars())
                .forceAutoEscaping(sandbox.isForceAutoEscaping())
                .allowComponents()
                .allowTags("extends", "block");
        SpringWebFunctions.allow(builder);
        if (properties.getSecurity().isEnabled()) {
            SpringSecurityFunctions.allow(builder);
        }
        if (sandbox.isAllowRawOutput()) {
            builder.allowRawOutput();
        }
        return builder.build();
    }

    private static String stripLeadingSlash(String value) {
        if (value == null) {
            return null;
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
            "org.springframework.security.core.context.SecurityContextHolder",
            "org.springframework.security.web.csrf.CsrfToken",
            "org.springframework.web.context.request.RequestContextHolder",
            "jakarta.servlet.http.HttpServletRequest"
    })
    @ConditionalOnProperty(prefix = "pebble.forge.security", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    static class SpringSecurityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SpringSecurityExtension springSecurityExtension() {
            return new SpringSecurityExtension();
        }
    }
}
