# Template contract checks

Pebble is dynamic. A missing model variable or a missing component prop usually appears at render time. The contracts module moves that failure into the build.

## What it checks

- A template reads a variable that is not in the declared model schema and not declared locally.
- A component call omits a required prop.
- A component call passes a prop the component does not declare. This is a warning because components allow unknown props.
- A component name or prop name is dynamic. This is a warning because the checker cannot resolve it.

It does not check Java types, method signatures, or expression semantics.

## Gradle plugin

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

The default template directory is `src/main/resources/templates`. The default suffix is `.peb`.

| Property | Default | Meaning |
| --- | --- | --- |
| `templatesDirectory` | `src/main/resources/templates` | Root of the template tree |
| `templateSuffix` | `.peb` | File suffix appended by the loader |
| `componentRoot` | `components` | Directory under the template root |
| `variables` | empty | Model variables the template may read |
| `allowAnyVariable` | `false` | Disable missing-variable errors |

`checkPebbleTemplates` runs as part of `check`.

The plugin is not on the Gradle Plugin Portal yet. Use the included build or publish it locally until that changes.

## Java API

```java
ContractChecker checker = ContractChecker.builder()
        .loader(new FileLoader("/srv/templates"))
        .components(Components.builder()
                .root("components")
                .suffix("")
                .build())
        .model(ModelSchema.builder()
                .variable("page")
                .variable("items")
                .build())
        .build();

ContractResult result = checker.check("page");
if (result.hasErrors()) {
    System.out.println(result.format());
}
```

`ModelSchema.any()` disables model-variable errors and keeps component prop checks.

## How it works

1. The template is parsed with a normal Pebble Forge engine and its components and language extensions.
2. A node visitor collects root variable reads, local declarations, and component calls.
3. Local declarations include `set` names, `for` variables, `loop`, `with` map keys, macro arguments, and component props.
4. The checker compares root variable reads with the model schema.
5. Static component names are resolved through `Components.resolvePath`. The component template is parsed to read its `props` declarations.
6. Missing required props are errors. Unknown props and dynamic names are warnings.

The checker caches parsed templates per run, so shared components are checked once.

## Component props

```twig
{# components/card.peb #}
{% props title, subtitle = "" %}
<article>
  <h2>{{ title }}</h2>
  {% if subtitle %}<p>{{ subtitle }}</p>{% endif %}
</article>
```

```twig
{% component "card" with {"title": page.title} %}{% endcomponent %}
```

`title` is required. `subtitle` is optional. A call without `title` fails. A call with `subtitle` passes. A call with `footer` warns.

## Known limits

- The check is presence-based, not flow-sensitive. A variable assigned later in the template satisfies an earlier read.
- Include and extends are not followed recursively. The Gradle task checks every template file, so shared templates are still covered, but include-local variables are not propagated.
- Dynamic component names and dynamic prop names are warnings.
- A `with` block must use a map literal for its keys to be visible to the checker. A dynamic map produces incomplete scope information.
- `with only` isolation is not modeled. A variable visible outside the block can satisfy a read inside it.
- Macros imported from another template are not expanded.
- Only the common Pebble node types are traversed. `embed` and `cache` bodies are not fully analyzed yet.
- Type checking is out of scope for this slice.

## Next slices

- JSON or YAML schema loaded by the Gradle plugin.
- Flow-sensitive local variable analysis.
- Include and extends scope propagation.
- Type checks for component props and model variables.
- Publish the Gradle plugin to the Plugin Portal.
- Inline suppression comments for intentional dynamic templates.
