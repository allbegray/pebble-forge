# Contributing

Thanks for taking a look at Pebble Forge. This project is an extension library for Pebble 4, not a fork.

## Build and test

```shell
./gradlew build
```

Run one module:

```shell
./gradlew :pebble-forge-core:test
./gradlew :pebble-forge-natural-html:test
./gradlew :pebble-forge-cli:test
./gradlew :pebble-forge-spring-boot-4-starter:test
./gradlew :pebble-forge-fuzz:test
```

Run the demo:

```shell
./gradlew :pebble-forge-demo:run
./gradlew :pebble-forge-demo-app:run
./gradlew :pebble-forge-cli:run --args="--help"
```

The build targets Java 21.

## Modules

| Module | Contents |
| --- | --- |
| `pebble-forge-core` | Components, sandbox, language extensions, context escaping, and the `PebbleForge` builder |
| `pebble-forge-natural-html` | Browser-openable Natural HTML loader and source maps |
| `pebble-forge-contracts` | Model variable and component prop contract checks |
| `pebble-forge-diagnostics` | Source frames and include traces for template errors |
| `pebble-forge-gradle-plugin` | `checkPebbleTemplates` and `renderPebbleTemplate` Gradle tasks |
| `pebble-forge-cli` | Standalone render diagnostics CLI |
| `pebble-forge-spring` | Spring Security functions and sandbox allowlist helpers |
| `pebble-forge-spring-boot-4-starter` | Spring Boot 4 auto-configuration and properties |
| `pebble-forge-demo` | Command-line examples |
| `pebble-forge-demo-app` | Spring Boot 4 browser demo |
| `pebble-forge-benchmarks` | Manual JMH benchmarks |
| `pebble-forge-fuzz` | jqwik property and fuzz tests |

## Tests

Add tests at the layer where the behavior lives. Parser and rendering behavior belongs in `pebble-forge-core` with real templates. Boot wiring belongs in the starter context tests. MockMvc is for view-resolution behavior.

A change to the sandbox, slot handling, or escaping needs a negative test that proves the unsafe path is blocked.

## Code style

- Java 21.
- Four spaces, no tabs.
- Prefer small classes and explicit names.
- Keep public API changes intentional. Add Javadoc for new public types.
- Do not add a dependency when the JDK or Pebble already provides the behavior.

## Pull requests

- Keep a pull request focused on one change.
- Include the test that proves the change.
- Update `CHANGELOG.md` when the change is user visible.
- Update `README.md`, `README.ko.md`, and `docs/DESIGN.md` when behavior or limits change.
- Do not commit build outputs, IDE files, or credentials.

## Commits

Use a short imperative subject. Conventional prefixes such as `feat:`, `fix:`, `docs:`, `test:`, and `build:` are welcome.

## License

By contributing, you agree that your contribution is licensed under the Apache License 2.0. See [LICENSE](LICENSE).

## Security

Do not open a public issue for a sandbox escape or an XSS bypass. Follow [SECURITY.md](SECURITY.md).
