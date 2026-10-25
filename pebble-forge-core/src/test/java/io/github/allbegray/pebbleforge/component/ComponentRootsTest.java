package io.github.allbegray.pebbleforge.component;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComponentRootsTest {

    private static String render(Map<String, String> templates, String templateName,
                                 Map<String, Object> context, Components components)
            throws java.io.IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(templates))
                .components(components)
                .build();
        StringWriter writer = new StringWriter();
        engine.getTemplate(templateName).evaluate(writer, context);
        return writer.toString();
    }

    @Test
    void defaultsExposeTheDefaultRoot() {
        Components components = Components.defaults();

        assertThat(components.root()).isEqualTo("components");
        assertThat(components.roots()).isEqualTo(Map.of("", "components"));
        assertThat(components.suffix()).isEqualTo(".peb");
        assertThat(components.maxDepth()).isEqualTo(16);
        assertThat(components.maxSlotChars()).isEqualTo(-1);
        assertThatThrownBy(() -> components.roots().put("ui", "ui"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void twoNamespacesResolveTheSameComponentNameToDifferentPaths() {
        Components components = Components.builder()
                .root("ui", "lib/components")
                .root("admin", "admin/components")
                .build();

        assertThat(components.resolvePath("ui:card")).isEqualTo("lib/components/card.peb");
        assertThat(components.resolvePath("admin:card")).isEqualTo("admin/components/card.peb");
        assertThat(components.root()).isNull();
        assertThat(components.roots()).isEqualTo(Map.of(
                "ui", "lib/components",
                "admin", "admin/components"));
    }

    @Test
    void rendersTwoNamespacedCardsInOnePage() throws java.io.IOException {
        Components components = Components.builder()
                .root("ui", "ui")
                .root("admin", "admin")
                .build();

        String output = render(
                Map.of(
                        "page.peb", """
                                {% component "ui:card" with {"title": "UI"} %}{% endcomponent %}
                                {% component "admin:card" with {"title": "Admin"} %}{% endcomponent %}
                                """,
                        "ui/card.peb", "<p>ui {{ title }}</p>",
                        "admin/card.peb", "<p>admin {{ title }}</p>"),
                "page.peb",
                Map.of(),
                components);

        assertThat(output.strip()).isEqualTo("<p>ui UI</p><p>admin Admin</p>");
    }

    @Test
    void unqualifiedNamesUseTheDefaultRoot() throws java.io.IOException {
        Components components = Components.builder()
                .root("components")
                .root("ui", "ui")
                .build();

        String output = render(
                Map.of(
                        "page.peb", """
                                {% component "card" with {"title": "Default"} %}{% endcomponent %}
                                {% component "ui:card" with {"title": "Named"} %}{% endcomponent %}
                                """,
                        "components/card.peb", "<p>{{ title }}</p>",
                        "ui/card.peb", "<p>ui {{ title }}</p>"),
                "page.peb",
                Map.of(),
                components);

        assertThat(output.strip()).isEqualTo("<p>Default</p><p>ui Named</p>");
    }

    @Test
    void failsWithoutADefaultRootForAnUnqualifiedName() {
        Components components = Components.builder()
                .root("ui", "ui")
                .build();

        assertThatThrownBy(() -> components.resolvePath("card"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No default component root is configured");

        assertThatThrownBy(() -> render(
                Map.of("page.peb", "{% component \"card\" %}{% endcomponent %}"),
                "page.peb",
                Map.of(),
                components))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("No default component root is configured");
    }

    @Test
    void unknownNamespacesFailClearly() {
        assertThatThrownBy(() -> Components.defaults().resolvePath("ui:card"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown component namespace [ui]");

        assertThatThrownBy(() -> render(
                Map.of("page.peb", "{% component \"ui:card\" %}{% endcomponent %}"),
                "page.peb",
                Map.of(),
                Components.defaults()))
                .isInstanceOf(PebbleException.class)
                .hasMessageContaining("Unknown component namespace [ui]");
    }

    @Test
    void rejectsInvalidComponentNames() {
        Components components = Components.defaults();

        assertThatThrownBy(() -> components.resolvePath(""))
                .hasMessageContaining("Component name must not be empty");
        assertThatThrownBy(() -> components.resolvePath("   "))
                .hasMessageContaining("Component name must not be empty");
        assertThatThrownBy(() -> components.resolvePath(":card"))
                .hasMessageContaining("Component namespace must not be empty");
        assertThatThrownBy(() -> components.resolvePath("ui:"))
                .hasMessageContaining("Component name must not be empty after namespace [ui]");
        assertThatThrownBy(() -> components.resolvePath("ui:card:extra"))
                .hasMessageContaining("at most one ':' namespace separator");
        assertThatThrownBy(() -> components.resolvePath("../secret"))
                .hasMessageContaining("invalid segment");
        assertThatThrownBy(() -> components.resolvePath("ui:../secret"))
                .hasMessageContaining("invalid segment");
        assertThatThrownBy(() -> components.resolvePath("ui:a//b"))
                .hasMessageContaining("invalid segment");
        assertThatThrownBy(() -> components.resolvePath("ui:a\\b"))
                .hasMessageContaining("must use '/' separators");
        assertThatThrownBy(() -> components.resolvePath("/card"))
                .hasMessageContaining("must be relative");
        assertThatThrownBy(() -> components.resolvePath("ui:ca rd"))
                .hasMessageContaining("invalid character");
    }

    @Test
    void rejectsInvalidNamespaceDeclarations() {
        assertThatThrownBy(() -> Components.builder().root("1ui", "ui"))
                .hasMessageContaining("must match [A-Za-z][A-Za-z0-9_-]*");
        assertThatThrownBy(() -> Components.builder().root("ui/x", "ui"))
                .hasMessageContaining("must match [A-Za-z][A-Za-z0-9_-]*");
        assertThatThrownBy(() -> Components.builder().root("ui!", "ui"))
                .hasMessageContaining("must match [A-Za-z][A-Za-z0-9_-]*");
        assertThatThrownBy(() -> Components.builder().root("", "ui"))
                .hasMessageContaining("Component namespace must not be empty");
        assertThatThrownBy(() -> Components.builder().root("ui", "a").root("ui", "b"))
                .hasMessageContaining("Component namespace [ui] is already declared");
        assertThatThrownBy(() -> Components.builder().root("components").root("components"))
                .hasMessageContaining("Default component root is already declared");
    }

    @Test
    void rejectsDuplicateNormalizedRoots() {
        assertThatThrownBy(() -> Components.builder()
                .root("ui", "/lib/components/")
                .root("admin", "lib/components")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ui")
                .hasMessageContaining("admin")
                .hasMessageContaining("lib/components");

        assertThatThrownBy(() -> Components.builder()
                .root("components")
                .root("ui", "components/")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("(default)")
                .hasMessageContaining("ui")
                .hasMessageContaining("components");
    }

    @Test
    void normalizesEveryRoot() {
        Components components = Components.builder()
                .root("ui", " /lib/components/ ")
                .build();

        assertThat(components.roots()).isEqualTo(Map.of("ui", "lib/components"));
        assertThat(components.resolvePath("ui:card")).isEqualTo("lib/components/card.peb");

        assertThatThrownBy(() -> Components.builder().root("ui", "../lib"))
                .hasMessageContaining("Component root must be a relative directory");
        assertThatThrownBy(() -> Components.builder().root("lib\\components"))
                .hasMessageContaining("Component root must be a relative directory");
        assertThatThrownBy(() -> Components.builder().root("ui", "lib\\components"))
                .hasMessageContaining("Component root must be a relative directory");
    }

    @Test
    void withMaxSlotCharsPreservesRoots() {
        Components components = Components.builder()
                .root("components")
                .root("ui", "ui")
                .maxSlotChars(50)
                .build();

        Components updated = components.withMaxSlotChars(12);

        assertThat(updated.maxSlotChars()).isEqualTo(12);
        assertThat(components.maxSlotChars()).isEqualTo(50);
        assertThat(updated.roots()).isEqualTo(components.roots());
        assertThat(updated.root()).isEqualTo("components");
        assertThat(updated.resolvePath("card")).isEqualTo("components/card.peb");
        assertThat(updated.resolvePath("ui:card")).isEqualTo("ui/card.peb");
        assertThatThrownBy(() -> updated.withMaxSlotChars(-2))
                .hasMessageContaining("maxSlotChars must be -1 or greater");
    }

    @Test
    void rendersAComponentPackagedInAJar(@TempDir Path tempDir) throws Exception {
        Path jar = tempDir.resolve("components.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            writeEntry(out, "lib/page.peb", """
                    {% component "lib:components/jar-card" with {"name": "Jar"} %}{% endcomponent %}
                    """);
            writeEntry(out, "lib/components/jar-card.peb", "<p>Hello {{ name }}</p>");
        }

        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader jarLoader = new URLClassLoader(
                new URL[]{jar.toUri().toURL()}, previous)) {
            Thread.currentThread().setContextClassLoader(jarLoader);
            // ClasspathLoader captures its loader at construction, so pass the jar loader explicitly.
            ClasspathLoader loader = new ClasspathLoader(jarLoader);
            loader.setPrefix("lib/");
            loader.setSuffix(".peb");
            PebbleEngine engine = PebbleForge.engine()
                    .loader(loader)
                    .components(Components.builder()
                            .root("lib", "")
                            .suffix("")
                            .build())
                    .build();

            StringWriter writer = new StringWriter();
            engine.getTemplate("page").evaluate(writer, Map.of());

            assertThat(writer.toString().strip()).isEqualTo("<p>Hello Jar</p>");
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static void writeEntry(JarOutputStream out, String name, String content)
            throws java.io.IOException {
        out.putNextEntry(new JarEntry(name));
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}
