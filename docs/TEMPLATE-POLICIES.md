# Template policies

Pebble Forge resolves sandbox policy and resource limits by template name.

| Layer | Scope |
| --- | --- |
| Allowlists, method access, path validation | Per template through `SandboxPolicyResolver` |
| Loop, include, slot limits | Per template through `SandboxLimitsResolver` |

## Policy resolver

```java
SandboxPolicy publicPolicy = SandboxPolicy.strict();
SandboxPolicy adminPolicy = SandboxPolicy.builder()
        .allowRawOutput()
        .allowTags("macro")
        .build();

SandboxPolicyResolver policies = SandboxPolicyResolver
        .builder(publicPolicy)
        .prefix("admin/", adminPolicy)
        .exact("public/special.peb", publicPolicy)
        .build();

PebbleEngine engine = PebbleForge.engine()
        .loader(loader)
        .components(Components.defaults())
        .policies(policies)
        .build();
```

Rules:

- Exact rules win over prefix rules.
- The longest matching prefix wins.
- A prefix gets a trailing `/`.
- Unmatched templates use the default policy.
- Literal templates created with `getLiteralTemplate` use the default policy. Their source text never selects a policy.
- Use either `sandbox(policy)` or `policies(resolver)`, not both. A method validator belongs on each `SandboxPolicy` when you use a resolver.

Spring Boot accepts a `SandboxPolicyResolver` bean. The bean replaces the policy built from properties. Do not define a `SandboxPolicy` bean and a resolver bean together. A resolver policy must add any framework functions and tags it needs, such as `SpringWebFunctions.allow(builder)`, `extends`, `block`, and the component tags.

```java
@Bean
SandboxPolicyResolver policies() {
    return SandboxPolicyResolver
            .builder(SandboxPolicy.strict())
            .prefix("admin/", SandboxPolicy.builder().allowRawOutput().build())
            .build();
}
```

## How enforcement works

The engine parses with the union of all allowed tags, filters, functions, and tests. Each parser and render call checks the policy for the template that owns the node.

| Construct | Check point |
| --- | --- |
| Tags | Parse time. Disallowed tags fail with the template name and line. |
| Filters | Render time through a policy-aware filter wrapper. |
| Functions | Render time through a policy-aware function wrapper. |
| Tests | Render time through a policy-aware test wrapper. |
| Methods | The attribute resolver routes each policy to its own Pebble resolver, so the member cache cannot carry access across policies. |
| Paths | The loader validates a name with the policy for that name. |

`escape` is required by autoescaping and cannot be denied. `default` and `defined` keep their concrete Pebble classes when a policy denies them, so strict-variable behavior still works. User extensions cannot override `escape`, `default`, or `defined`.

## Include and extends

An included template uses its own policy. The `include` tag itself is checked against the caller's policy.

A macro body uses the policy of the template that defines it. An embed override uses the policy of the template that declares the block. A component template uses its own policy. Includes, macros, embeds, and components restore the previous policy when they return.

An extending child and its parent use their own policies. A child block is checked as the child, and a parent block is checked as the parent. Do not assume that extending a trusted parent grants the child the parent's allowlist.

## Engine-wide settings

- The output limit is the strictest finite limit across all policies.
- Autoescaping is on if any policy forces it.
- Global variables are exposed only when every policy allows them.
- Literal templates use the default policy. They do not inherit a policy from their source text.

Resource limits can be supplied separately with `SandboxLimitsResolver`. If none is supplied, each policy's own limits are used.

## Tradeoffs

The union parser means a tag or filter is compiled even when only one policy allows it. Render-time wrappers then reject it for the templates that deny it. This keeps one engine and one template cache for all trust levels.

The alternative is one engine per policy. That is simpler to reason about but cannot share includes or inheritance across policy boundaries.

## Related

- [Sandbox](../README.md#sandbox)
- [Design](DESIGN.md)
- [Backlog](BACKLOG.md)
