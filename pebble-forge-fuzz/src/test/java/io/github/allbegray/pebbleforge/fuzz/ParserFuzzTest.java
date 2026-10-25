package io.github.allbegray.pebbleforge.fuzz;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.fuzz.support.FuzzText;
import io.github.allbegray.pebbleforge.fuzz.support.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.fail;

class ParserFuzzTest {

    private static final int MAX_TEMPLATE_LENGTH = 160;
    private static final String TEMPLATE_NAME = "fuzz.peb";
    private static final Map<String, Object> CONTEXT = Map.of(
            "name", "forge",
            "items", List.of(1, 2, 3),
            "ok", true,
            "value", "x");

    private static final String ALPHABET = "{}%#\"'<>/\\:=&;()[]|,+-.~_!?*^$@ \n\t\r\0éüΩ中🙂";
    private static final List<String> TOKENS = List.of(
            "{{", "}}", "{%", "%}", "{#", "#}",
            "if ", "endif", "else", "for ", "in ", "endfor",
            "include ", "extends ", "block ", "endblock",
            "macro ", "endmacro", "set ", "component ", "slot", "props",
            "break", "continue", "switch", "with ", "endwith",
            "verbatim", "endverbatim", "raw", "escape", "empty",
            "1", "2.5", "true", "false", "null",
            "|", "(", ")", "\"", "'", " ", "\n");

    @Property(tries = 200)
    void parserOnlyReproducesDeclaredFailures(@ForAll("templates") String source) {
        try {
            PebbleEngine engine = PebbleForge.engine()
                    .loader(new MapLoader(Map.of(TEMPLATE_NAME, source)))
                    .build();
            engine.getTemplate(TEMPLATE_NAME).evaluate(new StringWriter(), CONTEXT);
        } catch (PebbleException expected) {
            // Parser and evaluator reject malformed or disallowed templates this way.
        } catch (IOException exception) {
            fail("Unexpected IOException for input [" + FuzzText.escape(source) + "]", exception);
        } catch (RuntimeException | StackOverflowError | OutOfMemoryError exception) {
            fail("Unexpected " + exception.getClass().getName()
                    + " for input [" + FuzzText.escape(source) + "]", exception);
        }
    }

    @Provide
    Arbitrary<String> templates() {
        Arbitrary<String> noise = Arbitraries.strings()
                .withChars(ALPHABET)
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMaxLength(MAX_TEMPLATE_LENGTH);

        Arbitrary<String> structured = Arbitraries.of(TOKENS)
                .list()
                .ofMaxSize(20)
                .map(parts -> String.join("", parts))
                .filter(source -> source.length() <= MAX_TEMPLATE_LENGTH);

        return Arbitraries.oneOf(noise, structured);
    }
}
