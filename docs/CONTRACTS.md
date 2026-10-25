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

It does not check Java types, method signatures, or expression semantics.

## Gradle plugin

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.contracts")
}

pebbleForgeContracts {
    schemaFile.set(layout.projectDirectory.file("contracts.yaml"))
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
| `componentRoot` | `components` | Default component directory under the template root |
| `schemaFile` | none | Optional JSON or YAML file with model variables |
| `variables` | empty | Model variables the template may read |
| `allowAnyVariable` | `false` | Disable missing-variable errors |
| `rootTemplates` | empty | Force root status for dual-role templates |
| `partialTemplates` | empty | Exclude a template from root inference |

Names are relative to the template root with the template suffix removed. `pages/account`, not `pages/account.peb`.

### Schema file

`schemaFile` loads model variables from JSON or YAML. `variables` and the file are merged. `allowAnyVariable` is true when either the DSL or the file enables it.

```yaml
variables:
  - page
  - items
allowAnyVariable: false
```

```json
{
  "variables": ["page", "items"],
  "allowAnyVariable": false
}
```

The root must be an object. The only fields are `variables`, `allowAnyVariable`, and optional `$schema`. Unknown fields, wrong types, blank names, malformed files, and extensions other than `.json`, `.yaml`, or `.yml` fail the build. `$schema` is ignored; it is an editor hint.

The schema is flat. It declares names and one permissive flag. It does not define types, required fields, nullability, or nested shapes.

The core `Components` API supports a default root plus named roots such as `ui:card`, and the Boot starter binds `pebble.forge.components.roots.*`. The stock Gradle task still exposes one `componentRoot`; a custom task can pass a fully configured `Components` when a project checks namespaced components.

`checkPebbleTemplates` runs as part of `check`.

The plugin is not on the Gradle Plugin Portal yet. Use the included build or publish it locally until that changes.

## Root and partial policy

The task walks the template tree once and calls `ContractChecker.checkAll`. It does not check each file as an independent root.

1. A name in `rootTemplates` is a root.
2. A direct component entry point, or a template called by a static `{% component %}` call, is a root. Component membership wins over `partialTemplates`, and the task logs a warning when a direct component is listed there.
3. A name in `partialTemplates` is not a root.
4. A nested template under the component root that is only included, not called as a component, is treated as a partial.
5. A non-component template with no incoming static include or extends edge is a root.
6. A parent reached only through extends is not a root. A parent that is also rendered directly needs `rootTemplates` or `partialTemplates`.
7. Everything else is checked only through the calls that reach it.
8. A non-component, non-partial template that no root reaches is checked as a fallback root. This keeps an excluded partial from hiding a page with a missing variable.

A static include or extends target is resolved with the loader's `resolveRelativePath`, the same call the renderer uses. A template name in the Gradle DSL is validated against the walked template set, so a typo fails the build instead of silently disabling a check.

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

`check(name)` treats the name as a root and follows its static includes and extends parents. It does not apply root inference or the override sets.

`checkAll(names)` treats the collection as the template universe. It parses every name, analyzes every name for structural errors, infers roots, and evaluates roots plus components.

`ModelSchema.any()` disables model-variable errors and keeps component prop checks.

## How it works

1. Each requested template is parsed once with a normal Pebble Forge engine and its components and language extensions.
2. A node visitor records root variable reads, local declarations, component calls, and include edges.
3. Include and extends edges carry the literal and resolved target, resolution errors, line, and the owning block. Include edges also carry literal `with` keys, dynamic flags, and a snapshot of the caller's declared names at the include point. Extends parent edges are followed.
4. The analyze pass checks every parsed template for parse failures, missing include or extends targets, invalid targets, dynamic targets, dynamic `with` keys, component calls, and prop mismatches. Unreachable partials still get these structural checks.
5. Root inference uses the static include and extends graph plus the explicit root and partial sets.
6. The evaluate pass checks direct reads with a source-order flow snapshot. `if` and `switch` branches join by intersection. It follows a static include with `caller scope + lexical visible names at the call + literal with keys`, and a static extends parent with the child's visible declarations. A component is evaluated closed over its props and the model. A call that repeats a template already being rendered is reported as a cycle.
7. Include, extends, and component cycles are errors. A dynamic include target is checked for its target-variable read but is not followed. A dynamic `with` map or dynamic `with` key makes the affected child scope incomplete, so missing-variable errors are suppressed for that call site and a warning explains why.
8. Local declarations include root and block `set` names, `for` variables, `loop`, `with` map keys, macro arguments, and component props. A read must be definitely visible at its source point. Loop, with, and macro locals do not escape after their construct. Reads owned by an overridden block are skipped.
9. A parent block that a child overrides never executes. Its declarations are excluded from the child's visible names. A parent root declaration is visible to a child block, and a child root declaration is visible to the parent. Cross-template extends propagation remains presence-based.

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

- Direct reads use source-order flow. A read before a `set`, in a sibling branch, or after a loop/with/macro scope is reported.
- `if` and `switch` joins use intersection. A `switch` without a `default` always keeps the no-match path, so a set in every case can still be reported as missing.
- A loop body is conservatively treated as running zero or more times, so a body-only set is not visible after the loop.
- A root read can see a declaration from a non-overridden block of the same template regardless of source position.
- Include visibility uses a lexical snapshot at the include point. It is not propagated through branch joins.
- Cross-template extends summaries publish branch and loop declarations that are not definitely visible. A child conditional set can satisfy a parent read.
- Extends parent reads are reported at the parent template and line. Load and parse failures at the extends edge include an extends path. Parent-body read errors do not repeat the whole chain.
- A dynamic or missing parent suppresses scope-dependent missing reads owned by blocks because the parent declarations are unknown. Model reads inside the child block are still checked.
- Cross-template block ownership is presence-based. A block declaration can satisfy a later parent content read. Intra-block direct reads are flow-sensitive. Nested blocks are not modeled individually.
- Dynamic include targets and dynamic `with` keys are warnings. A dynamic target whose base expression is a model read still checks that base.
- A partial reached only through a dynamic include is inferred as a root unless it is listed in `partialTemplates`.
- Components do not see caller locals at check time, even though runtime `includeTemplate` shares the scope chain.
- A bare dynamic include target is checked as a model read. A compound target checks its base expression. Neither can be followed to a template.
- A nested template under the component root is a partial unless a static component call targets it or `rootTemplates` forces it.
- A component cycle is reported at the call line. Component analysis is closed, so a component that intentionally reads caller state still fails.
- A `with` block must use a map literal for its keys to be visible to the checker. A dynamic map suppresses direct-read errors for the block.
- `with only` hides outer locals and is a closed scope. Pebble internal names such as `__slots`, `__component`, `locale`, and `template` are not modeled as visible and can produce false positives.
- A macro body is a closed scope with its arguments. It does not see outer template locals. A macro body reading component props can false-positive.
- Macros imported from another template are not expanded.
- Only the common Pebble node types are traversed. `embed` and `cache` bodies are not fully analyzed yet.
- Type checking is out of scope for this slice. The JSON/YAML schema is a flat variable-name list, not a type system.
- Include chains are encoded in diagnostic messages. `ContractDiagnostic` has no path field.
- Scope evaluation is capped at 4,096 distinct scopes per template. When the cap is reached, the checker emits a warning and skips the remaining call-site evaluations for that template.

## Next slices

- Flow-sensitive cross-template include and extends summaries.
- Typed JSON or YAML schema for component props and model variables.
- Exact block ordering and switch exhaustiveness.
- Publish the Gradle plugin to the Plugin Portal.
- Inline suppression comments for intentional dynamic templates.
