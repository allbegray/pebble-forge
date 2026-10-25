# Pebble Forge

Pebble Forge is an extension pack for [Pebble 4](https://pebbletemplates.io). It keeps Pebble templates valid and adds the features that other template engines do better. The 0.1 release ships components with named slots, a capability sandbox, language extensions, and Spring Boot and Spring Security integration.

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
├── include
│   ├── IncludeExtension     depth-limited include tag
│   ├── ForgeIncludeNode     include rendering and trace capture
│   └── IncludeTrace         include stack and frames
├── language
│   ├── LanguageExtension    switch, break, continue, with, and a break-aware for
│   ├── ForNode              for loop that catches break and continue signals
│   ├── SwitchNode           first matching case, no fall-through
│   └── WithNode             scoped variable map
├── reload
│   └── ReloadingLoader      hashes template content into the cache key
└── sandbox
    ├── SandboxPolicy        allowlists, limits, path rules
    ├── SandboxedExtension   filters a user extension
    ├── SandboxedLoader      rejects absolute paths and parent traversal
    ├── SandboxedFileLoader  resolves real paths and rejects symlink escapes
    └── SandboxMethodAccessValidator  blocks dangerous classes, methods, and return types

pebble-forge-contracts
├── ModelSchema              allowed model variables
├── ContractChecker          parses templates and validates reads and component calls
└── ContractDiagnostic       errors and warnings with template and line

pebble-forge-diagnostics
├── TemplateDiagnostics        builds source frames from a PebbleException and IncludeTrace
└── DiagnosticReport           structured frames and terminal formatting

pebble-forge-gradle-plugin
└── CheckPebbleTemplatesTask checks every template under the configured root

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

## Language extensions

Pebble Forge adds these language features that Pebble does not ship.

| Tag | Behavior |
| --- | --- |
| `switch`, `case`, `default`, `endswitch` | Renders the first matching case. A case can list several comma-separated values. It does not fall through. |
| `break`, `continue` | Controls a `for` loop. `break` also exits a `switch`. |
| `for key, value in ...` | Destructures map entries, or an index and a value for other iterables. |
| `with { ... }` | Renders a body with extra scoped variables. |
| `with { ... } only` | Hides outer variables while keeping internal slot state, locale, and template. |

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

`for` is replaced with a break and continue aware node. It keeps `loop.index`, `loop.first`, `loop.last`, `loop.length`, `loop.revindex`, and the `else` branch. A list destructured with `for index, value` uses a zero-based index.

`with only` hides the model and extension scope. Slot content still renders because the internal slot state is copied into the isolated scope.

`break` and `continue` are lexical. The parser tracks loop and switch nesting while it parses. A tag outside a loop or switch fails at parse time. A macro that contains `break` and is called outside a loop is outside this guarantee.

`PebbleForge.engine().languageExtensions(false)` disables the language extension. The Boot starter exposes the same switch as `pebble.forge.language.enabled`.

## Sandbox

The sandbox is allowlist based. `SandboxPolicy.strict()` uses these defaults.

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
| Template paths | Absolute paths, `..`, `.` segments, backslashes, NUL, colon, and empty segments rejected |
| Symlinks | `SandboxedLoader` is lexical. `SandboxedFileLoader` resolves real paths and rejects escapes. |

`range` is not in the default function set. The `..` operator delegates to `range` at render time, so strict mode blocks `..` as well.

Add capabilities with `allowTags`, `allowFilters`, `allowFunctions`, `allowTests`, `allowGlobalVariables`, `allowComponents`, and `allowRawOutput`. The `allowOnly*` methods replace a default set.

The sandbox is an in-process capability guard, not a CPU quota. It stops capability abuse and output bombs. It does not stop a template from burning CPU with a large user-supplied list or many large slot captures. Run hostile templates in a separate process when CPU and memory are also hostile. Loop limits are per loop. The total loop budget is shared across a render, except parallel branches get a separate budget. Include depth is limited to 16 by default.

The path jail is lexical in `SandboxedLoader`. It rejects traversal in template names but does not resolve symlinks. `SandboxedFileLoader` resolves real paths and rejects symlinks that leave the root.

## Spring Boot 4

The starter targets Spring Boot 4.0.6, Spring Framework 7.0.7, Spring Security 7.0.8, and Pebble's `pebble-spring-boot-starter:4.1.2`. The module directory and artifact name include `boot-4` so the major version boundary is explicit. A Spring Boot 3 integration would need a separate module.

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-spring-boot-4-starter:0.1.0-SNAPSHOT")
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
pebble.forge.sandbox.max-loop-iterations=100000
pebble.forge.sandbox.max-total-iterations=1000000
pebble.forge.sandbox.max-include-depth=16
pebble.forge.sandbox.max-slot-chars=1000000
pebble.forge.sandbox.allow-raw-output=false
pebble.forge.sandbox.force-auto-escaping=true

pebble.forge.security.enabled=true
pebble.forge.language.enabled=true
pebble.forge.dev-mode=false
```

The language switch is on by default. Set `pebble.forge.language.enabled=false` to parse plain Pebble. A `SandboxPolicy` bean replaces the policy built from properties. If an application defines its own `PebbleEngine`, the starter backs off. If it defines its own `pebbleLoader` or `springExtension`, the starter uses those beans.

The property-built sandbox policy adds `SpringWebFunctions`, which allows `message`, `href`, and the form binding result functions. It also allows `extends` and `block` so layouts work. A hand-written policy calls `SpringWebFunctions.allow(builder)` and allows the layout tags it uses.

The official view resolver checks `instanceof ClasspathLoader` before it strips a leading slash from `pebble.prefix`. Sandboxing wraps the loader, so that check fails. The starter normalizes `pebble.prefix` once when the effective loader is a `ClasspathLoader`. This keeps the official resolver working with the wrapped loader.

## Spring Security

The security extension exposes safe functions. It never hands an `Authentication` object to a template.

| Function | Result |
| --- | --- |
| `isAuthenticated()` | True for a logged-in user. Anonymous returns false. Remember-me returns true. |
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

The Boot starter registers the extension only when Spring Security Core, Spring Security Web, Spring Web, and the servlet API are present. The servlet stack is tested with MockMvc and the official Pebble view resolver. WebFlux is not verified in 0.1.

When the sandbox is enabled, the Boot starter adds `SpringSecurityFunctions` to the policy. A hand-written policy calls `SpringSecurityFunctions.allow(builder)`.

## First release scope

Version 0.1 contains these features and the tests that prove them.

1. Components with props, default props, named slots, a default slot, a depth limit, and a clear missing-prop error.
2. A sandbox with tag, filter, function, and test allowlists, a method access validator, a path-jailed loader, and an output limit.
3. Language extensions for `switch`, `break`, `continue`, and `with`.
4. A Spring Boot starter that replaces the official engine bean and keeps the official view resolvers.
5. Safe Spring Security functions and sandbox allowlist helpers.
6. A runnable demo that renders a component, a sandbox rejection, and the language tags.

The next releases add typed contracts, diagnostics with source frames, hot reload, and context-aware escaping. Natural HTML authoring is a separate dialect loader. It does not belong in the first release because it needs a real HTML parser and a source map back to the original template.

The work queue, acceptance criteria, and release gates are in [BACKLOG.md](BACKLOG.md).

## Verification

| Area | Tests | What it proves |
| --- | --- | --- |
| Components | 15 | Real templates, real engine, exact output, slot isolation |
| Language | 28 | Switch, break, continue, with, destructuring, multi-value cases, sandbox on and off |
| Iteration budget | 3 | Sequential loops, nested loops, per-render reset |
| For compatibility | 9 | Forge `for` output compared with plain Pebble across iterables and loop variables |
| Dev mode | 4 | File edits, includes, sandbox, and cache-off comparison |
| Contracts | 16 | Model variables, component props, dynamic names, parse errors, schema bypass |
| Diagnostics | 7 | Source frames, include stacks, nested includes, parse errors, missing source |
| Gradle plugin | 3 | TestKit projects pass and fail through `checkPebbleTemplates` |
| Sandbox | 22 | Real templates, real engine, whole-render assertions, loop limit |
| Include depth | 4 | Limit enforcement, zero limit, unlimited mode |
| File loader | 6 | Root reads, traversal, absolute paths, symlinks inside and outside |
| Spring Security | 10 | Roles, authorities, anonymous, remember-me, CSRF, sandbox on and off |
| Boot auto-config | 9 | Application context, official auto-config ordering, policy override, language switch, dev mode |
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
| Maven group and Java package | `io.github.allbegray.pebbleforge` | Matches the future GitHub account and the Maven Central `io.github.*` namespace |
| Fork or depend | Depend on Pebble 4.1.2 | Keeps compatibility and upgrade path |
| Feature shape | Extensions with explicit toggles | Components and sandbox are opt-in. Language extensions are on by default and can be disabled. |
| Components | Template-defined, rendered through `includeTemplate` | Reuses Pebble caching, inheritance, and locale |
| Language extensions | Token parsers plus a custom break-aware `for` node | Pebble has no switch, break, continue, or with. Catching signals inside the loop is the only way to continue correctly. |
| Slots | Captured as pre-rendered `SafeString` | No double escaping, no new AST evaluator |
| Slot safety | Only captured `SafeString` values are emitted | A with block cannot forge `__slots` and bypass escaping |
| Component suffix | Configurable, empty when the loader owns the suffix | Avoids a double suffix |
| Sandbox default | Deny by allowlist | A missing entry fails closed |
| Path safety | Wrap the loader, reject `..` and absolute names | Include and extends both use the loader |
| Method safety | Class, package, name, and return-type rules | The default blacklist is too small |
| Spring Boot | Replace the `pebbleEngine` bean before the official auto-configuration | Reuses official properties, extensions, and view resolvers |
| Spring Security | Safe functions over `SecurityContextHolder` and the CSRF request attribute | Templates never receive an `Authentication` object |
| Contracts | Build-time presence checks for model variables and component props | Catches common errors without a full Java type checker |
| Diagnostics | Custom include parser plus a source-frame formatter | Captures the include path without replacing the renderer |
| Dev mode | Content hash in the loader cache key | Reloads edited templates without rebuilding the engine |
| Language follow-ups | Comma-separated case values, key/value destructuring, local-only with | Fits the existing tags without a new expression language |
| Test style | Real templates, real engine, real output | Proves the feature, not a mock |

## Accepted limits

- Slot output cannot be concatenated or filtered without a second escape. Emit slots directly.
- `switch` has no fall-through. Each case is independent.
- `break` and `continue` are validated lexically. A macro that contains `break` and is called outside a loop can still let the signal escape.
- The break-aware `for` replaces Pebble's built-in `for`. It mirrors the current loop variables, map, array, enumeration, parallel, and `else` behavior. A future Pebble change to `for` must be mirrored here.
- Component depth uses a static thread-local counter. Nested renders across different engines on the same thread share that counter. The counter resets at depth zero.
- A sandbox policy is fixed per engine. One engine that serves multiple trust levels applies the same policy to all of them. Build separate engines for separate trust levels.
- The output limit caps written output. Slot capture has a separate per-component budget, and captured content is counted again when written to the main output.
- The path jail is lexical and does not resolve symlinks.
- The Spring Security extension requires the servlet stack. Core-only or WebFlux security is not covered.
- Contract checks are presence-based and do not follow includes or extends recursively. See [CONTRACTS.md](CONTRACTS.md) for the full list of limits.
- Diagnostics trace includes but not extends. The caret points at the first non-whitespace character because Pebble does not expose a column. See [DIAGNOSTICS.md](DIAGNOSTICS.md).
- Dev mode reads and hashes each template on lookup and leaves old cache keys behind. It is for development, not long-running production processes. Classpath resources inside a jar cannot change.
- The total loop budget is keyed by the render scope chain. Parallel branches deep-copy that scope chain and get a separate budget.
- `with only` hides model and extension globals. Internal slot state, locale, and template are preserved. `_context` becomes empty in the isolated scope.
- `for key, value` uses the map key for entries and a zero-based index for other iterables.

## Open questions

- Should a component allow unknown props? Current answer is yes. A warning mode is a later addition.
- Should the contract schema move from the Gradle DSL to JSON or YAML?
- Should the Gradle plugin be published to the Plugin Portal?
- Should the sandbox replace the built-in `include` tag with a depth-guarded version? The output limit covers the easy bomb, not recursive CPU.
- Should the component root be one directory or a list of loaders? A list is needed for shared component libraries.
- Should component depth use per-engine state instead of a static thread-local?
- Should the Spring integration cover WebFlux and reactive CSRF?
- Should diagnostics capture the extends hierarchy and integrate with Spring Boot error handling?
