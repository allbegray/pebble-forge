# Template policies

Pebble Forge has two layers of sandbox configuration.

| Layer | Scope | Status |
| --- | --- | --- |
| Allowlists and method access | One `SandboxPolicy` per engine | Available |
| Resource limits | Per template name | Available |
| Allowlists per template name | Per template name | Not yet. Tracked as PF-108 |

This document covers the per-template resource limits.

## Limits resolver

```java
SandboxLimits publicLimits = SandboxLimits.builder()
        .maxLoopIterations(1_000)
        .maxTotalIterations(10_000)
        .maxIncludeDepth(8)
        .maxSlotChars(100_000)
        .build();

SandboxLimits adminLimits = SandboxLimits.builder()
        .maxLoopIterations(100_000)
        .maxTotalIterations(1_000_000)
        .maxIncludeDepth(32)
        .maxSlotChars(1_000_000)
        .build();

SandboxLimitsResolver limits = SandboxLimitsResolver
        .builder(SandboxLimits.from(policy))
        .prefix("public/", publicLimits)
        .prefix("admin/", adminLimits)
        .build();

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .components(Components.defaults())
        .sandbox(policy)
        .limitsResolver(limits)
        .build();
```

Spring Boot accepts a `SandboxLimitsResolver` bean.

```java
@Bean
SandboxLimitsResolver limitsResolver() {
    return SandboxLimitsResolver
            .builder(SandboxLimits.from(SandboxPolicy.strict()))
            .prefix("public/", publicLimits)
            .build();
}
```

## Matching

- Exact rules win over prefix rules.
- The longest matching prefix wins.
- A prefix is normalized and gets a trailing `/`.
- If no rule matches, the default limits are used.

## What can vary

| Limit | Field |
| --- | --- |
| Iterations in one loop | `maxLoopIterations` |
| Total iterations in one render | `maxTotalIterations` |
| Nested include depth | `maxIncludeDepth` |
| Slot capture per component | `maxSlotChars` |

`maxRenderedChars` remains an engine setting. Different output limits per template need the full per-template policy work.

## Why allowlists are not per template yet

Pebble builds one parser and one extension registry per engine. Tags, filters, functions, and tests are resolved through that registry. The method access validator has no template context. Full per-template allowlists need one of these designs.

1. Parse with the union of all allowlists, then wrap every tag, filter, function, test, and method access with a policy check. This is the most compatible path but touches many extension points.
2. Build one engine per policy and route at the application boundary. This is simple but includes and extends cannot cross policies.

The current release takes the safe, smaller step. Resource limits vary by template. The allowlist remains per engine.

## Related

- [Sandbox](../README.md#sandbox)
- [Design](DESIGN.md)
- [Backlog](BACKLOG.md)
