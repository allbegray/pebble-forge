package io.github.allbegray.pebbleforge.demo;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.loader.ClasspathLoader;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public final class Demo {

    public static void main(String[] args) throws Exception {
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix("templates/");
        loader.setSuffix(".peb");

        PebbleEngine engine = PebbleForge.engine()
                .loader(loader)
                .components(Components.builder()
                        .root("components")
                        .suffix("")
                        .maxDepth(8)
                        .build())
                .sandbox(SandboxPolicy.builder()
                        .allowComponents()
                        .allowTags("extends", "block")
                        .allowFilters("escape", "length", "upper")
                        .maxRenderedChars(200_000)
                        .build())
                .build();

        Map<String, Object> page = Map.of(
                "title", "Pebble Forge",
                "summary", "Components, slots, and a sandbox on top of Pebble.",
                "body", "<strong>This text is escaped by the sandbox.</strong>",
                "footer", "Rendered through a template-defined component.");
        Map<String, Object> model = Map.of("page", page);

        System.out.println("=== rendered page ===");
        System.out.println(render(engine, "page", model));

        System.out.println("=== sandbox blocked raw output ===");
        try {
            render(engine, "raw-demo", model);
            System.out.println("ERROR: raw output was allowed");
        } catch (PebbleException e) {
            System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        System.out.println("=== language extensions ===");
        System.out.println(render(engine, "language-demo",
                Map.of("numbers", List.of(1, 2, 3, 4, 5, 6))));
    }

    private static String render(PebbleEngine engine, String template, Map<String, Object> model)
            throws Exception {
        StringWriter writer = new StringWriter();
        engine.getTemplate(template).evaluate(writer, model);
        return writer.toString();
    }
}
