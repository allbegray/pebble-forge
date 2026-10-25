# pebble-forge-diagnostics

Source-frame diagnostics with include and extends traces for Pebble Forge.

```java
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

The Pebble Forge include and extends nodes record their paths by default. `TemplateDiagnostics` renders the failing template source with a caret and the include or extends chain. Each `DiagnosticFrame` carries `Kind.INCLUDE`, `Kind.EXTENDS`, or `Kind.ERROR` plus the target template, so consumers do not parse the display label. The `renderPebbleTemplate` Gradle task and the `pebble-forge-cli` renderer print the same report.

Wrap the whole `evaluate` call in `TemplateTrace.run` so an error in an extended parent body carries the extends frame. The Spring Boot starter does this for its official view resolver. See [docs/DIAGNOSTICS.md](../docs/DIAGNOSTICS.md) for the full behavior and limits.
