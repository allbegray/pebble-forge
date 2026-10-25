package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.error.LoaderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SandboxedFileLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsAFileInsideTheRoot() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("root"));
        Files.writeString(root.resolve("page.peb"), "hello");

        assertThat(read(root, "page")).isEqualTo("hello");
    }

    @Test
    void rejectsParentTraversal() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("root"));
        SandboxedFileLoader loader = loader(root);

        assertThatThrownBy(() -> loader.getReader("../page"))
                .isInstanceOf(LoaderException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    void rejectsAbsolutePaths() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("root"));
        SandboxedFileLoader loader = loader(root);

        assertThatThrownBy(() -> loader.getReader("/etc/passwd"))
                .isInstanceOf(LoaderException.class)
                .hasMessageContaining("Absolute");
    }

    @Test
    void rejectsASymlinkOutsideTheRoot() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("root"));
        Path outside = Files.createDirectories(tempDir.resolve("outside"));
        Path secret = outside.resolve("secret.peb");
        Files.writeString(secret, "secret");
        Files.createSymbolicLink(root.resolve("link.peb"), secret);

        SandboxedFileLoader loader = loader(root);

        assertThatThrownBy(() -> loader.getReader("link"))
                .isInstanceOf(LoaderException.class)
                .hasMessageContaining("outside the loader root");
    }

    @Test
    void allowsASymlinkInsideTheRoot() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("root"));
        Files.writeString(root.resolve("target.peb"), "inside");
        Files.createSymbolicLink(root.resolve("link.peb"), root.resolve("target.peb"));

        assertThat(read(root, "link")).isEqualTo("inside");
    }

    @Test
    void resourceExistsRejectsASymlinkOutsideTheRoot() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("root"));
        Path outside = Files.createDirectories(tempDir.resolve("outside"));
        Path secret = outside.resolve("secret.peb");
        Files.writeString(secret, "secret");
        Files.createSymbolicLink(root.resolve("link.peb"), secret);

        assertThat(loader(root).resourceExists("link")).isFalse();
    }

    private static String read(Path root, String name) throws IOException {
        SandboxedFileLoader loader = loader(root);
        try (Reader reader = loader.getReader(name)) {
            StringWriter writer = new StringWriter();
            reader.transferTo(writer);
            return writer.toString();
        }
    }

    private static SandboxedFileLoader loader(Path root) {
        SandboxedFileLoader loader = new SandboxedFileLoader(root);
        loader.setSuffix(".peb");
        return loader;
    }
}
