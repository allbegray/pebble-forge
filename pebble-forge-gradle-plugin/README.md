# pebble-forge-gradle-plugin

Gradle plugins for contract checks and render diagnostics.

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.contracts")
}

pebbleForgeContracts {
    variables.add("page")
    variables.add("items")
    componentRoot.set("components")

    rootTemplates.add("pages/account")
    partialTemplates.add("fragments/row")
}
```

```shell
./gradlew checkPebbleTemplates
```

The task walks every `.peb` file under `src/main/resources/templates` once. A template with no incoming static include edge is a root. Direct components and statically called component templates are roots. A nested template under the component root that is only included is a partial. Partials are checked through the includes that reach them. A non-partial template that no root reaches is checked as a fallback root. `rootTemplates` forces root status; `partialTemplates` excludes a fragment from root inference. Names are relative to the template root with the suffix removed.

Missing model variables, missing include targets, include cycles, and required component props fail the build. Unknown props, dynamic names, and dynamic include targets produce warnings.

## Render diagnostics

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.diagnostics")
}

pebbleForgeDiagnostics {
    templateName.set("page")
    model.put("page", mapOf("title" to "Account"))
}
```

```shell
./gradlew renderPebbleTemplate --template=page
```

The task renders one template and logs the result. On failure it logs the include/extends source-frame report and fails the build. Model values are JSON-like Gradle values. The task is not part of `check`.

The plugins are not on the Gradle Plugin Portal yet. Use the included build or publish them locally.

See [docs/CONTRACTS.md](../docs/CONTRACTS.md) and [docs/DIAGNOSTICS.md](../docs/DIAGNOSTICS.md).
