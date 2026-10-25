package io.github.allbegray.pebbleforge.fuzz;

import io.github.allbegray.pebbleforge.fuzz.support.FuzzText;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.github.allbegray.pebbleforge.sandbox.SandboxedFileLoader;
import io.github.allbegray.pebbleforge.sandbox.SandboxedLoader;
import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.loader.Loader;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

class PathFuzzTest {

    private static final String VISIBLE_CONTENT = "VISIBLE";
    private static final String SECRET_CONTENT = "OUTSIDE_SECRET";

    private static Path root;
    private static Path outsideSecret;
    private static SandboxedFileLoader fileLoader;
    private static SandboxedLoader<String> sandboxedLoader;

    private static synchronized void setUp() throws IOException {
        if (fileLoader != null) {
            return;
        }
        Path tempDir = Files.createTempDirectory("pebble-forge-path-fuzz");
        root = Files.createDirectories(tempDir.resolve("root"));
        Files.writeString(root.resolve("visible.peb"), VISIBLE_CONTENT);

        Path outside = Files.createDirectories(tempDir.resolve("outside"));
        outsideSecret = outside.resolve("secret.peb");
        Files.writeString(outsideSecret, SECRET_CONTENT);
        Files.writeString(tempDir.resolve("secret.peb"), SECRET_CONTENT);

        fileLoader = new SandboxedFileLoader(root);
        fileLoader.setSuffix(".peb");

        SandboxPolicy policy = SandboxPolicy.builder()
                .allowOnlyTags("if")
                .allowOnlyFilters("escape")
                .allowOnlyFunctions("slot")
                .allowOnlyTests("empty")
                .build();
        sandboxedLoader = new SandboxedLoader<>(fileLoader, policy);

        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.forEach(path -> path.toFile().deleteOnExit());
        }
    }

    @Property(tries = 100)
    void candidateNamesStayInsideTheRoot(@ForAll("hostileNames") String name) throws IOException {
        setUp();
        assertStaysInside(sandboxedLoader, name);
        assertStaysInside(fileLoader, name);
    }

    @Provide
    Arbitrary<String> hostileNames() {
        Arbitrary<String> segments = Arbitraries.of(
                "..", ".", "", "visible", "visible.peb", "outside", "secret", "secret.peb",
                "a", "a.b", "%2e%2e", "%2E%2E", "%2f", "%5c", "...", "....", "~", "$HOME");
        Arbitrary<String> joined = Combinators.combine(segments, segments, segments, segments)
                .as((first, second, third, fourth) ->
                        first + "/" + second + "/" + third + "/" + fourth);

        Arbitrary<String> raw = Arbitraries.strings()
                .withChars("..//\\\\\0\r\n\t%:{}<>\"';&= abcXYZ019é中")
                .ofMaxLength(40);
        Arbitrary<String> prefixed = Arbitraries.of(
                        "", "/", "./", "../", "..\\", "C:\\", "\\\\", "visible/", "/visible/")
                .flatMap(prefix -> raw.map(rest -> prefix + rest));

        return Arbitraries.oneOf(joined, raw, prefixed);
    }

    @Test
    void traversalAndHostileNamesAreRejected() throws IOException {
        setUp();
        List<String> rejected = List.of(
                "../outside/secret",
                "..\\outside\\secret",
                "visible/../../outside/secret",
                "visible/../outside/secret",
                "/etc/passwd",
                "C:\\Windows\\win.ini",
                "\\\\server\\share\\secret",
                "visible\0.peb",
                "%2e%2e%2foutside%2fsecret",
                "visible//",
                "./visible",
                "visible/.",
                "~/secret",
                "....//outside/secret",
                "visible\0",
                "visible\n",
                "visible\uFF0F..\uFF0Foutside\uFF0Fsecret",
                "a".repeat(300));

        for (String name : rejected) {
            assertThatThrownBy(() -> sandboxedLoader.getReader(name))
                    .as("sandboxed loader for [%s]", FuzzText.escape(name))
                    .isInstanceOf(LoaderException.class);
            assertThatThrownBy(() -> fileLoader.getReader(name))
                    .as("file loader for [%s]", FuzzText.escape(name))
                    .isInstanceOf(LoaderException.class);
        }
    }

    @Test
    void visibleTemplateIsReadableThroughBothLoaders() throws IOException {
        setUp();
        assertThat(read(sandboxedLoader, "visible")).isEqualTo(VISIBLE_CONTENT);
        assertThat(read(fileLoader, "visible")).isEqualTo(VISIBLE_CONTENT);
    }

    @Test
    void symlinkOutsideTheRootIsRejected() throws IOException {
        setUp();
        Path link = root.resolve("link.peb");
        try {
            Files.createSymbolicLink(link, outsideSecret);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Assumptions.abort("Symlinks are unavailable: " + exception.getMessage());
        }
        link.toFile().deleteOnExit();

        assertThatThrownBy(() -> fileLoader.getReader("link"))
                .isInstanceOf(LoaderException.class)
                .hasMessageContaining("outside the loader root");
        assertThatThrownBy(() -> sandboxedLoader.getReader("link"))
                .isInstanceOf(LoaderException.class);
    }

    private static void assertStaysInside(Loader<String> loader, String name) {
        try (Reader reader = loader.getReader(name)) {
            assertThat(readAll(reader))
                    .as("content for [%s]", FuzzText.escape(name))
                    .isEqualTo(VISIBLE_CONTENT)
                    .doesNotContain(SECRET_CONTENT);
        } catch (LoaderException expected) {
            // Rejected before the delegate can open a file.
        } catch (IOException exception) {
            fail("Unexpected IOException for [" + FuzzText.escape(name) + "]", exception);
        } catch (RuntimeException exception) {
            fail("Unexpected " + exception.getClass().getName()
                    + " for [" + FuzzText.escape(name) + "]", exception);
        }
    }

    private static String read(Loader<String> loader, String name) throws IOException {
        try (Reader reader = loader.getReader(name)) {
            return readAll(reader);
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        reader.transferTo(writer);
        return writer.toString();
    }
}
