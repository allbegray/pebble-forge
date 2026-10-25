# Security policy

## Supported versions

| Version | Supported |
| --- | --- |
| 0.1.x | Yes |
| Earlier snapshots | No |

Pebble Forge is pre-1.0. Security fixes land on the latest 0.1.x release.

## Reporting a vulnerability

Do not open a public issue for a vulnerability.

Use the GitHub Security Advisory form.

```text
https://github.com/allbegray/pebble-forge/security/advisories/new
```

If the repository is not public yet, contact the maintainer through the GitHub account `allbegray` before disclosing details.

Include the affected version, a minimal template and model, the expected result, the actual result, and any preconditions. A reproduction that runs against the demo or a small test project is ideal.

## What counts as a security issue

- A sandbox escape that reaches the filesystem, process, classloader, reflection, or an unapproved method.
- A path traversal that reads a template outside the loader root.
- An XSS bypass in slot handling, component props, autoescaping, or a Spring Security helper.
- A Spring Security function that leaks an `Authentication` object or exposes more state than the documented functions.
- A template injection that executes Java code or calls an unapproved method.
- A dependency vulnerability that affects the published artifacts.

## What does not count as a security issue

- CPU or memory exhaustion from a template that is allowed to run in-process. The sandbox is a capability guard, not a CPU quota. Run hostile templates in a separate process.
- A sandbox policy that deliberately allows `raw`, dangerous methods, or unsafe tags.
- A model object that intentionally exposes secrets through a public getter. The application decides what enters the model.
- A symlink inside a file loader root that points outside the root. The path jail is lexical. Use a loader root with no symlinks or a custom loader with canonical path checks.
- A vulnerability in Pebble itself. Report it to the Pebble project and open a Pebble Forge issue only if the integration needs a workaround.

## Threat model

Pebble Forge separates two trust levels.

| Mode | Template source | Boundary |
| --- | --- | --- |
| Authoring | Application developers | Full Pebble, components, language extensions |
| Untrusted | End users, tenants, workflow authors | Allowlisted tags, filters, functions, tests, methods, paths, and output size |

The sandbox policy is fixed per engine. If one engine serves templates from different trust levels, all of them share the same policy. Build separate engines for separate trust levels.

The sandbox protects capabilities. It does not protect CPU time, memory, or wall-clock time. The path jail is lexical and does not resolve symlinks. `break` and `continue` are validated lexically, so a macro can move the signal across a call boundary.

Context escaping is a data-sink boundary, not an HTML sanitizer. `html_attr` and `url` protect ordinary values in known sinks. `SafeString` from `slot()`, `raw`, or a custom function bypasses every strategy. The URL strategy validates schemes, not hosts, paths, or open redirects. Natural HTML rejects known safe producers and unsafe contexts, but a custom function that returns `SafeString` remains trusted application code.

See [docs/DESIGN.md](docs/DESIGN.md) for the full list of accepted limits.

## Safe deployment guidance

- Use a strict sandbox policy for any template the application does not own.
- Keep `raw` disabled unless the template author is trusted.
- Keep strict variables enabled.
- Serve each trust level from its own engine.
- Run hostile templates in a separate process with OS-level CPU and memory limits.
- Keep Pebble and Spring Boot dependencies current.
- Do not expose `Authentication`, `Class`, `ClassLoader`, `Runtime`, or filesystem objects to the model.

## Disclosure process

1. The report is acknowledged within three business days.
2. The maintainer reproduces the issue and decides whether it is in scope.
3. A fix and regression test are prepared in a private advisory.
4. A patched release is published.
5. The advisory is published after users have had time to upgrade.
