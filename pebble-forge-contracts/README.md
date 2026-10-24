# pebble-forge-contracts

Build-time template contract checks for Pebble Forge.

The module parses templates with a normal Pebble Forge engine and checks:

- model variable reads against a declared schema,
- required component props,
- unknown component props,
- dynamic component and prop names.

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
```

The checker is presence-based. It does not perform Java type checking or flow-sensitive analysis.

For the Gradle task, use the `pebble-forge-gradle-plugin` module or the documented `checkPebbleTemplates` task.

See [docs/CONTRACTS.md](../docs/CONTRACTS.md) for the full behavior and limits.
