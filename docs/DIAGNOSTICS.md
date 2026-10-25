# Template diagnostics

Pebble errors carry a template name and line number. They do not show the source line, the caret, or the include and extends path. The diagnostics module adds those.

## Setup

```java
ClasspathLoader loader = new ClasspathLoader();
loader.setPrefix("templates/");
loader.setSuffix(".peb");

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .build();

TemplateDiagnostics diagnostics = TemplateDiagnostics.builder()
        .loader(loader)
        .contextLines(2)
        .build();

try {
    TemplateTrace.run(() -> engine.getTemplate("page").evaluate(writer, model));
} catch (PebbleException error) {
    System.err.println(diagnostics.render(error).format());
}
```

The Pebble Forge include and extends nodes record their paths by default. Wrap the entire `evaluate` call in `TemplateTrace.run` so an error in an extended parent body carries the extends frame. Pebble evaluates the parent after the child root node returns, so a node-local catch cannot see that failure.

## Example

```text
Root attribute [missing] does not exist (layout.peb:2)
page.peb:1 extends layout.peb
  1 | {% extends "layout.peb" %}
    | ^
layout.peb:2
  1 | layout
> 2 | {{ missing }}
    | ^
```

`DiagnosticReport.frames()` exposes the structured frames for tooling. Each `DiagnosticFrame` has `kind` (`INCLUDE`, `EXTENDS`, or `ERROR`), `target`, template, line, source lines, error-line index, and caret column. `format()` renders them for a terminal or log.

```java
DiagnosticReport report = diagnostics.render(error);
for (DiagnosticFrame frame : report.frames()) {
    System.out.println(frame.kind() + " " + frame.target() + " " + frame.template());
}
```

## How it works

1. The include node pushes an `INCLUDE` frame. The extends node pushes an `EXTENDS` frame.
2. A single core `TemplateTrace` keeps include and extends frames in chronological order on a thread-local stack.
3. When a render fails, the outermost `TemplateTrace.run` attaches a copy of the stack to the exception as a suppressed marker. Include nodes also attach on a nested failure, and the marker is never duplicated.
4. `TemplateDiagnostics` finds the marker, reads the source through the loader, and builds one frame per include or extends step.
5. The failing template and line come from the `PebbleException`.
6. Each frame shows nearby lines, the error line, and a caret at the first non-whitespace character.

`TemplateTrace.run` resets the stack at a new top-level render and truncates it in `finally`, so frames do not leak between renders. Include boundaries truncate nested frames after a successful include. Include depth counts only `INCLUDE` frames.

`IncludeTrace` remains available as a compatibility façade that returns only include frames.

## Gradle task

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.diagnostics")
}

pebbleForgeDiagnostics {
    templateName.set("pages/account")
    model.put("page", mapOf("title" to "Account"))
    model.put("items", listOf(mapOf("name" to "One")))
}
```

```shell
./gradlew renderPebbleTemplate
# or override the configured template
./gradlew renderPebbleTemplate --template=pages/account
```

| Property | Default | Meaning |
| --- | --- | --- |
| `templatesDirectory` | `src/main/resources/templates` | Template root |
| `templateSuffix` | `.peb` | Loader suffix |
| `templateName` | none | Template name without suffix |
| `model` | empty | Render model, JSON-like values only |
| `componentRoot` | `components` | Component directory |
| `strictVariables` | `true` | Fail on missing variables |
| `contextLines` | `2` | Source lines around the error |

The task is not part of `check`. On success it logs the rendered text. On a render failure it logs the source-frame report and fails the build. A missing top-level template is a configuration error; a missing include or extends target is a render diagnostic. Model values are supplied through the Gradle extension, so no JSON dependency is needed.

## CLI

The standalone `pebble-forge-cli` module renders one template with an inline JSON model or a JSON/YAML model file.

```shell
./gradlew :pebble-forge-cli:run \
  --args="--templates src/main/resources/templates --template page --model-file model.yaml"
```

```shell
./gradlew :pebble-forge-cli:installDist
pebble-forge-cli/build/install/pebble-forge-cli/bin/pebble-forge-cli \
  --templates src/main/resources/templates --template page \
  --model '{"page":{"title":"Store"}}'
```

Options are `--templates`, `--suffix`, `--template`, `--model`, `--model-file`, `--component-root`, `--context-lines`, `--strict`, `--lenient`, and `--output`. Exit codes are 0 for success, 1 for a render failure, and 2 for a usage or model error. The CLI has no sandbox and is intended for trusted templates. A top-level template name is jailed to the template root, but `FileLoader` includes and extends are not jailed; see the module README.

## Spring Boot 4

The starter defines `TemplateDiagnostics` from the raw `pebbleLoader` bean and wraps the official Pebble view. A render failure becomes a real servlet 500 error dispatch, and the application error page can read:

- `pebbleDiagnostics`: a structured `DiagnosticReport`
- `pebbleDiagnosticsText`: the formatted report

The starter does not own the error page. It does not replace the official view resolver. It only adds a view decorator and an optional `ErrorAttributes` adapter.

```properties
pebble.forge.diagnostics.enabled=true
pebble.forge.diagnostics.context-lines=2
pebble.forge.diagnostics.expose-in-error-attributes=false
```

`expose-in-error-attributes` is off by default so source text does not leak into production API responses. When enabled and no application `ErrorAttributes` bean exists, the starter adds the two attributes. An application can also read the request attribute directly from a custom error controller.

## Limits

- Parent-body errors need `TemplateTrace.run` around the whole evaluation. A direct `evaluate` call without the wrapper may show only the failing template.
- The caret points at the first non-whitespace character. Pebble does not expose a column number.
- The loader must be able to read the template by name after the failure.
- `embed` and cache nodes are not traced.
- Parallel render branches use different threads and do not share the thread-local trace.
- A committed response cannot be replaced by an error page. The view decorator records and logs the report but does not call `sendError`.
- Source mapping for a future Natural HTML dialect is out of scope.

## Next


Natural HTML analysis errors are `NaturalHtmlException`, not `PebbleException`, so they carry `template:line:column` in the message but do not produce a `TemplateDiagnostics` source frame. The Natural HTML source map can locate generated offsets in the original file. See [NATURAL-HTML.md](NATURAL-HTML.md).
- Add source maps for generated templates.
