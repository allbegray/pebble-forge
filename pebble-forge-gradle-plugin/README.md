# pebble-forge-gradle-plugin

Gradle plugins for contract checks and render diagnostics.

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.contracts") version "0.4.0"
}

pebbleForgeContracts {
    schemaFile.set(layout.projectDirectory.file("contracts.yaml"))
    variables.add("page")
    variables.add("items")
    componentRoot.set("components")
    componentRoots.put("ui", "ui-components")

    rootTemplates.add("pages/account")
    partialTemplates.add("fragments/row")
}
```

```shell
./gradlew checkPebbleTemplates
```

`schemaFile` is an optional JSON or YAML file with `variables` and `allowAnyVariable`. It merges with the DSL `variables` set.

`componentRoot` is the default component root. `componentRoots` adds named roots keyed by namespace. A page calls `{% component "ui:card" %}` and the plugin resolves it to `ui-components/card.peb`. Names without a namespace use `componentRoot`. A namespace must match `[A-Za-z][A-Za-z0-9_-]*`, and two namespaces cannot share a root.

The task walks every `.peb` file under `src/main/resources/templates` once. A template with no incoming static include edge is a root. Direct components and statically called component templates are roots. A nested template under a component root that is only included is a partial. Partials are checked through the includes that reach them. A non-partial template that no root reaches is checked as a fallback root. `rootTemplates` forces root status; `partialTemplates` excludes a fragment from root inference. Names are relative to the template root with the suffix removed.

Missing model variables, missing include targets, include cycles, and required component props fail the build. Unknown props, dynamic names, and dynamic include targets produce warnings.

## Render diagnostics

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.diagnostics") version "0.4.0"
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

## Plugin IDs

- `io.github.allbegray.pebbleforge.contracts`
- `io.github.allbegray.pebbleforge.diagnostics`

Version 0.4.0 is submitted to the Portal and awaits first-time approval for both IDs. The coordinates start resolving after approval.

## Publish to the Gradle Plugin Portal

The build applies `com.gradle.plugin-publish`. Put the Portal API key in `~/.gradle/gradle.properties`, or set `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET`. Then run:

```shell
./gradlew :pebble-forge-gradle-plugin:publishPlugins
```

The Portal rejects `SNAPSHOT` versions, so set a release version in the root `build.gradle.kts` first. A new plugin ID needs manual approval the first time. The release checklist is in [docs/RELEASING.md](../docs/RELEASING.md).

See [docs/CONTRACTS.md](../docs/CONTRACTS.md) and [docs/DIAGNOSTICS.md](../docs/DIAGNOSTICS.md).
