# Template diagnostics

Pebble errors carry a template name and line number. They do not show the source line, the caret, or the include path. The diagnostics module adds those.

## Setup

```java
ClasspathLoader loader = new ClasspathLoader();
loader.setPrefix("templates/");
loader.setSuffix(".peb");

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .extension(new DiagnosticsExtension())
        .build();

TemplateDiagnostics diagnostics = TemplateDiagnostics.builder()
        .loader(loader)
        .contextLines(2)
        .build();

try {
    engine.getTemplate("page").evaluate(writer, model);
} catch (PebbleException error) {
    System.err.println(diagnostics.render(error).format());
}
```

`DiagnosticsExtension` replaces the include tag with a version that records the include path. Without it, the report still shows the failing template source, but not the include chain.

## Example

```text
Root attribute [missing] does not exist (components/card.peb:2)
page.peb:3 includes components/card.peb
  1 | first
  2 | second
> 3 | {% include "components/card.peb" %}
    | ^
components/card.peb:2
  1 | card
> 2 | {{ missing }}
    | ^
```

`DiagnosticReport.frames()` exposes the structured frames for tooling. `format()` renders them for a terminal or log.

## How it works

1. The custom include node pushes the caller template, include line, and target template onto a thread-local stack.
2. When a nested render fails, the node attaches a copy of the stack to the exception as a suppressed marker.
3. `TemplateDiagnostics` finds the marker, reads the source through the loader, and builds a frame for each include.
4. The failing template and line come from the `PebbleException`.
5. Each frame shows nearby lines, the error line, and a caret at the first non-whitespace character.

The trace is attached once. Outer include nodes do not duplicate it.

## Limits

- The extends chain is not captured yet. Only includes are traced.
- The caret points at the first non-whitespace character. Pebble does not expose a column number.
- The loader must be able to read the template by name after the failure.
- `embed` and cache nodes are not traced.
- Spring Boot error handling is not wired yet. Use the formatter in an exception handler.
- Source mapping for a future Natural HTML dialect is out of scope.

## Next

- Capture the extends hierarchy at render time.
- Add a Spring Boot error controller integration.
- Add source maps for generated templates.
- Add a CLI and Gradle task for rendering a template with diagnostics.
