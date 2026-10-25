package io.github.allbegray.pebbleforge.benchmarks;

import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Shared templates and model data for the JMH benchmarks.
 */
final class BenchTemplates {

    static final Map<String, Object> MODEL = Map.of(
            "title", "Forge benchmarks",
            "items", List.of("alpha", "beta", "gamma", "delta"),
            "message", "<strong>hello</strong>",
            "role", "owner",
            "numbers", List.of(1, 2, 3, 4, 5, 6, 7, 8));

    private BenchTemplates() {
    }

    static ClasspathLoader loader() {
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix("bench/");
        loader.setSuffix(".peb");
        return loader;
    }

    static String render(PebbleTemplate template) throws IOException {
        StringWriter writer = new StringWriter(256);
        template.evaluate(writer, MODEL);
        return writer.toString();
    }
}
