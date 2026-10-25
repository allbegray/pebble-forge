# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Template-defined components with props, defaults, named slots, a default slot, and a depth limit.
- Capability sandbox with tag, filter, function, and test allowlists, a method access validator, a path-jailed loader, an output limit, a per-loop and per-render iteration limit, an include depth limit, and a per-component slot capture budget.
- `switch`, `break`, `continue`, and `with` template tags.
- Multi-value switch cases, `for key, value` destructuring, and `with ... only` scope isolation.
- Break and continue aware `for` loop that preserves Pebble loop variables and `else`.
- Spring Boot 4 starter that replaces the official engine bean and reuses Pebble properties, extensions, and view resolvers.
- Spring Security functions for authentication, anonymous and remember-me state, any-role and any-authority checks, username, and CSRF.
- Build-time contract checks for model variables and component props, with source-order flow, static include and extends scope propagation, block ownership, `checkAll`, root/partial overrides, and a `checkPebbleTemplates` Gradle task.
- Source-frame diagnostics with include and extends traces for template errors.
- Spring Boot render-error diagnostics through a view decorator, optional error attributes, and a real 500 error dispatch.
- A `renderPebbleTemplate` Gradle task for rendering one template with a model and printing source-frame diagnostics.
- Symlink-safe file loader that resolves real paths and rejects escapes.
- Per-template resource limits for loops, include depth, and slot capture through `SandboxLimitsResolver`.
- Per-template sandbox allowlists and method access through `SandboxPolicyResolver`, with policy ownership for includes, macros, embeds, and components.
- Development mode that reloads edited templates through a content-hash cache key.
- Runnable demo with components, sandbox rejection, and language extensions.

### Fixed

- Slot content cannot be forged through a `with` block. The slot function only emits captured `SafeString` values.
- Missing component and root template checks report diagnostics instead of throwing a `NullPointerException`.

### Known limits

- `switch` has no fall-through.
- `break` and `continue` are validated lexically.
- The sandbox does not provide a CPU quota. The default `SandboxedLoader` is lexical; use `SandboxedFileLoader` when symlinks are possible.
- Per-template policies share one engine. The parser is the union of all policies. The output limit, autoescaping, and global variables remain engine-wide.
- Contract checks use source-order flow for direct reads and lexical scope snapshots for includes and extends parents. Cross-template summaries remain presence-based. Dynamic includes and exact block ordering are not followed.
- Diagnostics for errors inside an extended parent need `TemplateTrace.run` around the whole template evaluation. The Boot starter applies this wrapper.
- Spring Security support covers the servlet stack only.

## [0.1.0] - Unreleased

First public release. The feature set above is the release candidate.
