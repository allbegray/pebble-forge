package io.github.allbegray.pebbleforge.include;

import io.github.allbegray.pebbleforge.trace.TemplateTrace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncludeTraceTest {

    @BeforeEach
    void clearTraceBefore() {
        TemplateTrace.popTo(0);
    }

    @AfterEach
    void clearTraceAfter() {
        TemplateTrace.popTo(0);
    }

    @Test
    void keepsTheIncludeOnlyViewOfTheUnifiedTrace() {
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, "page.peb",
                1, "layout.peb"));
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.INCLUDE, "layout.peb",
                4, "card.peb"));
        RuntimeException error = new RuntimeException("boom");

        TemplateTrace.attach(error);

        assertThat(IncludeTrace.frames(error))
                .containsExactly(new IncludeTrace.IncludeFrame("layout.peb", 4, "card.peb"));
    }

    @Test
    void depthCountsOnlyIncludes() {
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, "page.peb",
                1, "layout.peb"));
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.INCLUDE, "layout.peb",
                2, "card.peb"));
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, "card.peb",
                1, "card-base.peb"));

        assertThat(IncludeTrace.depth()).isEqualTo(1);
    }
}
