package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.diagnostics.DiagnosticReport;
import io.github.allbegray.pebbleforge.diagnostics.TemplateDiagnostics;
import io.pebbletemplates.boot.autoconfigure.PebbleAutoConfiguration;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.spring.servlet.PebbleViewResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PebbleDiagnosticsAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PebbleForgeAutoConfiguration.class,
                    PebbleForgeDiagnosticsAutoConfiguration.class,
                    PebbleAutoConfiguration.class))
            .withUserConfiguration(MessageSourceConfiguration.class)
            .withPropertyValues("pebble.strict-variables=true");

    @Test
    void usesTheRawLoaderWithSandboxAndDevMode() {
        runner.withPropertyValues(
                "pebble.forge.sandbox.enabled=true",
                "pebble.forge.dev-mode=true").run(context -> {
            Loader<?> rawLoader = (Loader<?>) context.getBean("pebbleLoader");
            assertThat(rawLoader).isInstanceOf(ClasspathLoader.class);
            assertThat(context.getBean(PebbleEngine.class).getLoader()).isNotSameAs(rawLoader);

            TemplateDiagnostics diagnostics = context.getBean(TemplateDiagnostics.class);
            assertThat(ReflectionTestUtils.getField(diagnostics, "loader")).isSameAs(rawLoader);

            DiagnosticReport report = diagnostics.render(renderError(context.getBean(PebbleEngine.class)));
            assertThat(report.frames()).hasSize(2);
            assertThat(report.format())
                    .contains("broken-view:1 includes components/broken")
                    .contains("> 1 | {{ missing }}");
        });
    }

    @Test
    void keepsAndWrapsTheOfficialViewResolver() {
        runner.run(context -> {
            assertThat(context.getBean("pebbleViewResolver"))
                    .isInstanceOf(PebbleViewResolver.class);
            assertThat(context.getBean("pebbleDiagnosticsViewResolver"))
                    .isInstanceOf(PebbleDiagnosticsViewResolver.class);

            PebbleViewResolver official = context.getBean(PebbleViewResolver.class);
            PebbleDiagnosticsViewResolver diagnostics =
                    context.getBean(PebbleDiagnosticsViewResolver.class);
            assertThat(diagnostics.getOrder()).isEqualTo(official.getOrder() - 1);
        });
    }

    @Test
    void bindsDiagnosticsProperties() {
        runner.withPropertyValues(
                "pebble.forge.diagnostics.context-lines=0",
                "pebble.forge.diagnostics.expose-in-error-attributes=true").run(context -> {
            PebbleForgeProperties.Diagnostics diagnostics =
                    context.getBean(PebbleForgeProperties.class).getDiagnostics();
            assertThat(diagnostics.isEnabled()).isTrue();
            assertThat(diagnostics.getContextLines()).isZero();
            assertThat(diagnostics.isExposeInErrorAttributes()).isTrue();
        });
    }

    @Test
    void capturesNoDiagnosticsWhenDisabled() {
        runner.withPropertyValues(
                "pebble.forge.diagnostics.enabled=false",
                "pebble.forge.diagnostics.expose-in-error-attributes=true").run(context -> {
            assertThat(context).doesNotHaveBean(TemplateDiagnostics.class);
            assertThat(context).doesNotHaveBean(ErrorAttributes.class);
            assertThat(context).hasBean("pebbleDiagnosticsViewResolver");
        });
    }

    @Test
    void exposesErrorAttributesOnlyWhenAllowed() {
        runner.withPropertyValues("pebble.forge.diagnostics.expose-in-error-attributes=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ErrorAttributes.class);
                    assertThat(context.getBean(ErrorAttributes.class))
                            .isInstanceOf(PebbleDiagnosticsErrorAttributes.class);
                });

        runner.run(context -> assertThat(context).doesNotHaveBean(ErrorAttributes.class));
    }

    @Test
    void applicationErrorAttributesWin() {
        runner.withPropertyValues("pebble.forge.diagnostics.expose-in-error-attributes=true")
                .withUserConfiguration(ApplicationErrorAttributesConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ErrorAttributes.class);
                    assertThat(context.getBean(ErrorAttributes.class))
                            .isSameAs(context.getBean("applicationErrorAttributes"));
                });
    }

    private static PebbleException renderError(PebbleEngine engine) {
        PebbleException error = catchThrowableOfType(PebbleException.class,
                () -> engine.getTemplate("broken-view").evaluate(new StringWriter()));
        assertThat(error).isNotNull();
        return error;
    }

    @Configuration(proxyBeanMethods = false)
    static class MessageSourceConfiguration {

        @Bean
        MessageSource messageSource() {
            return new StaticMessageSource();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ApplicationErrorAttributesConfiguration {

        @Bean
        ErrorAttributes applicationErrorAttributes() {
            return new DefaultErrorAttributes();
        }
    }
}
