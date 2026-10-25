package io.github.allbegray.pebbleforge.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.groups.Tuple.tuple;

class TemplateTraceTest {

    @BeforeEach
    void clearTraceBefore() {
        TemplateTrace.popTo(0);
    }

    @AfterEach
    void clearTraceAfter() {
        TemplateTrace.popTo(0);
    }

    @Test
    void collectsIncludeAndExtendsFramesInChronologicalOrder() {
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, "page.peb",
                1, "layout.peb"));
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.INCLUDE, "layout.peb",
                4, "card.peb"));
        RuntimeException error = new RuntimeException("boom");

        TemplateTrace.attach(error);

        assertThat(TemplateTrace.frames(error))
                .extracting(TemplateTrace.Frame::kind, TemplateTrace.Frame::template,
                        TemplateTrace.Frame::line, TemplateTrace.Frame::target)
                .containsExactly(
                        tuple(TemplateTrace.Kind.EXTENDS,
                                "page.peb", 1, "layout.peb"),
                        tuple(TemplateTrace.Kind.INCLUDE,
                                "layout.peb", 4, "card.peb"));
    }

    @Test
    void attachesOneMarkerOnly() {
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.INCLUDE, "page.peb",
                2, "card.peb"));
        RuntimeException error = new RuntimeException("boom");

        TemplateTrace.attach(error);
        TemplateTrace.attach(error);

        assertThat(error.getSuppressed()).hasSize(1);
        assertThat(TemplateTrace.frames(error)).hasSize(1);
    }

    @Test
    void runClearsFramesOnSuccess() throws Exception {
        TemplateTrace.run(() -> TemplateTrace.push(new TemplateTrace.Frame(
                TemplateTrace.Kind.EXTENDS, "page.peb", 1, "layout.peb")));

        assertThat(TemplateTrace.currentFrames()).isEmpty();

        RuntimeException later = new RuntimeException("later");
        TemplateTrace.attach(later);
        assertThat(TemplateTrace.frames(later)).isEmpty();
    }

    @Test
    void runStartsFromACleanTrace() throws Exception {
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, "old.peb",
                1, "old-layout.peb"));

        TemplateTrace.run(() -> {
        });

        assertThat(TemplateTrace.currentFrames()).isEmpty();
    }

    @Test
    void runAttachesFramesOnFailure() {
        IllegalStateException error = catchThrowableOfType(IllegalStateException.class,
                () -> TemplateTrace.run(() -> {
                    TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS,
                            "page.peb", 3, "layout.peb"));
                    throw new IllegalStateException("boom");
                }));

        assertThat(TemplateTrace.frames(error))
                .extracting(TemplateTrace.Frame::kind, TemplateTrace.Frame::template,
                        TemplateTrace.Frame::target)
                .containsExactly(tuple(
                        TemplateTrace.Kind.EXTENDS, "page.peb", "layout.peb"));
        assertThat(TemplateTrace.currentFrames()).isEmpty();
    }

    @Test
    void includeDepthCountsOnlyIncludes() {
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, "page.peb",
                1, "layout.peb"));
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.INCLUDE, "layout.peb",
                2, "card.peb"));
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, "card.peb",
                1, "card-base.peb"));

        assertThat(TemplateTrace.includeDepth()).isEqualTo(1);
    }

    @Test
    void popToTruncatesNestedFramesAndClampsMarks() {
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.EXTENDS, "page.peb",
                1, "layout.peb"));
        int mark = TemplateTrace.mark();
        TemplateTrace.push(new TemplateTrace.Frame(TemplateTrace.Kind.INCLUDE, "layout.peb",
                2, "card.peb"));

        TemplateTrace.popTo(mark);
        TemplateTrace.popTo(100);

        assertThat(TemplateTrace.currentFrames())
                .extracting(TemplateTrace.Frame::template)
                .containsExactly("page.peb");
    }
}
