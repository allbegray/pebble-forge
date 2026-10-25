package io.github.allbegray.pebbleforge.benchmarks;

import io.github.allbegray.pebbleforge.PebbleForge;
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
 * Pebble Forge rendering of the plain template without components or the sandbox.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ForgePlainBenchmark {

    /**
     * Creates the benchmark state. JMH instantiates it.
     */
    public ForgePlainBenchmark() {
    }

    private PebbleTemplate template;

    /**
     * Builds the engine and loads the benchmark template.
     *
     * @throws IOException if the template cannot be read
     */
    @Setup(Level.Trial)
    public void setUp() throws IOException {
        PebbleEngine engine = PebbleForge.engine()
                .loader(BenchTemplates.loader())
                .build();
        template = engine.getTemplate("plain");
    }

    /**
     * Renders the benchmark template once.
     *
     * @return the rendered output
     * @throws IOException if rendering fails
     */
    @Benchmark
    public String render() throws IOException {
        return BenchTemplates.render(template);
    }
}
