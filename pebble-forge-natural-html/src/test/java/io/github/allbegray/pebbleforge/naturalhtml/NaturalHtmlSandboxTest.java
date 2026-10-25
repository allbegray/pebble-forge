package io.github.allbegray.pebbleforge.naturalhtml;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.sandbox.SandboxPolicy;
import io.pebbletemplates.pebble.PebbleEngine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalHtmlSandboxTest {

    private static final String TEMPLATE = "page.html";

    @Test
    void strictSandboxRendersTextAttributeAndUrlExpressions() throws IOException {
        String source = "<p title=\"{{ title }}\">"
                + "<a href=\"/users/{{ id }}\">{{ text }}</a></p>";
        PebbleEngine engine = PebbleForge.engine()
                .loader(NaturalHtml.loader(new MapLoader(Map.of(TEMPLATE, source))))
                .sandbox(SandboxPolicy.strict())
                .build();
        StringWriter writer = new StringWriter();

        engine.getTemplate(TEMPLATE).evaluate(writer, Map.of(
                "title", "\" onmouseover=\"alert(1)",
                "id", "42",
                "text", "<script>alert(1)</script>"));

        assertThat(writer.toString()).isEqualTo(
                "<p title=\"&quot;&#32;onmouseover&#61;&quot;alert(1)\">"
                        + "<a href=\"/users/42\">&lt;script&gt;alert(1)&lt;/script&gt;</a></p>");
    }

    @Test
    void strictSandboxSupportsTheDoubleQuotedFilterArgument() throws IOException {
        String source = "<p title='{{ title }}'>x</p>";
        PebbleEngine engine = PebbleForge.engine()
                .loader(NaturalHtml.loader(new MapLoader(Map.of(TEMPLATE, source))))
                .sandbox(SandboxPolicy.strict())
                .build();
        StringWriter writer = new StringWriter();

        engine.getTemplate(TEMPLATE).evaluate(writer, Map.of("title", "' onfocus='alert(1)"));

        assertThat(writer.toString())
                .isEqualTo("<p title='&#39;&#32;onfocus&#61;&#39;alert(1)'>x</p>");
    }
}
