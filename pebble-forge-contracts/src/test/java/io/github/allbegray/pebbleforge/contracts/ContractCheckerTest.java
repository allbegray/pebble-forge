package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.contracts.testing.MapLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContractCheckerTest {

    @Test
    void acceptsDeclaredModelVariables() {
        ContractResult result = check(
                Map.of("page.peb", "{{ page.title }}{{ items }}"),
                ModelSchema.builder().variables("page", "items").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void reportsMissingModelVariable() {
        ContractResult result = check(
                Map.of("page.peb", "{{ missing }}"),
                ModelSchema.builder().build());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.format())
                .contains("page.peb:1: ERROR")
                .contains("[missing]");
    }

    @Test
    void acceptsLoopVariables() {
        ContractResult result = check(
                Map.of("page.peb",
                        "{% for item in items %}{{ item }}{{ loop.index }}{% endfor %}"),
                ModelSchema.builder().variable("items").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void acceptsSetVariables() {
        ContractResult result = check(
                Map.of("page.peb", "{% set name = \"Ada\" %}{{ name }}"),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void acceptsWithVariables() {
        ContractResult result = check(
                Map.of("page.peb",
                        "{% with {\"name\": \"Ada\"} %}{{ name }}{% endwith %}"),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void acceptsComponentProps() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% component "card" with {"title": "Hello"} %}{% endcomponent %}
                                """,
                        "components/card.peb", """
                                {% props title %}<h1>{{ title }}</h1>
                                """),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void acceptsComponentPropsWithDefaults() {
        ContractResult result = check(
                Map.of(
                        "page.peb", "{% component \"card\" %}{% endcomponent %}",
                        "components/card.peb", """
                                {% props title = "Default" %}<h1>{{ title }}</h1>
                                """),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void reportsMissingRequiredProp() {
        ContractResult result = check(
                Map.of(
                        "page.peb", "{% component \"card\" %}{% endcomponent %}",
                        "components/card.peb", "{% props title %}<h1>{{ title }}</h1>"),
                ModelSchema.builder().build());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.format())
                .contains("Component [card] requires prop [title]");
    }

    @Test
    void warnsOnUnknownProp() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% component "card" with {"title": "Hello", "extra": "x"} %}{% endcomponent %}
                                """,
                        "components/card.peb", "{% props title %}<h1>{{ title }}</h1>"),
                ModelSchema.builder().build());

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.format())
                .contains("WARNING")
                .contains("does not declare prop [extra]");
    }

    @Test
    void warnsOnDynamicComponentName() {
        ContractResult result = check(
                Map.of("page.peb", "{% component name %}{% endcomponent %}"),
                ModelSchema.builder().variable("name").build());

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.format())
                .contains("WARNING")
                .contains("Component name is dynamic");
    }

    @Test
    void warnsOnDynamicPropName() {
        ContractResult result = check(
                Map.of(
                        "page.peb", "{% component \"card\" with {key: \"x\"} %}{% endcomponent %}",
                        "components/card.peb", "{% props %}"),
                ModelSchema.builder().variable("key").build());

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.format())
                .contains("WARNING")
                .contains("dynamic prop name");
    }

    @Test
    void checksVariablesInsideComponentTemplates() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% component "card" with {"title": "Hello"} %}{% endcomponent %}
                                """,
                        "components/card.peb", """
                                {% props title %}{{ page.title }}{{ title }}
                                """),
                ModelSchema.builder().build());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.format())
                .contains("components/card.peb")
                .contains("[page]");
    }

    @Test
    void reportsParseErrors() {
        ContractResult result = check(
                Map.of("page.peb", "{% if %}"),
                ModelSchema.any());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.format()).contains("Could not parse template");
    }

    @Test
    void reportsMissingComponentAtTheCallLine() {
        ContractResult result = check(
                Map.of("page.peb",
                        "first line\n{% component \"card\" %}{% endcomponent %}"),
                ModelSchema.any());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.line()).isEqualTo(2);
            assertThat(error.message())
                    .contains("Component [card]")
                    .contains("components/card.peb");
        });
    }

    @Test
    void reportsMissingRootTemplateAtLineZero() {
        ContractResult result = check(Map.of(), ModelSchema.any());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isZero();
            assertThat(error.message()).contains("Could not load template");
        });
    }

    @Test
    void allowAnySchemaStillChecksComponentProps() {
        ContractResult result = check(
                Map.of(
                        "page.peb", "{% component \"card\" %}{% endcomponent %}{{ missing }}",
                        "components/card.peb", "{% props title %}<h1>{{ title }}</h1>"),
                ModelSchema.any());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.format())
                .doesNotContain("[missing]")
                .contains("requires prop [title]");
    }

    @Test
    void checksSwitchSubjectVariables() {
        ContractResult result = check(
                Map.of("page.peb",
                        "{% switch missing %}{% case 1 %}one{% endswitch %}"),
                ModelSchema.builder().build());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.format()).contains("[missing]");
    }

    @Test
    void checksFilterAndFunctionArguments() {
        ContractResult result = check(
                Map.of("page.peb", "{{ missing | default(\"x\") }}"),
                ModelSchema.builder().build());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.format()).contains("[missing]");
    }

    private static ContractResult check(Map<String, String> templates, ModelSchema model) {
        return ContractChecker.builder()
                .loader(new MapLoader(templates))
                .components(Components.defaults())
                .model(model)
                .build()
                .check("page.peb");
    }
}
