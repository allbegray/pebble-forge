package io.github.allbegray.pebbleforge.inheritance;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.github.allbegray.pebbleforge.trace.TemplateTrace;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.AbstractNodeVisitor;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.node.ExtendsNode;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.groups.Tuple.tuple;

class ForgeExtendsTest {

    @BeforeEach
    void clearTraceBefore() {
        TemplateTrace.popTo(0);
    }

    @AfterEach
    void clearTraceAfter() {
        TemplateTrace.popTo(0);
    }

    @Test
    void parsesExtendsIntoAForgeExtendsNode() {
        List<ExtendsNode> nodes = collect("""
                {% extends "layout.peb" %}
                {% block body %}child{% endblock %}
                """);

        assertThat(nodes).singleElement().isInstanceOf(ForgeExtendsNode.class);
    }

    @Test
    void rendersExtendsThroughTheDefaultExtension() throws Exception {
        String output = render(Map.of(
                "page.peb", """
                        {% extends "layout.peb" %}
                        {% block body %}child{% endblock %}
                        """,
                "layout.peb", "before[{% block body %}base{% endblock %}]after"));

        assertThat(output).isEqualTo("before[child]after");
    }

    @Test
    void evaluatesADynamicParentExpressionOnce() throws Exception {
        CountingParentExtension extension = new CountingParentExtension();
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of(
                        "page.peb", """
                                {% extends pick() %}
                                {% block body %}child{% endblock %}
                                """,
                        "layout.peb", "base[{% block body %}base{% endblock %}]")))
                .extension(extension)
                .build();

        StringWriter writer = new StringWriter();
        engine.getTemplate("page.peb").evaluate(writer);

        assertThat(writer.toString()).isEqualTo("base[child]");
        assertThat(extension.calls).hasValue(1);
    }

    @Test
    void includeBoundaryTruncatesNestedExtendsFramesOnSuccess() {
        Map<String, String> templates = Map.of(
                "page.peb", "{% extends \"layout.peb\" %}",
                "layout.peb", """
                        {% include "partial.peb" %}
                        {{ missing }}
                        """,
                "partial.peb", """
                        {% extends "partial-base.peb" %}
                        {% block body %}partial{% endblock %}
                        """,
                "partial-base.peb", "base[{% block body %}base{% endblock %}]");

        PebbleException error = catchThrowableOfType(PebbleException.class,
                () -> TemplateTrace.run(() -> engine(templates)
                        .getTemplate("page.peb").evaluate(new StringWriter())));

        assertThat(TemplateTrace.frames(error))
                .extracting(TemplateTrace.Frame::kind, TemplateTrace.Frame::template,
                        TemplateTrace.Frame::target)
                .containsExactly(tuple(TemplateTrace.Kind.EXTENDS,
                        "page.peb", "layout.peb"));
    }

    @Test
    void parentBodyErrorCarriesTheExtendsChainWhenRunWrapped() {
        Map<String, String> templates = Map.of(
                "page.peb", "{% extends \"layout.peb\" %}",
                "layout.peb", "layout\n{{ missing }}");

        PebbleException error = catchThrowableOfType(PebbleException.class,
                () -> TemplateTrace.run(() -> engine(templates)
                        .getTemplate("page.peb").evaluate(new StringWriter())));

        assertThat(TemplateTrace.frames(error))
                .extracting(TemplateTrace.Frame::kind, TemplateTrace.Frame::template,
                        TemplateTrace.Frame::line, TemplateTrace.Frame::target)
                .containsExactly(tuple(
                        TemplateTrace.Kind.EXTENDS, "page.peb", 1, "layout.peb"));
    }

    @Test
    void parentBodyErrorWithoutRunHasNoAttachedTrace() {
        Map<String, String> templates = Map.of(
                "page.peb", "{% extends \"layout.peb\" %}",
                "layout.peb", "layout\n{{ missing }}");

        PebbleException error = catchThrowableOfType(PebbleException.class,
                () -> engine(templates)
                        .getTemplate("page.peb").evaluate(new StringWriter()));

        assertThat(TemplateTrace.frames(error)).isEmpty();
    }

    @Test
    void aLaterExtendsRenderDropsFramesFromAnEarlierSuccessfulRender() throws Exception {
        Map<String, String> templates = Map.of(
                "good.peb", "{% extends \"good-layout.peb\" %}",
                "good-layout.peb", "good",
                "bad.peb", "{% extends \"missing.peb\" %}");
        engine(templates).getTemplate("good.peb").evaluate(new StringWriter());

        PebbleException error = catchThrowableOfType(PebbleException.class,
                () -> engine(templates).getTemplate("bad.peb").evaluate(new StringWriter()));

        assertThat(TemplateTrace.frames(error))
                .extracting(TemplateTrace.Frame::kind, TemplateTrace.Frame::template,
                        TemplateTrace.Frame::target)
                .containsExactly(tuple(TemplateTrace.Kind.EXTENDS, "bad.peb", "missing.peb"));
    }

    @Test
    void extendsCycleThrowsPebbleExceptionInsteadOfOverflowing() {
        Map<String, String> templates = Map.of(
                "a.peb", "{% extends \"b.peb\" %}",
                "b.peb", "{% extends \"a.peb\" %}");

        PebbleException error = catchThrowableOfType(PebbleException.class,
                () -> engine(templates)
                        .getTemplate("a.peb").evaluate(new StringWriter()));

        assertThat(error)
                .hasMessageContaining("Extends cycle")
                .hasMessageContaining("a.peb -> b.peb -> a.peb");
    }

    @Test
    void selfExtendsThrowsPebbleExceptionInsteadOfOverflowing() {
        Map<String, String> templates = Map.of("page.peb", "{% extends \"page.peb\" %}");

        PebbleException error = catchThrowableOfType(PebbleException.class,
                () -> engine(templates)
                        .getTemplate("page.peb").evaluate(new StringWriter()));

        assertThat(error)
                .hasMessageContaining("Extends cycle")
                .hasMessageContaining("page.peb -> page.peb");
    }

    private static List<ExtendsNode> collect(String source) {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", source)))
                .build();
        PebbleTemplateImpl template = (PebbleTemplateImpl) engine.getTemplate("page.peb");
        ExtendsCollector collector = new ExtendsCollector(template);
        template.getRootNode().accept(collector);
        return collector.nodes;
    }

    private static String render(Map<String, String> templates) throws Exception {
        StringWriter writer = new StringWriter();
        engine(templates).getTemplate("page.peb").evaluate(writer);
        return writer.toString();
    }

    private static PebbleEngine engine(Map<String, String> templates) {
        return PebbleForge.engine()
                .loader(new MapLoader(templates))
                .build();
    }

    private static final class ExtendsCollector extends AbstractNodeVisitor {

        private final List<ExtendsNode> nodes = new ArrayList<>();

        ExtendsCollector(PebbleTemplateImpl template) {
            super(template);
        }

        @Override
        public void visit(ExtendsNode node) {
            nodes.add(node);
            super.visit(node);
        }
    }

    private static final class CountingParentExtension extends AbstractExtension {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Map<String, Function> getFunctions() {
            return Map.of("pick", new Function() {
                @Override
                public List<String> getArgumentNames() {
                    return List.of();
                }

                @Override
                public Object execute(Map<String, Object> args, PebbleTemplate template,
                        EvaluationContext context, int lineNumber) {
                    calls.incrementAndGet();
                    return "layout.peb";
                }
            });
        }
    }
}
