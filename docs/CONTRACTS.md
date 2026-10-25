# Template contract checks

Pebble is dynamic. A missing model variable or a missing component prop usually appears at render time. The contracts module moves that failure into the build.

## What it checks

- A template reads a variable that is not in the declared model schema and not declared locally.
- A variable read inside a statically included template is not in the model schema, not declared in the included template, and not visible at the include call.
- A component call omits a required prop.
- A component call passes a prop the component does not declare. This is a warning because components allow unknown props.
- A component name or prop name is dynamic. This is a warning because the checker cannot resolve it.
- A static include target cannot be loaded or parsed.
- A static include cycle exists.

It does not check Java types, method signatures, or expression semantics. Extends parents are recorded but not followed in this slice.

## Gradle plugin

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

The default template directory is `src/main/resources/templates`. The default suffix is `.peb`.

| Property | Default | Meaning |
| --- | --- | --- |
| `templatesDirectory` | `src/main/resources/templates` | Root of the template tree |
| `templateSuffix` | `.peb` | File suffix appended by the loader |
| `componentRoot` | `components` | Directory under the template root |
| `variables` | empty | Model variables the template may read |
| `allowAnyVariable` | `false` | Disable missing-variable errors |
| `rootTemplates` | empty | Force root status for dual-role templates |
| `partialTemplates` | empty | Exclude a template from root inference |

Names are relative to the template root with the template suffix removed. `pages/account`, not `pages/account.peb`.

`checkPebbleTemplates` runs as part of `check`.

The plugin is not on the Gradle Plugin Portal yet. Use the included build or publish it locally until that changes.

## Root and partial policy

The task walks the template tree once and calls `ContractChecker.checkAll`. It does not check each file as an independent root.

1. A name in `rootTemplates` is a root.
2. A component is always a root. Component membership wins over `partialTemplates`, and the task logs a warning when a component is listed there.
3. A name in `partialTemplates` is not a root.
4. A non-component template with no incoming static include edge is a root.
5. Everything else is checked only through the includes that reach it.

A static include target is resolved with the loader's `resolveRelativePath`, the same call the renderer uses. A template name in the Gradle DSL is validated against the walked template set, so a typo fails the build instead of silently disabling a check.

A partial reached only through a dynamic include has no incoming edge, so it is inferred as a root. Add it to `partialTemplates` when that root check is not what you want. A dual-role template that is both rendered directly and included needs `rootTemplates`.

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
        .rootTemplates(List.of("pages/account"))
        .partialTemplates(List.of("fragments/row"))
        .build();

ContractResult oneRoot = checker.check("page");
ContractResult templateSet = checker.checkAll(List.of("page", "partial", "components/card"));
```

`check(name)` treats the name as a root and follows its static includes. It does not apply root inference or the override sets.

`checkAll(names)` treats the collection as the template universe. It parses every name, analyzes every name for structural errors, infers roots, and evaluates roots plus components.

`ModelSchema.any()` disables model-variable errors and keeps component prop checks.

## How it works

1. Each requested template is parsed once with a normal Pebble Forge engine and its components and language extensions.
2. A node visitor records root variable reads, local declarations, component calls, and include edges.
3. Include edges carry the literal target, resolved target, resolution error, literal `with` keys, dynamic flags, line, and a snapshot of the caller's declared names at the include point. Extends records a parent edge but does not follow it.
4. The analyze pass checks every parsed template for parse failures, missing include targets, invalid targets, dynamic targets, dynamic `with` keys, component calls, and prop mismatches. Unreachable partials still get these structural checks.
5. Root inference uses the static include graph and the explicit root and partial sets.
6. The evaluate pass checks reads. It follows a static include with `caller scope + visible names at the call + literal with keys`. A component is evaluated closed over its props and the model.
7. Include cycles are errors. A dynamic include target or a dynamic `with` key makes the affected child scope incomplete, so missing-variable errors are suppressed for that call site and a warning explains why.
8. Local declarations include `set` names, `for` variables, `loop`, `with` map keys, macro arguments, and component props.

Parsed templates are cached per run. Diagnostic order is deterministic.

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

Components stay closed over props and the model. The renderer shares the caller scope chain, but a component contract that reads a caller local is still reported as missing. This keeps props as the component interface.

## Known limits

- The check is presence-based, not flow-sensitive. A variable assigned later in a template satisfies an earlier read.
- Static includes are followed with a flat snapshot of the caller's declared names. This is not a block-scoped analysis.
- Extends parents are recorded but not followed. A parent template is checked as its own root when it is in the template set.
- Dynamic include targets and dynamic `with` keys are warnings. A dynamic target whose base expression is a model read still checks that base.
- A partial reached only through a dynamic include is inferred as a root unless it is listed in `partialTemplates`.
- Components do not see caller locals at check time, even though runtime `includeTemplate` shares the scope chain.
- A `with` block must use a map literal for its keys to be visible to the checker. A dynamic map produces incomplete scope information.
- `with only` isolation is not modeled. A variable visible outside the block can satisfy a read inside it.
- Macros imported from another template are not expanded.
- Only the common Pebble node types are traversed. `embed` and `cache` bodies are not fully analyzed yet.
- Type checking is out of scope for this slice.
- Include chains are encoded in diagnostic messages. `ContractDiagnostic` has no path field.

## Next slices

- Extends scope propagation and block ownership.
- JSON or YAML schema loaded by the Gradle plugin.
- Flow-sensitive local variable analysis.
- Type checks for component props and model variables.
- Publish the Gradle plugin to the Plugin Portal.
- Inline suppression comments for intentional dynamic templates.
