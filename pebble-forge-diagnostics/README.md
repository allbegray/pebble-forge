# pebble-forge-diagnostics

Source-frame diagnostics and include traces for Pebble Forge.

```java
PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
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

The Pebble Forge include node records the include path by default. `TemplateDiagnostics` renders the failing template source with a caret and the include chain.

The extends chain is not captured yet. See [docs/DIAGNOSTICS.md](../docs/DIAGNOSTICS.md) for the full behavior and limits.
