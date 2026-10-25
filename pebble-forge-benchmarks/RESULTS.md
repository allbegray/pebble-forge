# Benchmark results

This file records the local baseline for the JMH suite. The numbers below are from a real run, not estimates.

## Environment

| Item | Value |
| --- | --- |
| Date | 2026-10-25 |
| Machine | Apple M1 Max, 32 GB |
| OS | macOS 27.0.1 (build 26A434), Darwin 27.0.0, arm64 |
| Java | Amazon Corretto 25.0.4 (OpenJDK 64-Bit Server VM, 25.0.4+7-LTS) |
| Gradle | 9.7.1 |
| JMH | 1.37 |
| Threads | 1 per benchmark |
| Fork | 1 |

The build targets Java 21. The benchmark JVM was Java 25.0.4 from the current `JAVA_HOME`.

## Command

```shell
./gradlew :pebble-forge-benchmarks:benchmark
```

Default JMH arguments:

```text
-f 1 -wi 2 -i 3 -w 1s -r 1s
```

JMH mode and time unit come from the annotations: throughput, operations per second, with a 99.9% confidence interval.

## Throughput

| Benchmark | Throughput (ops/s) | Error (99.9%, ± ops/s) | Relative to plain |
| --- | ---: | ---: | ---: |
| `RawPebbleBenchmark.render` | 2,028,104.715 | 764,703.707 | 99.2% |
| `ForgePlainBenchmark.render` | 2,043,970.297 | 231,555.785 | 100.0% |
| `ComponentBenchmark.render` | 1,009,924.710 | 51,076.485 | 49.4% |
| `SandboxBenchmark.render` | 307,403.361 | 35,490.117 | 15.0% |
| `LanguageBenchmark.render` | 817,208.423 | 28,810.546 | 40.0% |

The plain baseline is `ForgePlainBenchmark.render` at 2,043,970.297 ops/s. Raw Pebble and Pebble Forge plain are within noise of each other. The sandbox is the most expensive path, as expected for policy checks, forced autoescaping, inheritance, and slot capture. The short run has wide error bars, especially for the raw row; rerun with longer warmup and measurement before investigating a small change.

The complete JMH summary from the recorded run:

```text
Benchmark                    Mode  Cnt        Score        Error  Units
ComponentBenchmark.render   thrpt    3  1009924.710 ±  51076.485  ops/s
ForgePlainBenchmark.render  thrpt    3  2043970.297 ± 231555.785  ops/s
LanguageBenchmark.render    thrpt    3   817208.423 ±  28810.546  ops/s
RawPebbleBenchmark.render   thrpt    3  2028104.715 ± 764703.707  ops/s
SandboxBenchmark.render     thrpt    3   307403.361 ±  35490.117  ops/s
```

## Regression rule

Record the table above as the local baseline. On a rerun on the same machine, same JDK, and same command, treat the result as a failed review when:

1. Any benchmark's throughput drops more than 25 percent from its recorded value in this table.
2. `ComponentBenchmark`, `SandboxBenchmark`, or `LanguageBenchmark` falls below 10 percent of the plain baseline recorded in the same run.

The current run passes: the lowest ratio is sandbox at 15.0 percent of plain.

This is a local regression gate, not a CI timing gate. Do not fail CI on absolute throughput and do not compare numbers across machines. Use the rule when reviewing a change that touches rendering, components, sandbox checks, or language nodes, and rerun with longer JMH settings when a result is close to a threshold.
