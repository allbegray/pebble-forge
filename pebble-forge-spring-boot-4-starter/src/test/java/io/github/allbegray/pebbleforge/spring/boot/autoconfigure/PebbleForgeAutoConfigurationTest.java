package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PebbleForgeAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PebbleForgeAutoConfiguration.class))
            .withUserConfiguration(MessageSourceConfiguration.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rendersComponentsThroughTheBootEngine() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(PebbleEngine.class);
            assertThat(render(context.getBean(PebbleEngine.class), "page", Map.of()).strip())
                    .isEqualTo("<p>Hello Ada</p>");
        });
    }

    @Test
    void replacesTheOfficialPebbleEngine() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        PebbleForgeAutoConfiguration.class,
                        io.pebbletemplates.boot.autoconfigure.PebbleAutoConfiguration.class))
                .withUserConfiguration(MessageSourceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(PebbleEngine.class);
                    assertThat(render(context.getBean(PebbleEngine.class), "page", Map.of()).strip())
                            .isEqualTo("<p>Hello Ada</p>");
                });
    }

    @Test
    void appliesTheSandboxWhenEnabled() {
        runner.withPropertyValues("pebble.forge.sandbox.enabled=true").run(context -> {
            assertThat(render(context.getBean(PebbleEngine.class), "escape",
                    Map.of("value", "<b>x</b>")).strip())
                    .isEqualTo("<p>&lt;b&gt;x&lt;/b&gt;</p>");

            assertThatThrownBy(() -> render(context.getBean(PebbleEngine.class), "raw",
                    Map.of("value", "<b>x</b>")))
                    .isInstanceOf(PebbleException.class)
                    .hasMessageContaining("raw");
        });
    }

    @Test
    void exposesSpringSecurityFunctionsInsideTheSandbox() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("ada", "n/a", "ROLE_ADMIN"));

        runner.withPropertyValues("pebble.forge.sandbox.enabled=true").run(context ->
                assertThat(render(context.getBean(PebbleEngine.class), "security", Map.of()).strip())
                        .isEqualTo("ada"));
    }

    @Test
    void letsAUserPolicyReplaceThePropertyPolicy() {
        runner.withPropertyValues("pebble.forge.sandbox.enabled=true")
                .withUserConfiguration(StrictPolicyConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SandboxPolicy.class);
                    assertThatThrownBy(() -> render(context.getBean(PebbleEngine.class), "security",
                            Map.of()))
                            .isInstanceOf(PebbleException.class)
                            .hasMessageContaining("username");
                });
    }

    @Test
    void canDisableTheSecurityExtension() {
        runner.withPropertyValues("pebble.forge.security.enabled=false").run(context ->
                assertThat(context).doesNotHaveBean("springSecurityExtension"));
    }

    private static String render(PebbleEngine engine, String template, Map<String, Object> model)
            throws java.io.IOException {
        StringWriter writer = new StringWriter();
        engine.getTemplate(template).evaluate(writer, model);
        return writer.toString();
    }

    @Configuration(proxyBeanMethods = false)
    static class MessageSourceConfiguration {

        @Bean
        MessageSource messageSource() {
            return new StaticMessageSource();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class StrictPolicyConfiguration {

        @Bean
        SandboxPolicy sandboxPolicy() {
            return SandboxPolicy.strict();
        }
    }
}
