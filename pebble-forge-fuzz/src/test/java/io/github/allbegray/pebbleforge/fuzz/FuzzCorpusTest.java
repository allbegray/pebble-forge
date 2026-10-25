package io.github.allbegray.pebbleforge.fuzz;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.fuzz.support.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class FuzzCorpusTest {

    private static final Map<String, Object> CONTEXT = Map.of(
            "name", "forge",
            "items", List.of(1, 2, 3),
            "ok", true);

    @TestFactory
    Stream<DynamicTest> validCorpusEntriesParseAndEvaluate() throws IOException {
        return corpusNames("valid").stream()
                .map(name -> dynamicTest("valid: " + name, () ->
                        assertThatCode(() -> parseAndEvaluate(corpusEntry("valid", name)))
                                .doesNotThrowAnyException()));
    }

    @TestFactory
    Stream<DynamicTest> invalidCorpusEntriesRaisePebbleException() throws IOException {
        return corpusNames("invalid").stream()
                .map(name -> dynamicTest("invalid: " + name, () ->
                        assertThatThrownBy(() -> parseAndEvaluate(corpusEntry("invalid", name)))
                                .isInstanceOf(PebbleException.class)));
    }

    private static List<String> corpusNames(String kind) throws IOException {
        URL directory = FuzzCorpusTest.class.getResource("/fuzz-corpus/" + kind);
        Objects.requireNonNull(directory, "Missing corpus directory: " + kind);
        try (Stream<Path> entries = Files.list(Path.of(directory.toURI()))) {
            return entries
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".peb"))
                    .sorted()
                    .toList();
        } catch (URISyntaxException exception) {
            throw new IOException("Could not resolve corpus directory: " + kind, exception);
        }
    }

    private static String corpusEntry(String kind, String name) throws IOException {
        String resource = "/fuzz-corpus/" + kind + "/" + name;
        try (InputStream input = FuzzCorpusTest.class.getResourceAsStream(resource)) {
            Objects.requireNonNull(input, "Missing corpus entry: " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void parseAndEvaluate(String source) throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("corpus.peb", source)))
                .build();
        engine.getTemplate("corpus.peb").evaluate(new StringWriter(), CONTEXT);
    }
}
