package io.github.allbegray.pebbleforge.benchmarks;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.pebbletemplates.pebble.PebbleEngine;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests that keep the measured templates rendering the intended features.
 */
class BenchmarkTemplatesTest {

    @Test
    void rawPebbleRendersThePlainTemplate() throws IOException {
        PebbleEngine engine = new PebbleEngine.Builder()
                .loader(BenchTemplates.loader())
                .build();

        assertThat(render(engine, "plain"))
                .contains("<h1>Forge benchmarks</h1>")
                .contains("<li>alpha</li>", "<li>beta</li>", "<li>gamma</li>", "<li>delta</li>");
    }

    @Test
    void forgeRendersThePlainTemplate() throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(BenchTemplates.loader())
                .build();

        assertThat(render(engine, "plain"))
                .contains("<h1>Forge benchmarks</h1>")
                .contains("<li>alpha</li>", "<li>beta</li>", "<li>gamma</li>", "<li>delta</li>");
    }

    @Test
    void componentTemplateRendersPropsAndTheNamedSlot() throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(BenchTemplates.loader())
                .components(Components.builder().suffix("").build())
                .build();

        assertThat(render(engine, "component-page"))
                .contains("<article class=\"card\">")
                .contains("<h2>Forge benchmarks</h2>")
                .contains("<span>4</span>")
                .contains("<p>&lt;strong&gt;hello&lt;/strong&gt;</p>");
    }

    @Test
    void sandboxTemplateRendersComponentsInheritanceLanguageAndEscaping() throws IOException {
        SandboxPolicy policy = SandboxPolicy.builder()
                .allowComponents()
                .allowTags("extends", "block")
                .build();
        PebbleEngine engine = PebbleForge.engine()
                .loader(BenchTemplates.loader())
                .components(Components.builder().suffix("").build())
                .sandbox(policy)
                .build();

        assertThat(render(engine, "sandbox-page"))
                .contains("<!doctype html><main>")
                .contains("<h2>Forge benchmarks</h2>")
                .contains("<p>&lt;strong&gt;hello&lt;/strong&gt;</p>")
                .contains("A</main>")
                .doesNotContain("<strong>hello</strong>");
    }

    @Test
    void languageTemplateRendersSwitchWithForBreakAndContinue() throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(BenchTemplates.loader())
                .build();

        assertThat(render(engine, "language"))
                .contains("A")
                .contains("1;3;4;5;");
    }

    private static String render(PebbleEngine engine, String templateName) throws IOException {
        return BenchTemplates.render(engine.getTemplate(templateName));
    }
}
