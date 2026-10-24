# pebble-forge-gradle-plugin

Gradle plugin that registers the `checkPebbleTemplates` task.

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.contracts")
}

pebbleForgeContracts {
    variables.add("page")
    variables.add("items")
    componentRoot.set("components")
}
```

```shell
./gradlew checkPebbleTemplates
```

The task checks every `.peb` file under `src/main/resources/templates` by default. Missing model variables and required component props fail the build. Unknown props and dynamic names produce warnings.

The plugin is not on the Gradle Plugin Portal yet. Use the included build or publish it locally.

See [docs/CONTRACTS.md](../docs/CONTRACTS.md).
