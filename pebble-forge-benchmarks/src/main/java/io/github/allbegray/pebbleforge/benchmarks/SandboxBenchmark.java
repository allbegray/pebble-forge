package io.github.allbegray.pebbleforge.benchmarks;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Sandboxed rendering with components, extends/block, language tags, and autoescaping.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class SandboxBenchmark {

    private PebbleTemplate template;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        SandboxPolicy policy = SandboxPolicy.builder()
                .allowComponents()
                .allowTags("extends", "block")
                .build();
        PebbleEngine engine = PebbleForge.engine()
                .loader(BenchTemplates.loader())
                .components(Components.builder().suffix("").build())
                .sandbox(policy)
                .build();
        template = engine.getTemplate("sandbox-page");
    }

    @Benchmark
    public String render() throws IOException {
        return BenchTemplates.render(template);
    }
}
