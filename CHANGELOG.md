# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Fixed

- Parent reads and includes now see a child block declaration only after the corresponding parent block starts. Child root declarations remain visible before every parent read.

## [0.3.0] - 2026-10-25

### Added

- Template-defined components with props, defaults, named slots, a default slot, and a depth limit.
- Capability sandbox with tag, filter, function, and test allowlists, a method access validator, a path-jailed loader, an output limit, a per-loop and per-render iteration limit, an include depth limit, and a per-component slot capture budget.
- `switch`, `break`, `continue`, and `with` template tags.
- Multi-value switch cases, `for key, value` destructuring, and `with ... only` scope isolation.
- Break and continue aware `for` loop that preserves Pebble loop variables and `else`.
- Spring Boot 4 starter that replaces the official engine bean and reuses Pebble properties, extensions, and view resolvers.
- Spring Security functions for authentication, anonymous and remember-me state, any-role and any-authority checks, username, and CSRF.
- Build-time contract checks for model variables and component props, with source-order flow, definite cross-template extends summaries, local block ordering, static include scope propagation, block ownership, `checkAll`, root/partial overrides, typed JSON/YAML schemas, and a `checkPebbleTemplates` Gradle task.
- Source-frame diagnostics with include and extends traces, structured `INCLUDE`, `EXTENDS`, and `ERROR` frame kinds, and `SourceMappedError` frames for Natural HTML analysis errors.
- Spring Boot render-error diagnostics through a view decorator, optional error attributes, and a real 500 error dispatch.
- A `renderPebbleTemplate` Gradle task for rendering one template with a model and printing source-frame diagnostics.
- A standalone `pebble-forge-cli` for rendering one template with inline JSON or JSON/YAML model files and the same source-frame diagnostics.
- Jackson 3 for JSON/YAML contract schemas and CLI models.
- Symlink-safe file loader that resolves real paths and rejects escapes.
- Per-template resource limits for loops, include depth, and slot capture through `SandboxLimitsResolver`.
- Per-template sandbox allowlists and method access through `SandboxPolicyResolver`, with policy ownership for includes, macros, embeds, and components.
- Development mode that reloads edited templates through a content-hash cache key.
- Runnable demo with components, sandbox rejection, and language extensions.
- Context-aware escaping strategies for HTML attributes and whole URLs, plus pinned tests for HTML text, JavaScript strings, and CSS.
- Natural HTML loader for browser-openable `.html` templates with context-driven expression rewriting, jsoup validation, source maps, and line-preserving diagnostics.
- Multiple component roots with namespaces, conflict detection, classpath or jar component resolution, and `componentRoots` in the Gradle task.
- Typed contract schemas with object properties, array item shapes, and static attribute-path checks.
- Gradle Plugin Portal metadata and `publishPlugins` configuration for both plugin IDs.
- JMH benchmark module with a recorded local baseline and a regression rule.
- jqwik fuzz and property tests for the parser, path jail, and sandbox allowlists, with a failure minimizer and checked-in corpus.
- Spring Boot 4 demo app with MVC, Security, CSRF login, components, extends, and language extensions.
- Aggregate Javadoc site, GitHub Pages workflow, Spotless checks, and Checkstyle static analysis.

### Fixed

- Slot content cannot be forged through a `with` block. The slot function only emits captured `SafeString` values.
- Missing component and root template checks report diagnostics instead of throwing a `NullPointerException`.

### Known limits

- `switch` has no fall-through.
- `break` and `continue` are validated lexically.
- The sandbox does not provide a CPU quota. The default `SandboxedLoader` is lexical; use `SandboxedFileLoader` when symlinks are possible.
- Per-template policies share one engine. The parser is the union of all policies. The output limit, autoescaping, and global variables remain engine-wide.
- Contract checks use source-order flow for direct reads and definite end-of-flow summaries for extends parents. Includes use lexical scope snapshots. Root-read block ordering is local; cross-template block execution order is not modeled. Typed JSON/YAML schemas check attribute shapes, not expression types; `required` is parsed but not enforced. The Gradle plugins are configured for the Plugin Portal but have not been uploaded.
- Diagnostics for errors inside an extended parent need `TemplateTrace.run` around the whole template evaluation. The Boot starter applies this wrapper.
- Spring Security and web integration cover WebMVC on the servlet stack only. WebFlux is not supported.
- The CLI is a trusted-template development tool. It has no sandbox; top-level template names are jailed, but includes and extends are not.
- Context escaping is explicit. It does not infer the HTML context; Natural HTML does that for expressions.
- `SafeString` from `slot()` or `raw` bypasses context escaping. The URL strategy validates schemes, not hosts or open redirects.
- Natural HTML supports expressions only. Control flow, includes, extends, components, macros, JavaScript, and CSS interpolation are rejected in the first slice.
- Benchmark numbers are local to one machine and JDK. They are a local regression gate, not a CI timing gate.
- The fuzz suite found no product failure in the committed trials. The checked-in corpus is a minimized regression set, not a set of reproduced product bugs.
- Javadoc deployment needs GitHub Pages enabled. The workflow is present but has not run from this local checkout.
- Spotless checks whitespace and final newlines only. A full Java reformat was deliberately not applied.
