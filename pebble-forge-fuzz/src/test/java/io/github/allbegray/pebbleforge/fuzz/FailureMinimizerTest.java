package io.github.allbegray.pebbleforge.fuzz;

import io.github.allbegray.pebbleforge.fuzz.support.FailureMinimizer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class FailureMinimizerTest {

    private static final Predicate<String> CONTAINS_ANGLE_AND_TWO_CHARS =
            input -> input.contains("<") && input.length() >= 2;

    @Test
    void shrinksAChunkedFailureToTwoCharacters() {
        FailureMinimizer minimizer = new FailureMinimizer(CONTAINS_ANGLE_AND_TWO_CHARS);

        assertThat(minimizer.minimize("prefix<middle<suffix")).hasValue("e<");
    }

    @Test
    void writesTheMinimizedReproducer() throws IOException {
        FailureMinimizer minimizer = new FailureMinimizer(CONTAINS_ANGLE_AND_TWO_CHARS);
        String minimized = minimizer.minimize("aaa<bbb").orElseThrow();

        Path file = FailureMinimizer.writeReproducer(
                Path.of("build", "fuzz-failures"), "contains-angle", minimized);

        assertThat(minimized).isEqualTo("<b");
        assertThat(file).hasFileName("contains-angle.txt").hasContent("<b");
    }

    @Test
    void overwritesAnExistingReproducer() throws IOException {
        Path directory = Path.of("build", "fuzz-failures");
        Path file = FailureMinimizer.writeReproducer(directory, "overwrite", "first");

        FailureMinimizer.writeReproducer(directory, "overwrite", "second");

        assertThat(file).hasContent("second");
    }

    @Test
    void returnsEmptyWhenThePredicateDoesNotHold() {
        FailureMinimizer minimizer = new FailureMinimizer(input -> false);

        assertThat(minimizer.minimize("nothing to see")).isEmpty();
    }

    @Test
    void leavesAnAlreadyMinimalInputAlone() {
        FailureMinimizer minimizer = new FailureMinimizer(CONTAINS_ANGLE_AND_TWO_CHARS);

        assertThat(minimizer.minimize("<b")).hasValue("<b");
        assertThat(minimizer.minimize("")).isEmpty();
    }

    @Test
    void handlesAnEmptyInputThatStillReproduces() {
        FailureMinimizer minimizer = new FailureMinimizer(input -> true);

        assertThat(minimizer.minimize("")).hasValue("");
    }

    @Test
    void preservesDistinctCharactersAcrossChunks() {
        Predicate<String> containsBothAngles = input -> input.contains("<") && input.contains(">");
        FailureMinimizer minimizer = new FailureMinimizer(containsBothAngles);

        assertThat(minimizer.minimize("a<b>c")).hasValue("<>");
    }
}
