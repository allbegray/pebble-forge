# pebble-forge-spring-boot-4-starter

Spring Boot 4 integration for Pebble Forge.

This module targets Spring Boot 4.0.6, Spring Framework 7.0.7, Spring Security 7.0.8, and Pebble 4.1.2.

The `boot-4` in the directory and artifact name is intentional. Pebble Forge does not publish one starter across Boot major versions. A Spring Boot 3 integration would be a separate module.

## Dependency

```kotlin
implementation("io.github.allbegray.pebbleforge:pebble-forge-spring-boot-4-starter:0.4.0")
```

## What it does

- Runs before Pebble's `PebbleAutoConfiguration`.
- Replaces the `pebbleEngine` bean with one built by `PebbleForge`.
- Reuses `PebbleProperties`, the `pebbleLoader` bean, `SpringExtension`, every `Extension` bean, and the official view resolvers.
- Binds `pebble.forge.*` properties for components, sandbox, Spring Security, language extensions, diagnostics, and dev mode.
- Registers `SpringSecurityExtension` when the servlet and Spring Security classes are present.
- Normalizes `pebble.prefix` when the effective loader is a `ClasspathLoader`, because sandboxing wraps the loader and the official view resolver no longer sees the raw type.
- Wraps the official Pebble view so a render failure becomes a real 500 error dispatch. An application error page can read `pebbleDiagnostics` and `pebbleDiagnosticsText` when exposure is enabled.

## Properties

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

A `SandboxPolicy` bean replaces the policy built from properties. A `SandboxLimitsResolver` bean supplies per-template resource limits.

## Verification

The module has tests for the engine context, the official view resolver, render-error 500 dispatch, diagnostic error attributes, error pages, a full-container request, disabled diagnostics, and direct task registration. The full container test renders a broken template and checks the error page.

See the [root README](../README.md) and [design document](../docs/DESIGN.md).
