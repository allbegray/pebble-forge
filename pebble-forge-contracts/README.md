# pebble-forge-contracts

Build-time template contract checks for Pebble Forge.

The module parses templates with a normal Pebble Forge engine and checks:

- model variable reads against a declared schema, including typed object properties and array item shapes,
- model reads inside static includes against the caller scope and literal `with` keys,
- model reads across source-order branches and static extends parents with child/parent scope and block ownership,
- required component props,
- unknown component props,
- dynamic component, prop, include, and extends names,
- include, extends, and component cycles and missing targets.

## Dependency

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-contracts:0.1.0-SNAPSHOT")
```

## API

```java
ContractChecker checker = ContractChecker.builder()
        .loader(new FileLoader("/srv/templates"))
        .components(Components.builder().root("components").suffix("").build())
        .model(ModelSchema.builder()
                .variable("page", ModelType.object(Map.of(
                        "title", ModelType.string())))
                .build())
        .build();

ContractResult result = checker.check("page");
ContractResult all = checker.checkAll(List.of("page", "partial", "components/card"));
```

`check(name)` treats the name as a root. `checkAll(names)` infers roots from the static include and extends graph, checks partials through their callers, and checks components closed over props and the model. Add `.rootTemplates(...)` or `.partialTemplates(...)` when inference is not enough.

Direct reads are flow-sensitive inside a template. `if` and `switch` branches join by intersection. The checker follows static includes with a lexical scope snapshot and static extends parents with block ownership and definite end-of-flow summaries. Typed schemas check literal attribute paths against object shapes. It does not perform full Java expression type checking.

For the Gradle task, use the `pebble-forge-gradle-plugin` module or the documented `checkPebbleTemplates` task.

See [docs/CONTRACTS.md](../docs/CONTRACTS.md) for the full behavior and limits.
