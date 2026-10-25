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
│   ├── Components           configuration: roots, namespaces, suffix, depth
│   ├── ComponentNode        renders a component template with props and slots
│   ├── SlotNode             captures a named block
│   ├── PropsNode            validates required props and applies defaults
│   └── SlotFunction         returns captured slot HTML without double escaping
├── include
│   ├── IncludeExtension     depth-limited include tag
│   ├── ForgeIncludeNode     include rendering and trace capture
│   ├── IncludeNodeView      read-only include target, with arguments, and line
│   └── IncludeTrace         include-only compatibility view of the trace
├── inheritance
│   ├── ExtendsExtension     replaces extends with a trace-aware node
│   ├── ForgeExtendsNode     extends tracing, cycle detection, and parent loading
│   └── ForgeExtendsTokenParser  delegates to Pebble's extends parser
├── escape
│   └── ContextEscaping      html_attr and url strategies on Pebble's escaper
├── language
│   ├── LanguageExtension    switch, break, continue, with, and a break-aware for
│   ├── ForNode              for loop that catches break and continue signals
│   ├── SwitchNode           first matching case, no fall-through
│   └── WithNode             scoped variable map
├── reload
│   └── ReloadingLoader      hashes template content into the cache key
├── trace
│   └── TemplateTrace        chronological include and extends frames
└── sandbox
    ├── SandboxPolicy        allowlists, limits, path rules
    ├── SandboxPolicyResolver resolves allowlists by template name
    ├── SandboxLimits        per-template loop, include, and slot limits
    ├── SandboxLimitsResolver resolves limits by template name
    ├── PolicyAware*         per-template tag, filter, function, test, and method checks
    ├── SandboxedExtension   filters and wraps a user extension
    ├── SandboxedLoader      rejects absolute paths and parent traversal
    ├── SandboxedFileLoader  resolves real paths and rejects symlink escapes
    └── SandboxMethodAccessValidator  blocks dangerous classes, methods, and return types

pebble-forge-contracts
├── ModelSchema              allowed model variables and their types
├── ModelType                object properties, arrays, scalars, and any
├── ContractChecker          single-root and whole-set contract entry points
├── CheckSession             parse, analyze, root inference, scoped evaluation, attribute shapes
├── Scope                    visible names plus a completeness flag
├── ContractVisitor          reads, attribute paths, locals, blocks, component calls, include/extends edges
├── TemplateScan             context-free facts, block lines, attribute reads, typed failures
└── ContractDiagnostic       errors and warnings with template and line

pebble-forge-diagnostics
├── TemplateDiagnostics        builds source frames from a PebbleException and TemplateTrace
└── DiagnosticReport           structured frames and terminal formatting

pebble-forge-cli
├── PebbleForgeCli          standalone render entry point and exit codes
├── CliArguments            option parser
├── CliOptions              typed CLI input
└── ModelLoader             inline JSON and JSON/YAML file models

pebble-forge-natural-html
├── NaturalHtml                loader entry point and analyze()
├── NaturalHtmlLoader          reads, rewrites, and delegates to Pebble
├── NaturalHtmlAnalyzer        expression classification, jsoup validation, rewrite
├── HtmlRegions                allowed and forbidden HTML regions
├── ExpressionScanner          string- and brace-aware {{ }} boundaries
└── SourceMap                  insert-only generated-to-original offset map

pebble-forge-gradle-plugin
├── CheckPebbleTemplatesTask  walks once, applies root/partial policy, and calls checkAll
└── RenderPebbleTemplateTask  renders one template with a model and prints diagnostics

pebble-forge-spring
├── SpringWebFunctions       sandbox allowlist for Pebble's Spring functions
└── security
    ├── SpringSecurityExtension  safe authentication, authorization, and CSRF functions
    └── SpringSecurityFunctions  sandbox allowlist for the security functions

pebble-forge-spring-boot-4-starter
├── PebbleForgeAutoConfiguration      replaces the official engine bean
├── PebbleForgeDiagnosticsAutoConfiguration  wraps the Pebble view and error attributes
├── PebbleDiagnosticsView             turns render failures into a 500 error dispatch
├── PebbleDiagnosticsErrorAttributes  adds pebbleDiagnostics and pebbleDiagnosticsText
└── PebbleForgeProperties             binds pebble.forge.*

pebble-forge-demo
└── Demo                     renders a component under a sandbox policy

pebble-forge-demo-app
├── DemoApplication          Spring Boot 4 MVC application
├── DemoController           home, components, language, admin, and login pages
└── SecurityConfig           in-memory users and form login

pebble-forge-benchmarks
└── five JMH states          raw, forge plain, component, sandbox, language

pebble-forge-fuzz
└── property tests           parser, path jail, allowlists, minimizer, corpus
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

`Components` stores a namespace-keyed root map. The empty namespace is the default root and is used by an unqualified component name. A named root uses `namespace:name`. Duplicate namespaces and duplicate normalized root paths fail at build time. Jar components are ordinary classpath resources, so the loader resolves them without a separate component classloader.

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

## Contract checks

The contracts module parses each template once into context-free facts. A node visitor records root and block reads, local declarations, component calls, and include and extends edges. An include edge carries the literal and resolved target, resolution errors, literal `with` keys, dynamic flags, the include line, and a lexical snapshot of the caller's declared names. An extends edge carries the resolved parent and the owning line.

A check session has three phases. It parses every requested template, analyzes every parsed template for structural errors, and evaluates roots under a `Scope`. Evaluation checks reads with a source-order flow snapshot, follows static includes with `caller scope + lexical visible names at the call + literal with keys`, and follows static extends parents with the child's visible declarations. `if` and `switch` branches join by intersection. A component is evaluated closed over props and the model. Block ownership excludes declarations from a parent block that a child overrides, while root declarations flow both ways. Extends summaries publish only names definitely visible at the end of the root or block flow; include propagation is a call-site snapshot. Evaluation is memoized by template, scope, and overridden block set.

A template with no incoming static include or extends edge is a root. Direct component entry points and static component call targets are roots. A nested template under the component root that is only included is a partial. Explicit `rootTemplates` force root status; `partialTemplates` remove a template from root inference. A non-component, non-partial template that no root reaches is checked as a fallback root. Include, extends, and component cycles are errors. Dynamic include targets and dynamic `with` keys warn, and a dynamic with key makes the child scope incomplete.

## Context-aware escaping

Pebble Forge registers two strategies on Pebble's `EscapeFilter`: `html_attr` for attribute values and `url` for whole URLs in an HTML attribute. `html`, `js`, and `css` stay Pebble's built-ins. `PebbleForge.engine()` registers the two new strategies before `build()`; plain Pebble builders call `ContextEscaping.register`.

The strategies are explicit. The template author chooses the sink. `html_attr` escapes markup characters, whitespace, `=`, backtick, NUL, and C0 controls. `url` validates the scheme against `http`, `https`, `mailto`, and `tel`, rejects others with `#`, and then applies `html_attr`. It does not percent-encode or validate hosts.

`SafeString` bypasses every strategy. Slots and `raw` output are trusted HTML, not values that can be reused in URL, JavaScript, or CSS sinks.

## Natural HTML

`pebble-forge-natural-html` is an opt-in loader. It reads a browser-openable `.html` file, uses jsoup 1.20.1 with position tracking to validate the document and find HTML regions, scans Pebble print expressions, and inserts a context escape around each expression. Pebble still parses and renders the rewritten source.

The first slice is expressions only. Text and RCDATA get `html`, quoted attributes get `html_attr`, and quoted URL attributes get `url`. All `{% ... %}` tags, Pebble comments, script/style interpolation, comments, tag and attribute names, unquoted values, event handlers, `srcdoc`, `srcset`, `ping`, and foreign-content subtrees are rejected. The insertion is newline-free, so Pebble errors retain original line numbers. `SourceMap` maps generated offsets back to original offsets and columns.

This is PF-202.1. Cross-template composition and JavaScript/CSS contexts remain open.

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
| Template paths | Absolute paths, `..`, `.` segments, backslashes, NUL, colon, whitespace, `{`, `}`, `%`, and empty segments rejected |
| Symlinks | `SandboxedLoader` is lexical. `SandboxedFileLoader` resolves real paths and rejects escapes. |

`range` is not in the default function set. The `..` operator delegates to `range` at render time, so strict mode blocks `..` as well.

Add capabilities with `allowTags`, `allowFilters`, `allowFunctions`, `allowTests`, `allowGlobalVariables`, `allowComponents`, and `allowRawOutput`. The `allowOnly*` methods replace a default set.

The sandbox is an in-process capability guard, not a CPU quota. It stops capability abuse and output bombs. It does not stop a template from burning CPU with a large user-supplied list or many large slot captures. Run hostile templates in a separate process when CPU and memory are also hostile. Loop limits are per loop. The total loop budget is shared across a render, except parallel branches get a separate budget. Include depth is limited to 16 by default.

The path jail is lexical in `SandboxedLoader`. It rejects traversal in template names but does not resolve symlinks. `SandboxedFileLoader` resolves real paths and rejects symlinks that leave the root.

Allowlists, method access, and path validation can vary by template name through `SandboxPolicyResolver`. The engine registers the union of all tags, filters, functions, and tests. Policy-aware token parsers check tags at parse time. Policy-aware filter, function, and test wrappers check at render time. The attribute resolver routes each policy to its own Pebble resolver, so the member cache cannot carry access across policies. An included template uses its own policy. The include tag itself uses the caller's policy. An extending child and its parent use their own policies.

The output limit is the strictest finite limit across policies. Autoescaping is on if any policy forces it. Global variables are exposed only when every policy allows them. `escape` is required by autoescaping. `default` and `defined` keep their concrete Pebble classes when a policy denies them, so strict-variable behavior still works.

Resource limits can vary by template name through `SandboxLimitsResolver`. Exact rules beat prefix rules, and the longest prefix wins. If no limits resolver is supplied, each policy's own limits are used. See [TEMPLATE-POLICIES.md](TEMPLATE-POLICIES.md).

## Spring Boot 4

The starter targets Spring Boot 4.0.6, Spring Framework 7.0.7, Spring Security 7.0.8, and Pebble's `pebble-spring-boot-starter:4.1.2`. The module directory and artifact name include `boot-4` so the major version boundary is explicit. A Spring Boot 3 integration would need a separate module.

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-spring-boot-4-starter:0.3.0")
```

`PebbleForgeAutoConfiguration` is ordered before Pebble's `PebbleAutoConfiguration`. It defines a `pebbleEngine` bean with `PebbleForge`, so the official engine bean backs off. It reuses `PebbleProperties`, the `pebbleLoader` bean, the `SpringExtension` bean, every `Extension` bean, and the official servlet view resolver. WebMVC is the only supported web stack.

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

The language switch is on by default. Set `pebble.forge.language.enabled=false` to parse plain Pebble. A `SandboxPolicy` bean replaces the policy built from properties. A `SandboxPolicyResolver` bean supplies per-template allowlists and replaces the property policy. Define either a `SandboxPolicy` bean or a resolver bean, not both. A `SandboxLimitsResolver` bean supplies per-template resource limits. If an application defines its own `PebbleEngine`, the starter backs off. If it defines its own `pebbleLoader` or `springExtension`, the starter uses those beans.

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

The Boot starter registers the extension only when Spring Security Core, Spring Security Web, Spring Web, and the servlet API are present. WebMVC on the servlet stack is tested with MockMvc and the official Pebble view resolver. WebFlux is not supported.

When the sandbox is enabled, the Boot starter adds `SpringSecurityFunctions` to the policy. A hand-written policy calls `SpringSecurityFunctions.allow(builder)`.

## First release scope

Version 0.1 contains these features and the tests that prove them.

1. Components with props, default props, named slots, a default slot, a depth limit, and a clear missing-prop error.
2. A sandbox with per-template tag, filter, function, and test allowlists, a method access validator, a path-jailed loader, and an output limit.
3. Language extensions for `switch`, `break`, `continue`, and `with`.
4. A Spring Boot starter that replaces the official engine bean and keeps the official view resolvers.
5. Safe Spring Security functions and sandbox allowlist helpers.
6. A runnable demo that renders a component, a sandbox rejection, and the language tags.

The next releases add a diagnostics CLI, Natural HTML control flow and composition, and JavaScript/CSS contexts. Natural HTML is a separate dialect loader because it needs a real HTML parser and a source map back to the original template.

The work queue, acceptance criteria, and release gates are in [BACKLOG.md](BACKLOG.md).

## Verification

| Module | Tests | What it proves |
| --- | --- | --- |
| Core | 215 | Components, roots, language extensions, sandbox, escaping, include/extends traces, dev mode |
| Contracts | 164 | Model variables, typed shapes, source-order flow, ordered cross-template blocks, lexical include and extends scopes, block ownership, component props |
| Diagnostics | 23 | Source frames, structured INCLUDE/EXTENDS/ERROR frames, source-mapped errors, include and extends stacks, parse errors, cycle and stale-frame handling |
| Natural HTML | 130 | Rewrite, rejection matrix, source map, diagnostics integration, XSS fixtures, jsoup preview, sandbox |
| Gradle plugin | 45 | Contracts policy, typed JSON/YAML schemas, named roots, Plugin Portal metadata, render diagnostics, direct task registration |
| CLI | 14 | Render output, JSON/YAML models, source frames, path and usage errors, exit codes |
| Spring Security | 10 | Roles, authorities, anonymous, remember-me, CSRF, sandbox on and off |
| Boot starter | 39 | Auto-configuration, web resolution, diagnostics, component roots |
| Benchmarks | 5 | Smoke renders for every measured template |
| Demo app | 8 | MVC pages, admin redirect, authenticated admin, CSRF login |
| Fuzz | 35 | 600 jqwik trials, path jail, allowlists, minimizer, corpus |
| **Total** | **688** | |

## Non-goals

- A new expression syntax.
- A client-side runtime.
- Replacing Pebble's parser.
- Java code generation for every template.
- A promise that in-process sandboxing stops CPU exhaustion.
- WebFlux support. WebMVC on the servlet stack is the only supported web integration.

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
| Web stack | WebMVC on the servlet stack only | WebFlux is permanently out of scope |
| Contracts | Build-time checks with source-order flow, ordered cross-template blocks, and typed JSON/YAML shapes | Catches common errors without a full Java type checker. Direct reads and extends summaries are flow-sensitive; include propagation is a call-site snapshot; components stay closed. Types are attribute shapes, not expression types. |
| Escaping | Explicit context strategies plus pinned Pebble built-ins | Keeps the template author in control and lets Natural HTML choose the sink without replacing Pebble's parser |
| Natural HTML | Loader that rewrites expressions, then Pebble parses | Browser-openable source, context safety, and line-preserving diagnostics without a parser fork |
| Component roots | Namespace-keyed root map | One validated model for multiple roots, jar resources, and conflict detection |
| Contract types | Typed JSON/YAML shapes plus static literal attribute paths | Checks model shape without claiming expression-level type inference |
| Plugin publishing | Gradle Plugin Portal metadata for both IDs, excluded from Maven Central aggregation | Keeps plugin distribution separate from library artifacts |
| Quality | Spotless misc checks plus Checkstyle and Javadoc `-Werror` | Catches hygiene and API documentation regressions without a mass reformat |
| Diagnostics | Unified include/extends trace plus a source-frame formatter with typed frame kinds and source-mapped errors | Captures the runtime chain without replacing the renderer and gives consumers structured targets |
| Boot diagnostics | View decorator plus optional `ErrorAttributes` adapter | Turns render failures into a 500 dispatch without owning the error page |
| Gradle diagnostics | Separate `renderPebbleTemplate` task | Renders a model without a JSON dependency and prints source frames on failure |
| CLI | Standalone render command with Jackson 3 models | Gives terminal users the same source-frame diagnostics without Gradle or a JSON-to-DSL bridge |
| Dev mode | Content hash in the loader cache key | Reloads edited templates without rebuilding the engine |
| Language follow-ups | Comma-separated case values, key/value destructuring, local-only with | Fits the existing tags without a new expression language |
| Template limits | Resolver by exact name and longest prefix | Different trust levels can share one engine with different resource budgets |
| Per-template policies | Union parser plus policy-aware tag, filter, function, test, and method wrappers | Keeps one template cache while separating trust levels |
| Test style | Real templates, real engine, real output | Proves the feature, not a mock |

## Accepted limits

- Slot output cannot be concatenated or filtered without a second escape. Emit slots directly.
- `switch` has no fall-through. Each case is independent.
- `break` and `continue` are validated lexically. A macro that contains `break` and is called outside a loop can still let the signal escape.
- The break-aware `for` replaces Pebble's built-in `for`. It mirrors the current loop variables, map, array, enumeration, parallel, and `else` behavior. A future Pebble change to `for` must be mirrored here.
- Component depth uses a static thread-local counter. Nested renders across different engines on the same thread share that counter. The counter resets at depth zero.
- Per-template policies share one engine. The parser is the union of all policies, so a tag or filter is compiled even when only one policy allows it. Render-time wrappers reject it for the other templates.
- The embed parser is a copy of Pebble's embed parser. It wraps overridden blocks with the policy owner. A future Pebble change to embed parsing must be mirrored here.
- The current policy owner is carried in a thread-local. Blocks, includes, macros, embeds, and components set and restore it. The method validator fails closed when no owner is set.
- The output limit, autoescaping, and global variables remain engine-wide. The output limit is the strictest finite policy limit, autoescaping is on if any policy forces it, and globals are exposed only when every policy allows them.
- The output limit caps written output. Slot capture has a separate per-component budget, and captured content is counted again when written to the main output.
- The path jail is lexical and does not resolve symlinks.
- The Spring Security extension requires WebMVC on the servlet stack. WebFlux is not supported.
- Per-template limits cover loops, include depth, and slot capture through `SandboxLimitsResolver`. If no resolver is supplied, each policy's limits are used. See [TEMPLATE-POLICIES.md](TEMPLATE-POLICIES.md).
- Contract checks use source-order flow inside a template. Direct reads before a set, in sibling branches, or after loop/with/macro scopes are rejected. Cross-template extends summaries publish only names definitely visible at the end of the root or block flow. Include propagation is a call-site snapshot. A root read can still see a non-overridden block declaration regardless of position.
- The typed JSON/YAML contract schema describes object properties and array item shapes. It has no union, optional, or nullability types. `required` is parsed but not enforced. Attribute checks stop at arrays and skip dynamic or numeric index segments. `$schema` is accepted and ignored.
- Root reads see a local block declaration only when the block starts at or before the read line. Parent reads and includes see a descendant block declaration only after the parent block starts. Ancestor block visibility remains line-blind upward. A switch without a default keeps the no-match path even when every declared case sets a name.
- A non-component, non-partial template that is not reachable from a root is checked as a fallback root. A nested template under the component root that is only included is treated as a partial.
- Components stay closed over props and the model at check time, even though runtime `includeTemplate` shares the caller scope chain.
- A partial reached only through a dynamic include has no static incoming edge and is inferred as a root unless it is listed in `partialTemplates`.
- A dynamic or missing extends parent suppresses scope-dependent reads owned by child blocks because the parent declarations are unknown. Model reads still run.
- Contract parent read errors point at the parent template and line. Only load and parse failures at the extends edge include the extends path.
- An overridden parent block is skipped for read and include/component evaluation, but its structural parse and target checks still run.
- Diagnostics trace includes and extends. Parent-body errors need `TemplateTrace.run` around the whole evaluation. The caret points at the first non-whitespace character because Pebble does not expose a column. See [DIAGNOSTICS.md](DIAGNOSTICS.md).
- Boot render errors produce a 500 error dispatch. Diagnostic attributes are off by default and an application-owned `ErrorAttributes` wins.
- Dev mode reads and hashes each template on lookup and leaves old cache keys behind. It is for development, not long-running production processes. Classpath resources inside a jar cannot change.
- The total loop budget is keyed by the render scope chain. Parallel branches deep-copy that scope chain and get a separate budget.
- `with only` hides model and extension globals. Internal slot state, locale, and template are preserved. `_context` becomes empty in the isolated scope.
- `for key, value` uses the map key for entries and a zero-based index for other iterables.
- Context escaping is explicit. It does not infer a sink; Natural HTML does that for print expressions. `SafeString` bypasses every strategy. `url` validates schemes, not hosts, paths, or open redirects.
- Natural HTML supports expressions only. It rejects all `{% ... %}` tags, Pebble comments, includes, extends, components, macros, script/style interpolation, and foreign-content subtrees. It is per-file and has no cross-template context propagation.
- Natural HTML analysis-time errors implement `SourceMappedError`, so `TemplateDiagnostics` can build an original-position frame. Pass the delegate/base loader, not `NaturalHtmlLoader`, or the source frame will show rewritten text. Columns count UTF-16 code units.
- The browser preview test parses the source and rendered output with jsoup. It is not real-browser automation.
- Component roots use a namespace map. A named-only configuration has no default root; unqualified names fail. Duplicate normalized root paths fail at build time. The stock Gradle check task exposes one `componentRoot`; the core API and Boot starter support named roots.
- Benchmark numbers are local to one machine and JDK. They are a local review gate, not a CI timing gate.
- The fuzz suite found no product failure in the committed trials. Its corpus is a minimized regression set, not a set of reproduced product bugs.
- Javadoc deployment needs GitHub Pages enabled. The workflow has not run from the local checkout.
- The CLI is a development tool with no sandbox. Its top-level template name is jailed to the template root, but `FileLoader` includes and extends are not jailed. It also does not cap model size, nesting, or rendered output.
- Spotless checks whitespace and final newlines only. A full Java reformat was deliberately not applied.

## Open questions

- Should a component allow unknown props? Current answer is yes. A warning mode is a later addition.
- Should contract types grow expression-level checks, unions, optional values, and required-property enforcement?
- How should upward ancestor block visibility be ordered?
- The stock Gradle task now exposes named component roots.
- Should Natural HTML grow control flow and cross-template composition, and how should it declare a partial's sink context?
- Should `url` percent-encode and reject protocol-relative URLs, or leave open-redirect policy to the application?
- Should contract components see caller locals, matching runtime `includeTemplate`, or stay closed over props?
- Should the output limit and global variables become per-template? Current answer is no. The engine-wide values are conservative.
- Should method access use an explicit render-scoped value instead of a thread-local template name?
- When should the first Gradle Plugin Portal release happen?
- Should the sandbox replace the built-in `include` tag with a depth-guarded version? The output limit covers the easy bomb, not recursive CPU.
- Should component depth use per-engine state instead of a static thread-local?
- Should parent read diagnostics include the full extends chain? Current messages point at the parent line.
