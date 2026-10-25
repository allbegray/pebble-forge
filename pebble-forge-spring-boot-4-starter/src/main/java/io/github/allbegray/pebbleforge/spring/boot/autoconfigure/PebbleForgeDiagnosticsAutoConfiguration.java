package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.diagnostics.TemplateDiagnostics;
import io.pebbletemplates.boot.autoconfigure.PebbleAutoConfiguration;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.spring.servlet.PebbleViewResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;

/**
 * Wires Pebble render diagnostics into Spring MVC error handling.
 */
@AutoConfiguration(
        after = PebbleAutoConfiguration.class,
        beforeName = "org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({PebbleViewResolver.class, TemplateDiagnostics.class})
@EnableConfigurationProperties(PebbleForgeProperties.class)
public class PebbleForgeDiagnosticsAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = "pebbleLoader")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "pebble.forge.diagnostics", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public TemplateDiagnostics pebbleDiagnostics(
            @Qualifier("pebbleLoader") Loader<?> pebbleLoader,
            PebbleForgeProperties properties) {
        return TemplateDiagnostics.builder()
                .loader(pebbleLoader)
                .contextLines(properties.getDiagnostics().getContextLines())
                .build();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(name = "pebbleViewResolver")
    static class ViewResolverConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "pebbleDiagnosticsViewResolver")
        public ViewResolver pebbleDiagnosticsViewResolver(
                @Qualifier("pebbleViewResolver") ViewResolver pebbleViewResolver,
                ObjectProvider<TemplateDiagnostics> pebbleDiagnostics) {
            return new PebbleDiagnosticsViewResolver(pebbleViewResolver,
                    pebbleDiagnostics.getIfAvailable());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ErrorAttributes.class, DefaultErrorAttributes.class})
    @ConditionalOnProperty(prefix = "pebble.forge.diagnostics", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    static class ErrorAttributesConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = "pebble.forge.diagnostics",
                name = "expose-in-error-attributes", havingValue = "true")
        @ConditionalOnMissingBean(ErrorAttributes.class)
        public ErrorAttributes errorAttributes() {
            return new PebbleDiagnosticsErrorAttributes();
        }
    }
}
