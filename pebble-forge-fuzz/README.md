# pebble-forge-fuzz

Property-based and fuzz tests for Pebble Forge. This module has tests only and is not published.

It covers three boundaries:

- Pebble parsing and evaluation: success or `PebbleException`; no unexpected runtime or stack failures.
- Path resolution: `SandboxedLoader` and `SandboxedFileLoader` never read outside the loader root.
- Sandbox allowlists: tag, filter, function, and test allowlists match exact names.

The suite uses jqwik with 600 property trials per run. `FailureMinimizer` greedily removes chunks and writes a reproducer under `build/fuzz-failures/`. The checked-in corpus under `src/test/resources/fuzz-corpus/` holds minimized regression inputs.

Run:

```shell
./gradlew :pebble-forge-fuzz:test
```
