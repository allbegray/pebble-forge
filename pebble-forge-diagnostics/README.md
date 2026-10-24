# pebble-forge-diagnostics

Source-frame diagnostics and include traces for Pebble Forge.

```java
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

`DiagnosticsExtension` records the include path. `TemplateDiagnostics` renders the failing template source with a caret and the include chain.

The extends chain is not captured yet. See [docs/DIAGNOSTICS.md](../docs/DIAGNOSTICS.md) for the full behavior and limits.
