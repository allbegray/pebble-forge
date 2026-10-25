# Pebble Forge

[English](README.md) | [한국어](README.ko.md)

Pebble Forge is an extension pack for Pebble 4. It keeps Pebble templates valid and adds the features that other template engines do better. It ships template-defined components, a capability sandbox, context-aware escaping, Natural HTML, build-time contract checks, and Spring Boot and Spring Security integration.

The name is a working title. The project is an extension library, not a Pebble fork.

The Maven group and Java package are `io.github.allbegray.pebbleforge`. They match the GitHub account `allbegray` and the Maven Central `io.github.*` namespace.

## Status

- Java 21
- Pebble 4.1.2
- Spring Boot 4.0.6
- Spring Security 7.0.8
- Version 0.3.0, GitHub release. Maven Central and Gradle Plugin Portal publication are pending.

The source release is at <https://github.com/allbegray/pebble-forge/releases/tag/v0.3.0>. The snapshot can be installed into the local Maven repository with `./gradlew publishToMavenLocal` until Maven Central is available.

675 tests cover the core, contracts, diagnostics, Natural HTML, the Gradle plugin, the CLI, Spring Security helpers, the Boot auto-configuration, benchmarks, fuzzing, and a Spring Boot demo app.

## Modules

| Module | Purpose |
| --- | --- |
| `pebble-forge-core` | Components, sandbox, language extensions, context-aware escaping, and the `PebbleForge.engine()` builder |
| `pebble-forge-natural-html` | Browser-openable `.html` templates with context-aware expression rewriting |
| `pebble-forge-benchmarks` | Manual JMH throughput benchmarks for plain, component, sandbox, and language rendering |
| `pebble-forge-contracts` | Build-time model variable and component prop checks |
| `pebble-forge-diagnostics` | Source frames and include/extends traces for template errors |
| `pebble-forge-gradle-plugin` | `checkPebbleTemplates` and `renderPebbleTemplate` Gradle tasks |
| `pebble-forge-cli` | Standalone template render and source-frame diagnostics CLI |
| `pebble-forge-spring` | Spring Security template functions and sandbox allowlist helpers |
| `pebble-forge-spring-boot-4-starter` | Spring Boot 4 auto-configuration, properties, and servlet integration |
| `pebble-forge-demo` | Runnable component and sandbox demo |
| `pebble-forge-demo-app` | Runnable Spring Boot 4 MVC demo with components, language extensions, and Spring Security |
| `pebble-forge-fuzz` | jqwik property tests for the parser, path jail, and allowlists |

## Core

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-core:0.3.0")
```

```java
ClasspathLoader loader = new ClasspathLoader();
loader.setPrefix("templates/");
loader.setSuffix(".peb");

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .components(Components.builder()
                .root("components")
                .suffix("")
                .maxDepth(16)
                .build())
        .sandbox(SandboxPolicy.builder()
                .allowComponents()
                .allowTags("extends", "block")
                .maxRenderedChars(200_000)
                .build())
        .build();
```

`PebbleForge` turns strict variables on by default. Plain Pebble leaves them off. Every other Pebble setting has a pass-through on the builder.

### Components

A component is a normal Pebble template. Props are declared with `props`. Missing required props fail with the component name and line number.

```twig
{# templates/components/card.peb #}
{% props title, subtitle = "" %}
<article class="card">
  <h2>{{ title }}</h2>
  {% if subtitle %}<p>{{ subtitle }}</p>{% endif %}
  <div class="body">{{ slot("body") }}</div>
</article>
```

```twig
{% component "card" with {"title": page.title, "subtitle": page.summary} %}
  {% slot "body" %}
    <p>{{ page.body }}</p>
  {% endslot %}
{% endcomponent %}
```

A body without explicit `slot` tags becomes the default slot.

`Components.suffix` is appended by Forge. If the loader already appends a suffix, set `Components.suffix("")`. The Boot starter does this. The suffix mismatch is the most common setup mistake.

Slots are pre-rendered and marked safe. Emit them directly with `{{ slot("body") }}`. Concatenating or filtering a slot turns it back into a plain string and escapes it again. The slot function only emits captured slot values, so a `with` block cannot forge slot content and bypass escaping.

Components can come from more than one root:

```java
Components components = Components.builder()
        .root("components")
        .root("ui", "lib/components")
        .root("admin", "admin/components")
        .suffix("")
        .build();
```

```twig
{% component "card" %}{% endcomponent %}
{% component "ui:card" %}{% endcomponent %}
```

Unqualified names use the default root. A named root uses `namespace:name`. Duplicate namespaces and duplicate normalized root paths fail at build time. Components packaged as classpath resources inside a jar work through the same loader.

### Context-aware escaping

`PebbleForge.engine()` registers two new escaping strategies on top of Pebble's built-ins.

| Context | Strategy |
| --- | --- |
| HTML text | `html` |
| HTML attribute value | `html_attr` |
| Whole URL in an attribute | `url` |
| Quoted JavaScript string | `js` |
| CSS in `<style>` | `css` |

```twig
<a href="{{ link | escape("url") }}" title="{{ title | escape("html_attr") }}">{{ body }}</a>
<script>const name = "{{ name | escape("js") }}";</script>
```

`url` allows `http`, `https`, `mailto`, `tel`, and relative URLs, and turns other schemes into `#`. `js` is for a quoted string, not a JavaScript expression. `SafeString` from `slot()` or `raw` bypasses every strategy.

See [docs/ESCAPING.md](docs/ESCAPING.md).

### Language extensions

Pebble Forge adds tags that Pebble does not ship.

| Tag | Behavior |
| --- | --- |
| `switch`, `case`, `default`, `endswitch` | Renders the first matching case. A case can list several values. It does not fall through. |
| `break`, `continue` | Controls a `for` loop. `break` also exits a `switch`. |
| `for key, value in ...` | Destructures map entries, or an index and a value for other iterables. |
| `with { ... }` | Renders a body with extra scoped variables. |
| `with { ... } only` | Hides outer variables. Internal slot state, locale, and template are kept. |

```twig
{% with {"role": page.role} %}
  {% switch role %}
    {% case "admin", "owner" %}Admin
    {% case "user" %}User
    {% default %}Guest
  {% endswitch %}
{% endwith %}

{% for key, value in page.metrics %}
  {{ key }}: {{ value }}
{% endfor %}

{% with {"inner": page.value} only %}
  {{ inner }}
{% endwith %}
```

`for` is replaced with a break and continue aware version. It keeps `loop.index`, `loop.first`, `loop.last`, `loop.length`, `loop.revindex`, and the `else` branch. A list destructured with `for index, value` uses a zero-based index.

`with only` hides the model and extension scope. Slot content still renders because the internal slot state is copied into the isolated scope.

`break` and `continue` are lexical. They must appear inside a `for` or `switch` in the template source. A macro that contains `break` and is called outside a loop is outside this guarantee.

Disable the language extensions when a template must parse as plain Pebble.

```java
PebbleForge.engine().languageExtensions(false)
```

### Contract checks

The contracts module parses templates at build time and checks model variable reads and component props. Direct reads are flow-sensitive inside a template. Static includes and extends parents are followed with the caller or child scope, literal `with` keys, block ownership, and definite end-of-flow declarations across templates. A typed JSON or YAML schema can declare object properties and array item shapes.

```kotlin
plugins {
    id("io.github.allbegray.pebbleforge.contracts")
}

pebbleForgeContracts {
    schemaFile.set(layout.projectDirectory.file("contracts.yaml"))
    variables.add("page")              // merged with the file
    variables.add("items")
    componentRoot.set("components")
    componentRoots.put("ui", "ui-components")

    rootTemplates.add("pages/account")     // force root status
    partialTemplates.add("fragments/row")  // exclude from root inference
}
```

```shell
./gradlew checkPebbleTemplates
```

A template with no incoming static include or extends edge is a root. Direct components and statically called component templates are roots. A nested template under the component root that is only included is a partial. A partial is checked through the calls that reach it. A parent reached only through extends is not a root. A non-partial template that no root reaches is checked as a fallback root. `rootTemplates` forces a dual-role template. `partialTemplates` covers a fragment reached only through a dynamic include. Names are relative to the template root with the suffix removed.

A missing model variable or required component prop fails the build. Dynamic include targets, dynamic `with` keys, unknown props, and dynamic component names are warnings. Include, extends, and component cycles are errors.

```text
page:4: ERROR: Variable [page] is not declared in the model schema
page:8: ERROR: Component [card] requires prop [title]
page:8: WARNING: Component [card] does not declare prop [footer]
```

The default template root is `src/main/resources/templates`. The default suffix is `.peb`. `schemaFile` loads `variables`, optional types, and `allowAnyVariable` from JSON or YAML.

```yaml
variables:
  page:
    type: object
    properties:
      title: string
  items:
    type: array
    items: string
```

Set `allowAnyVariable` to `true` to check only components.

The Gradle plugin is configured for the Plugin Portal but has not been uploaded yet. The `pebble-forge-contracts` module can also be used directly from Java. Use `check(name)` for one root or `checkAll(names)` for a template set. See [docs/CONTRACTS.md](docs/CONTRACTS.md) for the API and [known limits](docs/CONTRACTS.md#known-limits).

### Natural HTML

The `pebble-forge-natural-html` module ships a loader for browser-openable `.html` templates. Expressions are rewritten with the context strategy before Pebble parses them.

```java
ClasspathLoader base = new ClasspathLoader();
base.setPrefix("templates/");
base.setSuffix(".html");

PebbleEngine engine = PebbleForge.engine()
        .loader(NaturalHtml.loader(base))
        .build();
```

```html
<a href="{{ page.url }}" title="{{ page.title }}">{{ page.body }}</a>
```

The first slice accepts expressions only. It rejects `{% ... %}` tags, includes, extends, components, macros, JavaScript, and CSS interpolation. jsoup validates the HTML, insertions preserve line numbers, and a source map maps generated offsets back to the original file. See [docs/NATURAL-HTML.md](docs/NATURAL-HTML.md).

### Diagnostics

The diagnostics module turns a Pebble error into source frames and an include or extends path.

```java
PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .build();

try {
    TemplateTrace.run(() -> engine.getTemplate("page").evaluate(writer, model));
} catch (PebbleException error) {
    System.err.println(TemplateDiagnostics.builder()
            .loader(loader)
            .build()
            .render(error)
            .format());
}
```

```text
Root attribute [missing] does not exist (layout.peb:2)
page.peb:1 extends layout.peb
  1 | {% extends "layout.peb" %}
    | ^
layout.peb:2
  1 | layout
> 2 | {{ missing }}
    | ^
```

The Pebble Forge include and extends nodes record the chain by default. Wrap the entire `evaluate` call in `TemplateTrace.run` so an error in a parent body carries the extends frame. Each frame has a structured `INCLUDE`, `EXTENDS`, or `ERROR` kind and target. Natural HTML rejections implement `SourceMappedError` and get an original-position frame. See [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md).

The Gradle plugin also provides a render task.

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

The standalone CLI uses the same source-frame report and accepts a JSON or YAML model.

```shell
./gradlew :pebble-forge-cli:run --args="--templates src/main/resources/templates --template page --model-file model.yaml"
```

See [pebble-forge-cli/README.md](pebble-forge-cli/README.md) for options and exit codes.

### Development mode

Development mode reloads a template when its source changes. The same engine is reused.

```java
FileLoader loader = new FileLoader("/srv/templates");
loader.setSuffix(".peb");

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .devMode(true)
        .build();
```

The loader hashes the template source into the cache key. A changed file gets a new key, so the next render parses it again. Old keys stay in the cache until the engine is discarded. Use this in development, not in a long-running production process.

Classpath resources inside a jar cannot change at runtime. Point the loader at a directory when you want live reload.

Spring Boot uses the same switch.

```properties
pebble.forge.dev-mode=true
```

### Sandbox

`SandboxPolicy.strict()` is the default policy.

| Capability | Default |
| --- | --- |
| Tags | `if`, `for`, `set`, `verbatim`, `include`, `component`, `slot`, `props`, `switch`, `break`, `continue`, `with` |
| Filters | `escape`, `default`, `length`, `join`, `upper`, `lower`, `capitalize`, `title`, `trim`, `replace`, `slice`, `first`, `last`, `reverse`, `sort`, `split`, `abs`, `numberformat`, `date`, `abbreviate`, `urlencode` |
| Functions | `slot`, `i18n`, `max`, `min` |
| Tests | `empty`, `null`, `iterable`, `even`, `odd`, `map` |
| Raw output | Denied |
| Output limit | 1,000,000 characters |
| Loop limit | 100,000 iterations per loop |
| Total loop limit | 1,000,000 iterations per render |
| Include depth | 16 nested includes |
| Slot capture | 1,000,000 characters per component |
| Method access | Dangerous classes, methods, and return types blocked. Public fields blocked. |
| Template paths | Absolute paths, `..`, `.` segments, backslashes, NUL, colon, whitespace, `{`, `}`, `%`, and empty segments rejected |
| Symlinks | `SandboxedLoader` is lexical. `SandboxedFileLoader` resolves real paths and rejects escapes. |

Add capabilities with `allowTags`, `allowFilters`, `allowFunctions`, `allowTests`, `allowGlobalVariables`, `allowComponents`, and `allowRawOutput`. `allowOnly*` replaces a default set.

The sandbox is an in-process capability guard, not a CPU quota. A template can still burn CPU with a large user-supplied list. Run hostile templates in a separate process when CPU is hostile. Loop limits are per loop. The total loop budget is shared across the render, except parallel branches get a separate budget. Include depth is limited to 16 by default. `SandboxedLoader` is lexical. Use `SandboxedFileLoader` when symlinks are possible.

### Per-template policies

Allowlists, method access, and path validation can vary by template name.

```java
SandboxPolicy publicPolicy = SandboxPolicy.strict();
SandboxPolicy adminPolicy = SandboxPolicy.builder()
        .allowRawOutput()
        .build();

SandboxPolicyResolver policies = SandboxPolicyResolver
        .builder(publicPolicy)
        .prefix("admin/", adminPolicy)
        .build();

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .policies(policies)
        .build();
```

The engine parses the union of all policies. Tags are checked at parse time. Filters, functions, tests, and methods are checked at render time against the policy of the template that owns the node. Includes, macros, embeds, and components use the policy of the template that declares the node. `escape` is required by autoescaping. `default` and `defined` keep Pebble's special handling when a policy denies them.

Exact rules beat prefix rules, and the longest prefix wins. The output limit is the strictest finite limit across policies. Autoescaping is on if any policy forces it. A global variable is exposed only when every policy allows it. See [docs/TEMPLATE-POLICIES.md](docs/TEMPLATE-POLICIES.md).

### Per-template resource limits

Resource limits can vary by template name.

```java
SandboxLimitsResolver limits = SandboxLimitsResolver
        .builder(SandboxLimits.from(policy))
        .prefix("public/", SandboxLimits.builder()
                .maxLoopIterations(1_000)
                .maxTotalIterations(10_000)
                .maxIncludeDepth(8)
                .maxSlotChars(100_000)
                .build())
        .build();

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .sandbox(policy)
        .limitsResolver(limits)
        .build();
```

Exact rules beat prefix rules, and the longest prefix wins. Spring Boot picks up a `SandboxLimitsResolver` bean.

If no limits resolver is supplied, each policy's own limits are used.

## Spring Boot 4

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-spring-boot-4-starter:0.3.0")
```

The starter targets Spring Boot 4.0.6. The module directory and artifact name include `boot-4` so the major version boundary is explicit. A Spring Boot 3 integration would need a separate module.

The starter auto-configures before Pebble's `PebbleAutoConfiguration`. It defines `pebbleEngine` with `PebbleForge`, so the official engine backs off. It reuses `PebbleProperties`, the loader bean, `SpringExtension`, every `Extension` bean, and the official view resolvers.

```properties
pebble.forge.components.enabled=true
pebble.forge.components.root=components
pebble.forge.components.suffix=
pebble.forge.components.max-depth=16

pebble.forge.sandbox.enabled=false
pebble.forge.sandbox.max-rendered-chars=1000000
pebble.forge.sandbox.max-loop-iterations=100000
pebble.forge.sandbox.max-total-iterations=1000000
pebble.forge.sandbox.max-include-depth=16
pebble.forge.sandbox.max-slot-chars=1000000
pebble.forge.sandbox.allow-raw-output=false
pebble.forge.sandbox.force-auto-escaping=true

pebble.forge.diagnostics.enabled=true
pebble.forge.diagnostics.context-lines=2
pebble.forge.diagnostics.expose-in-error-attributes=false

pebble.forge.security.enabled=true
pebble.forge.language.enabled=true
pebble.forge.dev-mode=false
```

The language extension is on by default. Set `pebble.forge.language.enabled=false` to parse plain Pebble.

A `SandboxPolicy` bean replaces the policy built from properties. A `SandboxPolicyResolver` bean supplies per-template allowlists and replaces the property policy. Define either a `SandboxPolicy` bean or a `SandboxPolicyResolver` bean, not both. A `SandboxLimitsResolver` bean supplies per-template resource limits. If you define your own `PebbleEngine`, the starter backs off. If you define your own `pebbleLoader` or `springExtension`, the starter uses it.

The starter adds the Pebble Spring functions to the property-built sandbox policy. It also allows `extends` and `block` so layouts work. A hand-written policy should call `SpringWebFunctions.allow(builder)` and allow the layout tags it uses.

When diagnostics are enabled, the starter wraps the official Pebble view. A render failure becomes a real 500 error dispatch, and the application error page can read `pebbleDiagnostics` or `pebbleDiagnosticsText`. The starter does not own the error page. Set `pebble.forge.diagnostics.expose-in-error-attributes=true` to add those values to Boot's error attributes. They are off by default.

## Spring Security

The security extension exposes safe functions. It never hands an `Authentication` object to a template.

| Function | Result |
| --- | --- |
| `isAuthenticated()` | True for a logged-in user. Anonymous authentication returns false. Remember-me authentication returns true. |
| `isAnonymous()` | True when authentication is anonymous or absent. |
| `isFullyAuthenticated()` | True for a non-anonymous, non-remember-me user. |
| `isRememberMe()` | True for remember-me authentication. |
| `username()` | Authentication name, or an empty string for anonymous users. |
| `hasRole("ADMIN")` | Checks `ROLE_ADMIN`. A role that already starts with `ROLE_` is used as written. |
| `hasAnyRole("ADMIN", "OWNER")` | True when any listed role matches. |
| `hasAuthority("order:read")` | Exact authority match. |
| `hasAnyAuthority("order:read", "order:write")` | True when any listed authority matches. |
| `authorities()` | List of authority strings. Empty for anonymous users. |
| `csrfToken()` | Current CSRF token value, or an empty string outside a request. |
| `csrfParameterName()` | CSRF parameter name, or `_csrf` when no token is present. |
| `csrfHeaderName()` | CSRF header name, or `X-CSRF-TOKEN` when no token is present. |

```twig
{% if hasAnyRole("ADMIN", "OWNER") %}
  <a href="/admin">Admin</a>
{% endif %}

<form method="post">
  <input type="hidden" name="{{ csrfParameterName() }}" value="{{ csrfToken() }}">
</form>
```

When the sandbox is enabled, the Boot starter adds these functions to the policy. A hand-written policy calls `SpringSecurityFunctions.allow(builder)`.

The extension registers only when Spring Security Core, Spring Security Web, Spring Web, and the servlet API are present. WebMVC on the servlet stack is the only supported web integration. WebFlux is not supported.

## Demo

The command-line demo renders a component with named slots. It shows the sandbox escaping HTML in slot data, blocking the `raw` filter, rendering `switch`, `with`, `break`, and `continue`, and printing a source-frame diagnostic for an include error.

```shell
./gradlew :pebble-forge-demo:run
```

The Spring Boot 4 demo app shows the same features in a browser, plus Spring Security, a CSRF login form, and the Pebble view resolver.

```shell
./gradlew :pebble-forge-demo-app:run
```

Open <http://localhost:8080>. Demo accounts are `user` / `password` and `admin` / `password`.

## Build

```shell
./gradlew test
./gradlew build
./gradlew javadocAll
./gradlew spotlessCheck checkstyleMain checkstyleTest
./gradlew :pebble-forge-benchmarks:benchmark
./gradlew publishToMavenLocal
```

`build` runs tests, Spotless, and Checkstyle. CI also runs `javadoc`. `javadocAll` writes an aggregate API site to `build/docs-site/index.html`. The benchmark task is manual because JVM timing is machine-specific.

## API docs

The GitHub Pages workflow in `.github/workflows/docs.yml` builds and deploys the aggregate Javadoc after Pages is enabled for the repository.

## Project

- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
- [Contract checks](docs/CONTRACTS.md)
- [Template policies](docs/TEMPLATE-POLICIES.md)
- [Diagnostics](docs/DIAGNOSTICS.md)
- [Context-aware escaping](docs/ESCAPING.md)
- [Natural HTML](docs/NATURAL-HTML.md)
- [Security policy](SECURITY.md)
- [Release checklist](docs/RELEASING.md)

## License

Apache License 2.0. See [LICENSE](LICENSE).

## Backlog

Next work, release gates, and open decisions are tracked in [docs/BACKLOG.md](docs/BACKLOG.md).

## Design

The feature matrix, scope decisions, and accepted limits are in [docs/DESIGN.md](docs/DESIGN.md).
