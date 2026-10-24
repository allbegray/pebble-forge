# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Template-defined components with props, defaults, named slots, a default slot, and a depth limit.
- Capability sandbox with tag, filter, function, and test allowlists, a method access validator, a path-jailed loader, and an output limit.
- `switch`, `break`, `continue`, and `with` template tags.
- Break and continue aware `for` loop that preserves Pebble loop variables and `else`.
- Spring Boot 4 starter that replaces the official engine bean and reuses Pebble properties, extensions, and view resolvers.
- Spring Security functions for authentication, roles, authorities, username, and CSRF.
- Build-time contract checks for model variables and component props, with a `checkPebbleTemplates` Gradle task.
- Source-frame diagnostics with include traces for template errors.
- Runnable demo with components, sandbox rejection, and language extensions.

### Fixed

- Slot content cannot be forged through a `with` block. The slot function only emits captured `SafeString` values.

### Known limits

- `switch` has no fall-through.
- `break` and `continue` are validated lexically.
- The sandbox does not provide a CPU quota or resolve symlinks.
- Spring Security support covers the servlet stack only.

## [0.1.0] - Unreleased

First public release. The feature set above is the release candidate.
