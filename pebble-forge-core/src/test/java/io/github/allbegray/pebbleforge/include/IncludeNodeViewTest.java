package io.github.allbegray.pebbleforge.include;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.testing.MapLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractNodeVisitor;
import io.pebbletemplates.pebble.node.Node;
import io.pebbletemplates.pebble.node.expression.GetAttributeExpression;
import io.pebbletemplates.pebble.node.expression.LiteralStringExpression;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IncludeNodeViewTest {

    private static final String SOURCE = """
            first line
            {% include "a.peb" with {"title": page.title} %}
            {% include "b.peb" %}
            """;

    @Test
    void exposesTargetWithMapAndLineFromAParsedInclude() {
        List<IncludeNodeView> includes = collect(SOURCE);

        assertThat(includes).hasSize(2);

        IncludeNodeView withMap = includes.get(0);
        assertThat(withMap.getTargetExpression()).isInstanceOfSatisfying(
                LiteralStringExpression.class,
                literal -> assertThat(literal.getValue()).isEqualTo("a.peb"));
        assertThat(withMap.getLineNumber()).isEqualTo(2);
        assertThat(withMap.getWithExpression()).isNotNull();
        assertThat(withMap.getWithExpression().getEntries())
                .hasSize(1)
                .allSatisfy((key, value) -> {
                    assertThat(key).isInstanceOfSatisfying(LiteralStringExpression.class,
                            literal -> assertThat(literal.getValue()).isEqualTo("title"));
                    assertThat(value).isInstanceOf(GetAttributeExpression.class);
                });

        IncludeNodeView withoutMap = includes.get(1);
        assertThat(withoutMap.getTargetExpression()).isInstanceOfSatisfying(
                LiteralStringExpression.class,
                literal -> assertThat(literal.getValue()).isEqualTo("b.peb"));
        assertThat(withoutMap.getLineNumber()).isEqualTo(3);
        assertThat(withoutMap.getWithExpression()).isNull();
    }

    private static List<IncludeNodeView> collect(String source) {
        PebbleEngine engine = PebbleForge.engine()
                .loader(new MapLoader(Map.of("page.peb", source)))
                .build();
        PebbleTemplateImpl template = (PebbleTemplateImpl) engine.getTemplate("page.peb");
        IncludeCollector collector = new IncludeCollector(template);
        template.getRootNode().accept(collector);
        return collector.includes;
    }

    private static final class IncludeCollector extends AbstractNodeVisitor {

        private final List<IncludeNodeView> includes = new ArrayList<>();

        IncludeCollector(PebbleTemplateImpl template) {
            super(template);
        }

        @Override
        public void visit(Node node) {
            if (node instanceof IncludeNodeView include) {
                includes.add(include);
            }
        }
    }
}
