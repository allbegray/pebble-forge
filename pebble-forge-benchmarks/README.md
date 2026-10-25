# Pebble Forge benchmarks

JMH throughput benchmarks for raw Pebble, Pebble Forge plain rendering, components, the sandbox, and the language extensions.

The module is manual. `build` compiles the benchmarks and runs the template smoke tests. The `benchmark` task is not part of `check` or CI because JVM timing is machine-specific.

## Run

```shell
./gradlew :pebble-forge-benchmarks:build
./gradlew :pebble-forge-benchmarks:benchmark
```

The default run uses `-f 1 -wi 2 -i 3 -w 1s -r 1s`: one fork, two one-second warmups, and three one-second measurements per benchmark. It takes about 30 seconds on an Apple M1 Max.

Override the JMH arguments with `jmhArgs`:

```shell
./gradlew :pebble-forge-benchmarks:benchmark \
  -PjmhArgs="-f 1 -wi 5 -i 5 -w 2s -r 2s"
```

Run one benchmark by passing its class name as the JMH regex:

```shell
./gradlew :pebble-forge-benchmarks:benchmark \
  -PjmhArgs="ComponentBenchmark -f 1 -wi 2 -i 3 -w 1s -r 1s"
```

For a longer or quieter run, keep the same machine, JDK, and Gradle version, and close other CPU-heavy work.

## Benchmarks

All benchmarks use `ClasspathLoader` with prefix `bench/` and suffix `.peb`. Each benchmark state builds one engine, resolves the template once, and renders into a new `StringWriter` per invocation. The rendered string is returned so JMH consumes it.

| Benchmark | What it measures |
| --- | --- |
| `RawPebbleBenchmark.render` | Plain Pebble 4.1.2 with the `plain` template: one print and a four-item `for` loop. |
| `ForgePlainBenchmark.render` | `PebbleForge.engine()` with no components or sandbox, using the same `plain` template. |
| `ComponentBenchmark.render` | One component call with evaluated props and a named slot (`component-page` plus `components/card`). |
| `SandboxBenchmark.render` | A sandboxed page with a component, `extends`/`block`, `with`/`switch`, and forced autoescaping. |
| `LanguageBenchmark.render` | `with`, `switch`, `for`, `break`, and `continue` in one template. |

## Read the results

`RESULTS.md` records the current local baseline and the regression rule. Scores are throughput in operations per second: higher is better. `Error` is the 99.9% confidence interval printed by JMH. A short run has noisy error bars, so use them to judge stability, not to distinguish small differences.

Do not compare absolute numbers across machines, JDKs, or background load. The rule is a local regression gate, not a CI timing gate.
