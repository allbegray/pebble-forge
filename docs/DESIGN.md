# Pebble Forge

Pebble Forge is an extension pack for [Pebble 4](https://pebbletemplates.io). It keeps Pebble templates valid and adds the features that other template engines do better. The 0.1 release ships components with named slots, a capability sandbox, and Spring Boot and Spring Security integration.

The name is a working title.

## Why extend Pebble

Pebble already has the right base. It has Twig-like syntax, template inheritance, macros, autoescaping, i18n, a small extension API, and good rendering speed. A new engine would spend its first year rebuilding that base.

A fork would be worse. It splits the ecosystem, breaks Spring Boot integration, and turns every Pebble release into a merge.

Pebble Forge depends on `io.pebbletemplates:pebble` and registers normal `Extension` implementations. A project can turn each feature on or off. If a project turns everything off, it gets plain Pebble.

## What to borrow

| Engine | Borrow | Leave behind |
| --- | --- | --- |
| Pebble | Syntax, inheritance, macros, i18n, autoescape, extension API, cache | Dynamic-only checking, weak composition, limited sandbox |
| Thymeleaf | Component fragments, natural HTML idea, Spring familiarity | Large attribute dialect, slow runtime, heavy context |
| FreeMarker | Macro power, whitespace control, explicit output formats | Verbose syntax, weak browser preview |
| JTE | Compile-time checks, plain Java expressions, hot reload | Mandatory code generation for every template |
| Qute | Strict resolution, type-safe expressions, dev mode, variants | Quarkus coupling, custom section syntax everywhere |
| Rocker | Static analysis, fast output | Java-only template syntax |
| Mustache | Logic-less mode, cross-language partials | Too little logic for application pages |
| Handlebars | Helpers, block helpers, partials | JavaScript-only runtime |
| Liquid | Safe-by-default execution, allowlists, merchant editing | Too little logic for application pages |
| Blade | Components, slots, layouts, directives | PHP-specific runtime |
| JSX, Svelte, Astro | Component composition, islands, streaming | Client runtime in the server template layer |
| Razor | Typed model, tag helpers | C# coupling |

The borrow list is not a feature checklist. Features earn a place only when they fit Pebble's dynamic core and do not force a new language.

## Three modes

One engine cannot be maximally expressive and maximally safe at the same time. Pebble Forge makes the trade-off explicit with three modes. A project picks a mode per engine.

| Mode | User | Enabled |
| --- | --- | --- |
| Authoring | Application developers | Full Pebble, components, hot reload, diagnostics |
| Typed | Application developers with a known model | Contracts, strict variables, typed components |
| Untrusted | End users, tenants, workflow authors | Tag and filter allowlist, method allowlist, path jail, output limit, no raw output |

The mode is a policy object. It does not change the expression language. A template that renders in Authoring mode can render in Untrusted mode only if the policy allows every tag, filter, function, and method it uses.

## Architecture

```
pebble-forge-core
├── PebbleForge              builder that returns a normal PebbleEngine
├── component
│   ├── Components           configuration: root, suffix, depth
│   ├── ComponentNode        renders a component template with props and slots
│   ├── SlotNode             captures a named block
│   ├── PropsNode            validates required props and applies defaults
│   └── SlotFunction         returns captured slot HTML without double escaping
└── sandbox
    ├── SandboxPolicy        allowlists, limits, path rules
    ├── SandboxedExtension   filters a user extension
    ├── SandboxedLoader      rejects absolute paths and parent traversal
    └── SandboxMethodAccessValidator  blocks dangerous classes, methods, and return types

pebble-forge-spring
├── SpringWebFunctions       sandbox allowlist for Pebble's Spring functions
└── security
    ├── SpringSecurityExtension  safe authentication, authorization, and CSRF functions
    └── SpringSecurityFunctions  sandbox allowlist for the security functions

pebble-forge-spring-boot-4-starter
├── PebbleForgeAutoConfiguration  replaces the official engine bean
└── PebbleForgeProperties         binds pebble.forge.*

pebble-forge-demo
└── Demo                     renders a component under a sandbox policy
```

The public builder is the only entry point:

```java
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
        .allowFilters("length", "upper")
        .maxRenderedChars(1_000_000)
        .build())
    .build();
```

`Components.suffix` is appended by Forge. If the loader already appends a suffix, set the component suffix to `""`. The Spring Boot starter does this. A double suffix is the most common setup mistake.

## Components

Components are ordinary Pebble templates in the component root.

```twig
{# components/card.peb #}
{% props title, subtitle = "" %}
<article class="card">
  <h2>{{ title }}</h2>
  {% if subtitle %}<p>{{ subtitle }}</p>{% endif %}
  <div class="body">{{ slot("body") }}</div>
</article>
```

Callers pass props with a map and fill slots with blocks.

```twig
{% component "card" with {"title": page.title} %}
  {% slot "body" %}
    <p>{{ page.summary }}</p>
  {% endslot %}
{% endcomponent %}
```

The component body renders in the caller's context. Props and slots render in the component's context. A missing required prop fails with the component name and the source line. A body without explicit slot tags becomes the default slot.

Slots are captured as pre-rendered `SafeString` values. `ComponentNode.accept` walks the body so autoescaping applies to slot content before capture. Direct output of `slot()` escapes once. Concatenating or filtering a slot turns it back into a plain string and escapes it again.

## Sandbox

The sandbox is allowlist based. `SandboxPolicy.strict()` uses these defaults.

| Capability | Default |
| --- | --- |
| Tags | `if`, `for`, `set`, `verbatim`, `include`, `component`, `slot`, `props` |
| Filters | `escape`, `default`, `length`, `join`, `upper`, `lower`, `capitalize`, `title`, `trim`, `replace`, `slice`, `first`, `last`, `reverse`, `sort`, `split`, `abs`, `numberformat`, `date`, `abbreviate`, `urlencode` |
| Functions | `slot`, `i18n`, `max`, `min` |
| Tests | `empty`, `null`, `iterable`, `even`, `odd`, `map` |
| Raw output | Denied |
| Output limit | 1,000,000 characters |
| Method access | Dangerous classes, methods, and return types blocked. Public fields blocked. |
| Template paths | Absolute paths, `..`, `.` segments, backslashes, NUL, colon, and empty segments rejected |

`range` is not in the default function set. The `..` operator delegates to `range` at render time, so strict mode blocks `..` as well.

Add capabilities with `allowTags`, `allowFilters`, `allowFunctions`, `allowTests`, `allowGlobalVariables`, `allowComponents`, and `allowRawOutput`. The `allowOnly*` methods replace a default set.

The sandbox is an in-process capability guard, not a CPU quota. It stops capability abuse and output bombs. It does not stop a template from burning CPU with a large user-supplied list, a recursive include, or many large slot captures. Run hostile templates in a separate process when CPU and memory are also hostile.

The path jail is lexical. It rejects traversal in template names. It does not resolve symlinks. Use a loader root with no symlinks, or a custom loader that enforces canonical paths.

## Spring Boot 4

The starter targets Spring Boot 4.0.6, Spring Framework 7.0.7, Spring Security 7.0.8, and Pebble's `pebble-spring-boot-starter:4.1.2`. The module directory and artifact name include `boot-4` so the major version boundary is explicit. A Spring Boot 3 integration would need a separate module.

```kotlin
implementation("io.pebbleforge:pebble-forge-spring-boot-4-starter:0.1.0-SNAPSHOT")
```

`PebbleForgeAutoConfiguration` is ordered before Pebble's `PebbleAutoConfiguration`. It defines a `pebbleEngine` bean with `PebbleForge`, so the official engine bean backs off. It reuses `PebbleProperties`, the `pebbleLoader` bean, the `SpringExtension` bean, every `Extension` bean, and the official servlet or reactive view resolvers.

Properties:

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
```

A `SandboxPolicy` bean replaces the policy built from properties. If an application defines its own `PebbleEngine`, the starter backs off. If it defines its own `pebbleLoader` or `springExtension`, the starter uses those beans.

The property-built sandbox policy adds `SpringWebFunctions`, which allows `message`, `href`, and the form binding result functions. It also allows `extends` and `block` so layouts work. A hand-written policy calls `SpringWebFunctions.allow(builder)` and allows the layout tags it uses.

The official view resolver checks `instanceof ClasspathLoader` before it strips a leading slash from `pebble.prefix`. Sandboxing wraps the loader, so that check fails. The starter normalizes `pebble.prefix` once when the effective loader is a `ClasspathLoader`. This keeps the official resolver working with the wrapped loader.

## Spring Security

The security extension exposes safe functions. It never hands an `Authentication` object to a template.

| Function | Result |
| --- | --- |
| `isAuthenticated()` | True for a logged-in user. Anonymous authentication returns false. |
| `username()` | Authentication name, or an empty string for anonymous users. |
| `hasRole("ADMIN")` | Checks `ROLE_ADMIN`. A role that already starts with `ROLE_` is used as written. |
| `hasAuthority("order:read")` | Exact authority match. |
| `authorities()` | List of authority strings. Empty for anonymous users. |
| `csrfToken()` | Current CSRF token value, or an empty string outside a request. |
| `csrfParameterName()` | CSRF parameter name, or `_csrf` when no token is present. |
| `csrfHeaderName()` | CSRF header name, or `X-CSRF-TOKEN` when no token is present. |

```twig
{% if hasRole("ADMIN") %}
  <a href="/admin">Admin</a>
{% endif %}

<form method="post">
  <input type="hidden" name="{{ csrfParameterName() }}" value="{{ csrfToken() }}">
</form>
```

The Boot starter registers the extension only when Spring Security Core, Spring Security Web, Spring Web, and the servlet API are present. The servlet stack is tested with MockMvc and the official Pebble view resolver. WebFlux is not verified in 0.1.

When the sandbox is enabled, the Boot starter adds `SpringSecurityFunctions` to the policy. A hand-written policy calls `SpringSecurityFunctions.allow(builder)`.

## First release scope

Version 0.1 contains these features and the tests that prove them.

1. Components with props, default props, named slots, a default slot, a depth limit, and a clear missing-prop error.
2. A sandbox with tag, filter, function, and test allowlists, a method access validator, a path-jailed loader, and an output limit.
3. A Spring Boot starter that replaces the official engine bean and keeps the official view resolvers.
4. Safe Spring Security functions and sandbox allowlist helpers.
5. A runnable demo that renders a component inside a sandbox policy.

The next releases add typed contracts, diagnostics with source frames, hot reload, and context-aware escaping. Natural HTML authoring is a separate dialect loader. It does not belong in the first release because it needs a real HTML parser and a source map back to the original template.

## Verification

| Area | Tests | What it proves |
| --- | --- | --- |
| Components | 11 | Real templates, real engine, exact output |
| Sandbox | 20 | Real templates, real engine, whole-render assertions |
| Spring Security | 5 | Real security context, CSRF request, sandbox on and off |
| Boot auto-config | 6 | Application context, official auto-config ordering, policy override |
| Boot web | 1 | MockMvc request through the official Pebble view resolver |

## Non-goals

- A new expression syntax.
- A client-side runtime.
- Replacing Pebble's parser.
- Java code generation for every template.
- A promise that in-process sandboxing stops CPU exhaustion.
- A WebFlux security integration in 0.1.

## Decisions

| Decision | Choice | Reason |
| --- | --- | --- |
| Fork or depend | Depend on Pebble 4.1.2 | Keeps compatibility and upgrade path |
| Feature shape | Opt-in extensions | Plain Pebble stays plain |
| Components | Template-defined, rendered through `includeTemplate` | Reuses Pebble caching, inheritance, and locale |
| Slots | Captured as pre-rendered `SafeString` | No double escaping, no new AST evaluator |
| Component suffix | Configurable, empty when the loader owns the suffix | Avoids a double suffix |
| Sandbox default | Deny by allowlist | A missing entry fails closed |
| Path safety | Wrap the loader, reject `..` and absolute names | Include and extends both use the loader |
| Method safety | Class, package, name, and return-type rules | The default blacklist is too small |
| Spring Boot | Replace the `pebbleEngine` bean before the official auto-configuration | Reuses official properties, extensions, and view resolvers |
| Spring Security | Safe functions over `SecurityContextHolder` and the CSRF request attribute | Templates never receive an `Authentication` object |
| Test style | Real templates, real engine, real output | Proves the feature, not a mock |

## Accepted limits

- Slot output cannot be concatenated or filtered without a second escape. Emit slots directly.
- Component depth uses a static thread-local counter. Nested renders across different engines on the same thread share that counter. The counter resets at depth zero.
- A sandbox policy is fixed per engine. One engine that serves multiple trust levels applies the same policy to all of them. Build separate engines for separate trust levels.
- The output limit caps written output. Slot capture uses a separate cap per slot, so many large slots can allocate memory before the main output limit fails.
- The path jail is lexical and does not resolve symlinks.
- The Spring Security extension requires the servlet stack. Core-only or WebFlux security is not covered.

## Open questions

- Should a component allow unknown props? Current answer is yes. A warning mode is a later addition.
- Should typed contracts live in the core or a separate module? They need a model registry and a Gradle task, so a separate module is likely.
- Should the sandbox replace the built-in `include` tag with a depth-guarded version? The output limit covers the easy bomb, not recursive CPU.
- Should the component root be one directory or a list of loaders? A list is needed for shared component libraries.
- Should component depth use per-engine state instead of a static thread-local?
- Should the Spring integration cover WebFlux and reactive CSRF?
