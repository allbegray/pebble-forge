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
- Build-time contract checks for model variables and component props, with static include scope propagation, `checkAll`, root/partial overrides, and a `checkPebbleTemplates` Gradle task.
- Source-frame diagnostics with include traces for template errors.
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
- Contract checks follow static includes with a lexical scope snapshot. Direct reads remain presence-based. Extends, dynamic includes, and full flow-sensitive block scopes are not followed.
- Spring Security support covers the servlet stack only.

## [0.1.0] - Unreleased

First public release. The feature set above is the release candidate.
