# Pebble Forge

[English](README.md) | [한국어](README.ko.md)

Pebble Forge is an extension pack for Pebble 4. It keeps Pebble templates valid and adds the features that other template engines do better. The 0.1 release ships template-defined components, a capability sandbox, and Spring Boot and Spring Security integration.

The name is a working title. The project is an extension library, not a Pebble fork.

The Maven group and Java package are `io.github.allbegray.pebbleforge`. They match the GitHub account `allbegray` and the Maven Central `io.github.*` namespace.

## Status

- Java 21
- Pebble 4.1.2
- Spring Boot 4.0.6
- Spring Security 7.0.8
- Version 0.1.0-SNAPSHOT, not published

The current snapshot can be installed into the local Maven repository with `./gradlew publishToMavenLocal`.

121 tests cover the core, contracts, diagnostics, the Gradle plugin, Spring Security helpers, the Boot auto-configuration, and one MockMvc request that renders a component through the official Pebble view resolver.

## Modules

| Module | Purpose |
| --- | --- |
| `pebble-forge-core` | Components, sandbox, language extensions, and the `PebbleForge.engine()` builder |
| `pebble-forge-contracts` | Build-time model variable and component prop checks |
| `pebble-forge-diagnostics` | Source frames and include traces for template errors |
| `pebble-forge-gradle-plugin` | `checkPebbleTemplates` Gradle task |
| `pebble-forge-spring` | Spring Security template functions and sandbox allowlist helpers |
| `pebble-forge-spring-boot-4-starter` | Spring Boot 4 auto-configuration, properties, and servlet integration |
| `pebble-forge-demo` | Runnable component and sandbox demo |

## Core

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-core:0.1.0-SNAPSHOT")
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

The contracts module parses templates at build time and checks model variable reads and component props.

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

A missing model variable or required component prop fails the build. An unknown prop or a dynamic component name is a warning.

```text
page.peb:4: ERROR: Variable [page] is not declared in the model schema
page.peb:8: ERROR: Component [card] requires prop [title]
page.peb:8: WARNING: Component [card] does not declare prop [footer]
```

The default template root is `src/main/resources/templates`. The default suffix is `.peb`. Set `allowAnyVariable` to `true` to check only components.

The Gradle plugin is not on the Plugin Portal yet. The `pebble-forge-contracts` module can also be used directly from Java. See [docs/CONTRACTS.md](docs/CONTRACTS.md) for the API and [known limits](docs/CONTRACTS.md#known-limits).

### Diagnostics

The diagnostics module turns a Pebble error into source frames and an include path.

```java
PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .extension(new DiagnosticsExtension())
        .build();

try {
    engine.getTemplate("page").evaluate(writer, model);
} catch (PebbleException error) {
    System.err.println(TemplateDiagnostics.builder()
            .loader(loader)
            .build()
            .render(error)
            .format());
}
```

```text
Root attribute [missing] does not exist (components/card.peb:2)
page.peb:3 includes components/card.peb
  2 | second
> 3 | {% include "components/card.peb" %}
    | ^
components/card.peb:2
  1 | card
> 2 | {{ missing }}
    | ^
```

`DiagnosticsExtension` records the include stack. Without it, the report shows the failing template only. The extends chain is not captured yet. See [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md).

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
| Method access | Dangerous classes, methods, and return types blocked. Public fields blocked. |
| Template paths | Absolute paths, `..`, `.` segments, backslashes, NUL, colon, and empty segments rejected |

Add capabilities with `allowTags`, `allowFilters`, `allowFunctions`, `allowTests`, `allowGlobalVariables`, `allowComponents`, and `allowRawOutput`. `allowOnly*` replaces a default set.

The sandbox is an in-process capability guard, not a CPU quota. A template can still burn CPU with a large user-supplied list or a recursive include. Run hostile templates in a separate process when CPU is hostile. The path jail is lexical. A symlink inside a file loader root can still point outside the root.

## Spring Boot 4

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-spring-boot-4-starter:0.1.0-SNAPSHOT")
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
pebble.forge.sandbox.allow-raw-output=false
pebble.forge.sandbox.force-auto-escaping=true

pebble.forge.security.enabled=true
pebble.forge.language.enabled=true
pebble.forge.dev-mode=false
```

The language extension is on by default. Set `pebble.forge.language.enabled=false` to parse plain Pebble.

A `SandboxPolicy` bean replaces the policy built from properties. If you define your own `PebbleEngine`, the starter backs off. If you define your own `pebbleLoader` or `springExtension`, the starter uses it.

The starter adds the Pebble Spring functions to the property-built sandbox policy. It also allows `extends` and `block` so layouts work. A hand-written policy should call `SpringWebFunctions.allow(builder)` and allow the layout tags it uses.

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

The extension registers only when Spring Security Core, Spring Security Web, Spring Web, and the servlet API are present. The servlet stack is tested. WebFlux is not verified in 0.1.

## Demo

```shell
./gradlew :pebble-forge-demo:run
```

The demo renders a component with named slots. It shows the sandbox escaping HTML in slot data, blocking the `raw` filter, rendering `switch`, `with`, `break`, and `continue`, and printing a source-frame diagnostic for an include error.

## Build

```shell
./gradlew test
./gradlew build
./gradlew publishToMavenLocal
```

## Project

- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
- [Contract checks](docs/CONTRACTS.md)
- [Diagnostics](docs/DIAGNOSTICS.md)
- [Security policy](SECURITY.md)
- [Release checklist](docs/RELEASING.md)

## License

Apache License 2.0. See [LICENSE](LICENSE).

## Backlog

Next work, release gates, and open decisions are tracked in [docs/BACKLOG.md](docs/BACKLOG.md).

## Design

The feature matrix, scope decisions, and accepted limits are in [docs/DESIGN.md](docs/DESIGN.md).
