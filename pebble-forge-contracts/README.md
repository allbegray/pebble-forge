# pebble-forge-contracts

Build-time template contract checks for Pebble Forge.

The module parses templates with a normal Pebble Forge engine and checks:

- model variable reads against a declared schema,
- model reads inside static includes against the caller scope and literal `with` keys,
- required component props,
- unknown component props,
- dynamic component, prop, and include names,
- static include cycles and missing targets.

## Dependency

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-contracts:0.1.0-SNAPSHOT")
```

## API

```java
ContractChecker checker = ContractChecker.builder()
        .loader(new FileLoader("/srv/templates"))
        .components(Components.builder().root("components").suffix("").build())
        .model(ModelSchema.builder().variable("page").build())
        .build();

ContractResult result = checker.check("page");
ContractResult all = checker.checkAll(List.of("page", "partial", "components/card"));
```

`check(name)` treats the name as a root. `checkAll(names)` infers roots from the static include graph, checks partials through their callers, and checks components closed over props and the model. Add `.rootTemplates(...)` or `.partialTemplates(...)` when inference is not enough.

The checker is presence-based for direct reads. It follows static includes with a lexical scope snapshot. It does not perform Java type checking, full flow-sensitive block analysis, or extends propagation.

For the Gradle task, use the `pebble-forge-gradle-plugin` module or the documented `checkPebbleTemplates` task.

See [docs/CONTRACTS.md](../docs/CONTRACTS.md) for the full behavior and limits.
