package io.github.allbegray.pebbleforge.reload;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.FileLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReloadingLoaderTest {

    @TempDir
    Path templates;

    @Test
    void reloadsAChangedTemplateInDevMode() throws Exception {
        write("page.peb", "one");
        PebbleEngine engine = engine(true, false);

        assertThat(render(engine, "page")).isEqualTo("one");

        write("page.peb", "two");

        assertThat(render(engine, "page")).isEqualTo("two");
    }

    @Test
    void keepsTheCachedTemplateWithoutDevMode() throws Exception {
        write("page.peb", "one");
        PebbleEngine engine = engine(false, false);

        assertThat(render(engine, "page")).isEqualTo("one");

        write("page.peb", "two");

        assertThat(render(engine, "page")).isEqualTo("one");
    }

    @Test
    void reloadsAnIncludedTemplate() throws Exception {
        write("page.peb", "before {% include \"card\" %} after");
        write("card.peb", "one");
        PebbleEngine engine = engine(true, false);

        assertThat(render(engine, "page")).isEqualTo("before one after");

        write("card.peb", "two");

        assertThat(render(engine, "page")).isEqualTo("before two after");
    }

    @Test
    void reloadsInsideTheSandbox() throws Exception {
        write("page.peb", "one");
        PebbleEngine engine = engine(true, true);

        assertThat(render(engine, "page")).isEqualTo("one");

        write("page.peb", "two");

        assertThat(render(engine, "page")).isEqualTo("two");
    }

    private PebbleEngine engine(boolean devMode, boolean sandbox) {
        FileLoader loader = new FileLoader(templates.toAbsolutePath().toString());
        loader.setSuffix(".peb");

        PebbleForge.Builder builder = PebbleForge.engine()
                .loader(loader)
                .devMode(devMode);
        if (sandbox) {
            builder.sandbox(SandboxPolicy.strict());
        }
        return builder.build();
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(templates.resolve(name), content);
    }

    private static String render(PebbleEngine engine, String template) throws IOException {
        StringWriter writer = new StringWriter();
        engine.getTemplate(template).evaluate(writer, Map.of());
        return writer.toString();
    }
}
