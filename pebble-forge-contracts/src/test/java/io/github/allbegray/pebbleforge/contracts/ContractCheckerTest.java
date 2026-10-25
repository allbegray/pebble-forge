package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.contracts.testing.MapLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void checksComponentsAcrossNamedRoots() {
        ContractChecker checker = ContractChecker.builder()
                .loader(new MapLoader(Map.of(
                        "page.peb", """
                                {% component "ui:card" %}{% endcomponent %}
                                {% component "admin:card" %}{% endcomponent %}
                                """,
                        "ui/card.peb", "{% props title %}<h1>{{ title }}</h1>",
                        "admin/card.peb", "{% props title %}<h1>{{ title }}</h1>")))
                .components(Components.builder()
                        .root("ui", "ui")
                        .root("admin", "admin")
                        .build())
                .model(ModelSchema.builder().build())
                .build();

        ContractResult result = checker.checkAll(
                List.of("page.peb", "ui/card.peb", "admin/card.peb"));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.format())
                .contains("Component [ui:card] requires prop [title]")
                .contains("Component [admin:card] requires prop [title]");
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

    @Test
    void includeWithLiteralWithKeyIsClean() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% include "partial.peb" with {"item": "x"} %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void includeWithoutWithUsesCallerLoopLocal() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% for item in items %}{% include "partial.peb" %}{% endfor %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().variable("items").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void includeWithoutWithUsesCallerSetLocal() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% set item = "x" %}{% include "partial.peb" %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void includeInsideWithBlockSeesKeys() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% with {"item": "x"} %}{% include "partial.peb" %}{% endwith %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void includeInsideComponentSeesProps() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% component "card" with {"item": "x"} %}{% endcomponent %}
                                """,
                        "components/card.peb", """
                                {% props item %}{% include "partial.peb" %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void includeDoesNotSeeLaterCallerLocal() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% include "partial.peb" %}{% set item = "x" %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("partial.peb:1")
                    .contains("[item]");
        });
    }

    @Test
    void includeMergesCallerLocalsAndWithKeys() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% set outer = "x" %}{% include "partial.peb" with {"item": "y"} %}
                                """,
                        "partial.peb", "{{ outer }}{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void includeWithKeysDoNotLeakToCaller() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% include "partial.peb" with {"item": "x"} %}{{ item }}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void includeWithValueReadsAreChecked() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% include "partial.peb" with {"item": missing} %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[missing]");
        });
    }

    @Test
    void nestedIncludesSeeTransitiveCallerNames() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% set item = "x" %}{% include "middle.peb" %}
                                """,
                        "middle.peb", """
                                {% include "leaf.peb" %}
                                """,
                        "leaf.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void sameScopeFromTwoCallersReportsBothCallSites() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", "{% include \"partial.peb\" %}",
                        "other.peb", "{% include \"partial.peb\" %}",
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "other.peb", "partial.peb"));

        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors()).extracting(ContractDiagnostic::template)
                .containsExactlyInAnyOrder("page.peb", "other.peb");
    }

    @Test
    void multipleCallersGetPerCallSiteDiagnostics() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% for item in items %}{% include "partial.peb" with {"item": item} %}{% endfor %}
                        """,
                "other.peb", """
                        {% include "partial.peb" %}
                        """,
                "partial.peb", "{{ item }}");

        ContractResult result = checker(templates,
                ModelSchema.builder().variable("items").build())
                .checkAll(List.of("page.peb", "other.peb", "partial.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("other.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("partial.peb:1")
                    .contains("[item]");
        });
    }

    @Test
    void includeTargetResolvesRelativeToCaller() {
        Map<String, String> templates = Map.of(
                "pages/page.peb", """
                        {% include "./partial.peb" with {"item": "x"} %}
                        """,
                "pages/partial.peb", "{{ item }}");

        ContractResult result = checker(templates, ModelSchema.builder().build())
                .check("pages/page.peb");

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void dynamicIncludeTargetWarnsAndReportsTargetRead() {
        ContractResult result = check(
                Map.of("page.peb", "{% include partial %}"),
                ModelSchema.builder().build());

        assertThat(result.warnings()).singleElement().satisfies(warning -> {
            assertThat(warning.line()).isEqualTo(1);
            assertThat(warning.message()).contains("dynamic");
        });
        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).contains("[partial]"));
    }

    @Test
    void dynamicIncludeTargetWithDeclaredVariableIsCleanApartFromWarning() {
        ContractResult result = check(
                Map.of("page.peb", "{% include partial %}"),
                ModelSchema.builder().variable("partial").build());

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.warnings()).singleElement()
                .satisfies(warning -> assertThat(warning.message()).contains("dynamic"));
    }

    @Test
    void dynamicCompoundIncludeTargetStillReportsBaseRead() {
        ContractResult result = check(
                Map.of("page.peb", "{% include page.layout %}"),
                ModelSchema.builder().build());

        assertThat(result.warnings()).singleElement()
                .satisfies(warning -> assertThat(warning.message()).contains("dynamic"));
        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).contains("[page]"));
    }

    @Test
    void dynamicWithKeyMakesScopeIncomplete() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% include "partial.peb" with {key: "x"} %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().variable("key").build());

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.warnings()).singleElement().satisfies(warning -> {
            assertThat(warning.line()).isEqualTo(1);
            assertThat(warning.message()).contains("dynamic with key");
        });
    }

    @Test
    void dynamicWithKeyIsACallerRead() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% include "partial.peb" with {key: "x"} %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).contains("[key]"));
        assertThat(result.format()).doesNotContain("[item]");
    }

    @Test
    void missingIncludeTargetIsDiagnostic() {
        ContractResult result = check(
                Map.of("page.peb", """
                        first line
                        {% include "missing.peb" %}
                        """),
                ModelSchema.any());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(2);
            assertThat(error.message()).contains("Included template [missing.peb]");
        });
    }

    @Test
    void missingIncludeTargetReportsEachCaller() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", "{% include \"missing.peb\" %}",
                        "other.peb", "{% include \"missing.peb\" %}"),
                ModelSchema.any())
                .checkAll(List.of("page.peb", "other.peb"));

        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors()).extracting(ContractDiagnostic::template)
                .containsExactlyInAnyOrder("page.peb", "other.peb");
    }

    @Test
    void includedParseFailureKeepsItsLineAndChain() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                first line
                                {% include "broken.peb" %}
                                """,
                        "broken.peb", "{% if %}"),
                ModelSchema.any());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("broken.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("Included template [broken.peb]")
                    .contains("page.peb:2");
        });
    }

    @Test
    void invalidIncludeTargetIsDiagnostic() {
        ContractResult result = check(
                Map.of("page.peb", "{% include \"../..\" %}"),
                ModelSchema.any());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("Include target [../..] is invalid");
        });
    }

    @Test
    void nestedMissingReadPointsAtTheInnerIncludeWithItsChain() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% include "middle.peb" %}
                                """,
                        "middle.peb", """
                                {% include "leaf.peb" %}
                                """,
                        "leaf.peb", """
                                first line
                                {{ item }}
                                """),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("middle.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("leaf.peb:2")
                    .contains("[item]")
                    .contains("include path: page.peb:1 -> middle.peb:1");
        });
    }

    @Test
    void includeCycleIsReportedOnce() {
        ContractResult result = checker(
                Map.of(
                        "a.peb", """
                                {% include "b.peb" %}
                                """,
                        "b.peb", """
                                {% include "a.peb" %}
                                """),
                ModelSchema.any())
                .check("a.peb");

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("b.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("Include cycle: a.peb -> b.peb -> a.peb");
        });
    }

    @Test
    void selfIncludeIsReported() {
        ContractResult result = checker(
                Map.of("a.peb", "{% include \"a.peb\" %}"),
                ModelSchema.any())
                .check("a.peb");

        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.message())
                        .contains("Include cycle: a.peb -> a.peb"));
    }

    @Test
    void componentDoesNotSeeCallerLocals() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% set item = "x" %}{% component "card" %}{% endcomponent %}
                                """,
                        "components/card.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("components/card.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void checkAllDoesNotCheckIncludedPartialAsRoot() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% for item in items %}{% include "partial.peb" %}{% endfor %}
                        """,
                "partial.peb", "{{ item }}");
        ContractChecker checker = checker(templates,
                ModelSchema.builder().variable("items").build());

        assertThat(checker.checkAll(List.of("page.peb", "partial.peb")).isClean()).isTrue();
    }

    @Test
    void checkOnPartialStillChecksItAsRoot() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% for item in items %}{% include "partial.peb" %}{% endfor %}
                        """,
                "partial.peb", "{{ item }}");
        ContractChecker checker = checker(templates,
                ModelSchema.builder().variable("items").build());

        ContractResult result = checker.check("partial.peb");

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("partial.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void checkAllStillChecksTrueRoot() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", "{{ missing }}",
                        "partial.peb", "{% set unused = 1 %}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "partial.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[missing]");
        });
    }

    @Test
    void checkAllStillChecksComponentModelReads() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% component "card" %}{% endcomponent %}
                                """,
                        "components/card.peb", "{{ missing }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "components/card.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("components/card.peb");
            assertThat(error.message()).contains("[missing]");
        });
    }

    @Test
    void unusedNestedComponentPartialIsNotRoot() {
        ContractResult result = checker(
                Map.of("components/card/head.peb", "{{ item }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("components/card/head.peb"));

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void componentStatusWinsOverPartialOverride() {
        ContractResult result = checkerBuilder(
                Map.of(
                        "page.peb", """
                                {% component "card" %}{% endcomponent %}
                                """,
                        "components/card.peb", "{{ missing }}"),
                ModelSchema.builder().build())
                .partialTemplates(List.of("components/card.peb"))
                .build()
                .checkAll(List.of("page.peb", "components/card.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("components/card.peb");
            assertThat(error.message()).contains("[missing]");
        });
    }

    @Test
    void checkAllReportsPureCycle() {
        ContractResult result = checker(
                Map.of(
                        "a.peb", """
                                {% include "b.peb" %}
                                """,
                        "b.peb", """
                                {% include "a.peb" %}
                                """),
                ModelSchema.any())
                .checkAll(List.of("a.peb", "b.peb"));

        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.message())
                        .contains("Include cycle: a.peb -> b.peb -> a.peb"));
    }

    @Test
    void explicitRootWinsOverIncludedPartialInference() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% for item in items %}{% include "partial.peb" with {"item": item} %}{% endfor %}
                        """,
                "partial.peb", "{{ item }}");

        ContractResult inferred = checker(templates,
                ModelSchema.builder().variable("items").build())
                .checkAll(List.of("page.peb", "partial.peb"));
        assertThat(inferred.isClean()).isTrue();

        ContractResult forced = checkerBuilder(templates,
                ModelSchema.builder().variable("items").build())
                .rootTemplates(List.of("partial.peb"))
                .build()
                .checkAll(List.of("page.peb", "partial.peb"));
        assertThat(forced.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("partial.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void explicitPartialIsNotCheckedAsRoot() {
        Map<String, String> templates = Map.of("shared.peb", "{{ item }}");
        ModelSchema model = ModelSchema.builder().build();

        ContractResult inferred = checker(templates, model)
                .checkAll(List.of("shared.peb"));
        assertThat(inferred.errors()).singleElement()
                .satisfies(error -> assertThat(error.template()).isEqualTo("shared.peb"));

        ContractResult excluded = checkerBuilder(templates, model)
                .partialTemplates(List.of("shared.peb"))
                .build()
                .checkAll(List.of("shared.peb"));
        assertThat(excluded.isClean()).isTrue();
    }

    @Test
    void checkAllReportsPureSelfCycle() {
        ContractResult result = checker(
                Map.of("a.peb", "{% include \"a.peb\" %}"),
                ModelSchema.any())
                .checkAll(List.of("a.peb"));

        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.message())
                        .contains("Include cycle: a.peb -> a.peb"));
    }

    @Test
    void checkAllReportsStructuralErrorInUnreachablePartial() {
        ContractResult result = checkerBuilder(
                Map.of(
                        "page.peb", "",
                        "broken.peb", "{% if %}"),
                ModelSchema.any())
                .partialTemplates(List.of("broken.peb"))
                .build()
                .checkAll(List.of("page.peb", "broken.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("broken.peb");
            assertThat(error.message()).contains("Could not parse template");
        });
    }

    @Test
    void parentBodyMissingReadIsReported() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% block body %}hello{% endblock %}
                                """,
                        "layout.peb", "{{ missing }}"),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[missing]");
        });
    }

    @Test
    void dynamicExtendsParentRemainsAModelRead() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends layout %}{% block body %}hello{% endblock %}
                                """,
                        "layout.peb", "{{ missing }}"),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[layout]");
        });
    }

    @Test
    void explicitRootOutsideUniverseSuppliesScope() {
        ContractResult result = checkerBuilder(
                Map.of(
                        "page.peb", "{{ item }}",
                        "layout.peb", """
                                {% set item = "x" %}{% include "page.peb" %}
                                """),
                ModelSchema.builder().build())
                .rootTemplates(List.of("layout.peb"))
                .build()
                .checkAll(List.of("page.peb"));

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void diagnosticsAreDeterministic() {
        ContractChecker checker = checker(
                Map.of(
                        "page.peb", "{{ missing }}",
                        "other.peb", "{{ alsoMissing }}"),
                ModelSchema.builder().build());

        String first = checker
                .checkAll(List.of("other.peb", "page.peb"))
                .format();
        String second = checker
                .checkAll(List.of("page.peb", "other.peb"))
                .format();

        assertThat(first).isEqualTo(second);
        assertThat(first.indexOf("other.peb")).isLessThan(first.indexOf("page.peb"));
    }

    @Test
    void includeAfterLoopDoesNotSeeTheLoopVariable() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% for item in items %}{{ item }}{% endfor %}{% include "partial.peb" %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().variable("items").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("partial.peb:1")
                    .contains("[item]");
        });
    }

    @Test
    void includeAfterWithDoesNotSeeItsKeys() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% with {"item": "x"} %}{{ item }}{% endwith %}{% include "partial.peb" %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("partial.peb:1")
                    .contains("[item]");
        });
    }

    @Test
    void includeAfterMacroDoesNotSeeMacroArguments() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% macro card(item) %}{{ item }}{% endmacro %}{% include "partial.peb" %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("partial.peb:1")
                    .contains("[item]");
        });
    }

    @Test
    void includeInsideDynamicWithWarnsAndSkipsChildReads() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% with data %}{% include "partial.peb" %}{% endwith %}
                                """,
                        "partial.peb", "{{ missing }}"),
                ModelSchema.builder().variable("data").build());

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.warnings()).singleElement().satisfies(warning -> {
            assertThat(warning.line()).isEqualTo(1);
            assertThat(warning.message()).contains("dynamic with scope");
        });
    }

    @Test
    void partialTemplateDoesNotHideAnUnreachablePage() {
        ContractResult result = checkerBuilder(
                Map.of(
                        "partial.peb", "{% include \"page.peb\" %}",
                        "page.peb", "{{ missing }}"),
                ModelSchema.builder().build())
                .partialTemplates(List.of("partial.peb"))
                .build()
                .checkAll(List.of("page.peb", "partial.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[missing]");
        });
    }

    @Test
    void checkAllDiscoversTransitiveHelperOutsideTheUniverse() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", "{% include \"helper.peb\" %}",
                        "helper.peb", "{% include \"target.peb\" %}",
                        "target.peb", "{{ item }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "target.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("helper.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("target.peb:1")
                    .contains("[item]");
        });
    }

    @Test
    void nestedComponentPartialIncludedByComponentIsNotForcedToARoot() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% component "card" with {"title": "x"} %}{% endcomponent %}
                                """,
                        "components/card.peb", """
                                {% props title %}{% with {"item": title} %}{% include "components/card/detail.peb" %}{% endwith %}
                                """,
                        "components/card/detail.peb", "{{ item }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "components/card.peb",
                        "components/card/detail.peb"));

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void nestedComponentPartialCalledAsComponentIsCheckedClosed() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", "{% component \"card/detail\" %}{% endcomponent %}",
                        "components/card/detail.peb", "{{ item }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "components/card/detail.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("components/card/detail.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void componentSelfCycleIsReportedAtTheCallLine() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", "{% component \"loop\" %}{% endcomponent %}",
                        "components/loop.peb",
                        "{% component \"loop\" %}{% endcomponent %}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "components/loop.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("components/loop.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("Component cycle")
                    .contains("components/loop.peb -> components/loop.peb");
        });
    }

    @Test
    void mutualComponentCycleIsReportedOnce() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", "{% component \"a\" %}{% endcomponent %}",
                        "components/a.peb", "{% component \"b\" %}{% endcomponent %}",
                        "components/b.peb", "{% component \"a\" %}{% endcomponent %}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "components/a.peb", "components/b.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("components/b.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("Component cycle")
                    .contains("components/a.peb -> components/b.peb -> components/a.peb");
        });
    }

    @Test
    void includedParseFailureIsReportedPerCaller() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", "{% include \"broken.peb\" %}",
                        "other.peb", "{% include \"broken.peb\" %}",
                        "broken.peb", "{% if %}"),
                ModelSchema.any())
                .checkAll(List.of("page.peb", "other.peb"));

        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors()).extracting(ContractDiagnostic::message)
                .anySatisfy(message -> assertThat(message).contains("page.peb:1"))
                .anySatisfy(message -> assertThat(message).contains("other.peb:1"));
    }

    @Test
    void dynamicComponentPropKeySuppressesRequiredPropError() {
        ContractResult result = check(
                Map.of(
                        "page.peb",
                        "{% component \"card\" with {key: \"x\"} %}{% endcomponent %}",
                        "components/card.peb", "{% props title %}{{ title }}"),
                ModelSchema.builder().variable("key").build());

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.warnings()).singleElement().satisfies(warning -> {
            assertThat(warning.message()).contains("dynamic prop name");
        });
    }

    @Test
    void invalidExtendsTargetIsReportedAtTheExtendsLine() {
        ContractResult result = checker(
                Map.of("page.peb", """
                        first line
                        {% extends "../.." %}{% block body %}{% endblock %}
                        """),
                ModelSchema.any())
                .check("page.peb");

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(2);
            assertThat(error.message()).contains("Extends target [../..] is invalid");
        });
    }

    @Test
    void childRootSetFeedsParent() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% set title = "Hello" %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "<h1>{{ title }}</h1>"),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void parentRootSetFeedsChildBlock() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% block body %}<h1>{{ title }}</h1>{% endblock %}
                                """,
                        "layout.peb", """
                                {% set title = "Hello" %}
                                """),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void parentBlockReadsChildRootSet() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% set title = "Hello" %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", """
                                {% block sidebar %}<h1>{{ title }}</h1>{% endblock %}
                                """),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void extendsTargetResolvesRelativeToChild() {
        ContractResult result = checker(
                Map.of(
                        "pages/page.peb", """
                                {% extends "./layout.peb" %}{% block body %}{{ title }}{% endblock %}
                                """,
                        "pages/layout.peb", """
                                {% set title = "Hello" %}
                                """),
                ModelSchema.builder().build())
                .check("pages/page.peb");

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void missingExtendsParentIsReportedAtTheExtendsLine() {
        ContractResult result = checker(
                Map.of("page.peb", """
                        first line
                        {% extends "missing.peb" %}{% block body %}{{ item }}{% endblock %}
                        """),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.warnings()).isEmpty();
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(2);
            assertThat(error.message())
                    .contains("Extended template [missing.peb] was not found");
        });
    }

    @Test
    void extendsParentParseFailureKeepsItsLineAndChain() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                first line
                                {% extends "broken.peb" %}{% block body %}{% endblock %}
                                """,
                        "broken.peb", "{% if %}"),
                ModelSchema.any())
                .check("page.peb");

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("broken.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("Extended template [broken.peb]")
                    .contains("page.peb:2");
        });
    }

    @Test
    void extendsCycleAndSelfExtendsAreReportedOnce() {
        ContractResult mutual = checker(
                Map.of(
                        "a.peb", """
                                {% extends "b.peb" %}{% block body %}{% endblock %}
                                """,
                        "b.peb", """
                                {% extends "a.peb" %}{% block body %}{% endblock %}
                                """),
                ModelSchema.builder().build())
                .check("a.peb");

        assertThat(mutual.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("b.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("Extends cycle: a.peb -> b.peb -> a.peb");
        });

        ContractResult self = checker(
                Map.of("a.peb", """
                        {% extends "a.peb" %}{% block body %}{% endblock %}
                        """),
                ModelSchema.builder().build())
                .check("a.peb");

        assertThat(self.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("a.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("Extends cycle: a.peb -> a.peb");
        });
    }

    @Test
    void unreachableExtendsCycleIsReportedOnce() {
        ContractResult result = checkerBuilder(
                Map.of(
                        "a.peb", """
                                {% extends "b.peb" %}{% block body %}{% endblock %}
                                """,
                        "b.peb", """
                                {% extends "a.peb" %}{% block body %}{% endblock %}
                                """),
                ModelSchema.any())
                .partialTemplates(List.of("a.peb", "b.peb"))
                .build()
                .checkAll(List.of("a.peb", "b.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("b.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("Extends cycle: a.peb -> b.peb -> a.peb");
        });
    }

    @Test
    void checkAllDoesNotInferExtendedParentAsRoot() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% extends "layout.peb" %}{% set item = "x" %}{% block body %}{% endblock %}
                        """,
                "layout.peb", "{{ item }}");

        ContractResult result = checker(templates, ModelSchema.builder().build())
                .checkAll(List.of("page.peb", "layout.peb"));

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void explicitRootParentIsCheckedStandalone() {
        Map<String, String> templates = Map.of(
                "page.peb", """
                        {% extends "layout.peb" %}{% set item = "x" %}{% block body %}{% endblock %}
                        """,
                "layout.peb", "{{ item }}");

        ContractResult result = checkerBuilder(templates, ModelSchema.builder().build())
                .rootTemplates(List.of("layout.peb"))
                .build()
                .checkAll(List.of("page.peb", "layout.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void twoChildrenWithDifferentScopesDoNotDedupe() {
        ContractResult result = checker(
                Map.of(
                        "a.peb", """
                                {% extends "layout.peb" %}{% set item = "x" %}{% block body %}{% endblock %}
                                """,
                        "b.peb", """
                                {% extends "layout.peb" %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", """
                                {% include "partial.peb" %}
                                """,
                        "partial.peb", "{{ item }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("a.peb", "b.peb", "layout.peb", "partial.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("partial.peb:1")
                    .contains("[item]");
        });
    }

    @Test
    void onlyNonOverridingChildChecksParentBlock() {
        ContractResult result = checker(
                Map.of(
                        "good.peb", """
                                {% extends "layout.peb" %}{% block content %}{% endblock %}
                                """,
                        "bad.peb", """
                                {% extends "layout.peb" %}
                                """,
                        "layout.peb", """
                                {% block content %}{% include "partial.peb" %}{% endblock %}
                                """,
                        "partial.peb", "{{ missing }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("good.peb", "bad.peb", "layout.peb", "partial.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message())
                    .contains("partial.peb:1")
                    .contains("[missing]");
        });
    }

    @Test
    void overriddenParentBlockLocalIsNotVisibleInChildBlock() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% block content %}{{ parentLocal }}{% endblock %}
                                """,
                        "layout.peb", """
                                {% block content %}{% set parentLocal = "x" %}{% endblock %}
                                """),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[parentLocal]");
        });
    }

    @Test
    void topLevelReadSeesOwnBlockSet() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% block body %}{% set item = "x" %}{% endblock %}{{ item }}
                        """),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void childBlockLocalFeedsLaterParentContent() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% block content %}{% set fromChild = "x" %}{% endblock %}
                                """,
                        "layout.peb", """
                                {% block content %}{% endblock %}{{ fromChild }}
                                """),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void childConditionalRootSetDoesNotFeedParent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% if cond %}{% set item = "x" %}{% endif %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("cond").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void childRootSetInBothBranchesFeedsParent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% if cond %}{% set item = "x" %}{% else %}{% set item = "y" %}{% endif %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("cond").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void childSwitchWithDefaultFeedsParent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% switch value %}{% case 1 %}{% set item = "x" %}{% case 2 %}{% set item = "y" %}{% default %}{% set item = "z" %}{% endswitch %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("value").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void childSwitchWithoutDefaultDoesNotFeedParent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% switch value %}{% case 1 %}{% set item = "x" %}{% case 2 %}{% set item = "y" %}{% endswitch %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("value").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void childLoopOnlySetDoesNotFeedParent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% for entry in items %}{% set item = "x" %}{% endfor %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("items").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void childSetAfterLoopFeedsParent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% for entry in items %}{% endfor %}{% set item = "x" %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("items").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void childSetInsideWithDoesNotFeedParent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% with {"scope": "x"} %}{% set item = "x" %}{% endwith %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void childSetInsideWithOnlyDoesNotFeedParent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% with {"scope": "x"} only %}{% set item = "x" %}{% endwith %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void childBlockConditionalSetDoesNotFeedLaterParentContent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% block content %}{% if cond %}{% set fromChild = "x" %}{% endif %}{% endblock %}
                                """,
                        "layout.peb", """
                                {% block content %}{% endblock %}{{ fromChild }}
                                """),
                ModelSchema.builder().variable("cond").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[fromChild]");
        });
    }

    @Test
    void childBlockSetInBothBranchesFeedsLaterParentContent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% block content %}{% if cond %}{% set fromChild = "x" %}{% else %}{% set fromChild = "y" %}{% endif %}{% endblock %}
                                """,
                        "layout.peb", """
                                {% block content %}{% endblock %}{{ fromChild }}
                                """),
                ModelSchema.builder().variable("cond").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void childBlockSetAfterLoopFeedsLaterParentContent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% block content %}{% for entry in items %}{% endfor %}{% set fromChild = "x" %}{% endblock %}
                                """,
                        "layout.peb", """
                                {% block content %}{% endblock %}{{ fromChild }}
                                """),
                ModelSchema.builder().variable("items").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void grandchildConditionalSetDoesNotFeedGrandparent() {
        ContractResult result = check(
                Map.of(
                        "page.peb", """
                                {% extends "middle.peb" %}{% if cond %}{% set item = "x" %}{% endif %}{% block body %}{% endblock %}
                                """,
                        "middle.peb", """
                                {% extends "layout.peb" %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("cond").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("layout.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void parentReadSeesChildRootSetBeforeIfOrLoop() {
        ContractResult beforeIf = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% set item = "x" %}{% if cond %}{% endif %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("cond").build());
        ContractResult beforeLoop = check(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% set item = "x" %}{% for entry in items %}{% endfor %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", "{{ item }}"),
                ModelSchema.builder().variable("items").build());

        assertThat(beforeIf.isClean()).isTrue();
        assertThat(beforeLoop.isClean()).isTrue();
    }

    @Test
    void dynamicExtendsParentWarnsAndKeepsBaseRead() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends layout %}{% block body %}{{ maybeParent }}{% endblock %}
                                """,
                        "layout.peb", "{{ missing }}"),
                ModelSchema.builder().build())
                .check("page.peb");

        assertThat(result.warnings()).singleElement().satisfies(warning -> {
            assertThat(warning.template()).isEqualTo("page.peb");
            assertThat(warning.line()).isEqualTo(1);
            assertThat(warning.message()).contains("Extends target is dynamic");
        });
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.message()).contains("[layout]");
            assertThat(result.format()).doesNotContain("[maybeParent]");
        });
    }

    @Test
    void parentOutsideUniverseIsDiscovered() {
        ContractResult result = checker(
                Map.of(
                        "page.peb", """
                                {% extends "layout.peb" %}{% block body %}{% endblock %}
                                """,
                        "layout.peb", """
                                {% extends "base.peb" %}{% block body %}{% endblock %}
                                """,
                        "base.peb", "{{ missing }}"),
                ModelSchema.builder().build())
                .checkAll(List.of("page.peb"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("base.peb");
            assertThat(error.line()).isEqualTo(1);
            assertThat(error.message()).contains("[missing]");
        });
    }

    @Test
    void readBeforeRootSetIsAnError() {
        ContractResult result = check(
                Map.of("page.peb", "{{ item }}{% set item = \"x\" %}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void setValueDoesNotSeeItsOwnDeclaration() {
        ContractResult result = check(
                Map.of("page.peb", "{% set item = item %}"),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void setValueMayReadAModelVariable() {
        ContractResult result = check(
                Map.of("page.peb", "{% set item = item %}"),
                ModelSchema.builder().variable("item").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void readBeforeSetInBlockIsAnError() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% block body %}{{ item }}{% set item = "x" %}{% endblock %}
                        """),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void readAfterSetInBlockIsClean() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% block body %}{% set item = "x" %}{{ item }}{% endblock %}
                        """),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void readAfterThenOnlyAssignmentIsAnError() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% if cond %}{% set item = "x" %}{% endif %}{{ item }}
                        """),
                ModelSchema.builder().variable("cond").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void readAfterBothBranchesAssignIsClean() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% if cond %}{% set item = "x" %}{% else %}{% set item = "y" %}{% endif %}{{ item }}
                        """),
                ModelSchema.builder().variable("cond").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void thenBranchDoesNotSeeAssignmentFromElseBranch() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% if cond %}{{ item }}{% else %}{% set item = "x" %}{% endif %}
                        """),
                ModelSchema.builder().variable("cond").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void ifConditionCannotSeeTheSetItGuards() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% if item %}{% set item = "x" %}{% endif %}
                        """),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void switchWithoutDefaultDoesNotExposeCaseSet() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% switch value %}{% case 1 %}{% set item = "x" %}{% endswitch %}{{ item }}
                        """),
                ModelSchema.builder().variable("value").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void switchAllCasesAndDefaultExposeSet() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% switch value %}{% case 1 %}{% set item = "x" %}{% case 2 %}{% set item = "y" %}{% default %}{% set item = "z" %}{% endswitch %}{{ item }}
                        """),
                ModelSchema.builder().variable("value").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void switchCaseDoesNotSeeAnotherCaseSet() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% switch value %}{% case 1 %}{% set item = "x" %}{% case 2 %}{{ item }}{% default %}{% set item = "z" %}{% endswitch %}
                        """),
                ModelSchema.builder().variable("value").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void switchCaseReadsItsOwnSet() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% switch value %}{% case 1 %}{% set item = "x" %}{{ item }}{% default %}{% set item = "z" %}{% endswitch %}
                        """),
                ModelSchema.builder().variable("value").build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void loopVariableDoesNotLeakAfterLoop() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% for item in items %}{{ item }}{% endfor %}{{ item }}
                        """),
                ModelSchema.builder().variable("items").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void loopBodySetDoesNotLeakAfterLoop() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% for entry in items %}{% set item = "x" %}{% endfor %}{{ item }}
                        """),
                ModelSchema.builder().variable("items").build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void withKeyDoesNotLeakAfterWith() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% with {"item": "x"} %}{{ item }}{% endwith %}{{ item }}
                        """),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void withKeyIsVisibleInsideWith() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% with {"item": "x"} %}{{ item }}{% endwith %}
                        """),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void withOnlyBodySeesItsOwnKeys() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% with {"item": "x"} only %}{{ item }}{% endwith %}
                        """),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void withOnlyBodyDoesNotSeeOuterSet() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% set outer = "x" %}{% with only %}{{ outer }}{% endwith %}
                        """),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[outer]");
        });
    }

    @Test
    void macroArgumentDoesNotLeakAfterMacro() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% macro card(item) %}{{ item }}{% endmacro %}{{ item }}
                        """),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[item]");
        });
    }

    @Test
    void macroBodyDoesNotSeeOuterSet() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% set outer = "x" %}{% macro card(value) %}{{ outer }}{% endmacro %}
                        """),
                ModelSchema.builder().build());

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.template()).isEqualTo("page.peb");
            assertThat(error.message()).contains("[outer]");
        });
    }

    @Test
    void macroBodySeesItsArgument() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% macro card(item) %}{{ item }}{% endmacro %}
                        """),
                ModelSchema.builder().build());

        assertThat(result.isClean()).isTrue();
    }

    @Test
    void dynamicWithSuppressesDirectReads() {
        ContractResult result = check(
                Map.of("page.peb", """
                        {% with data %}{{ missing }}{% endwith %}
                        """),
                ModelSchema.builder().variable("data").build());

        assertThat(result.isClean()).isTrue();
    }

    private static ContractChecker checker(Map<String, String> templates, ModelSchema model) {
        return checkerBuilder(templates, model).build();
    }

    private static ContractChecker.Builder checkerBuilder(Map<String, String> templates,
            ModelSchema model) {
        return ContractChecker.builder()
                .loader(new MapLoader(templates))
                .components(Components.defaults())
                .model(model);
    }

    private static ContractResult check(Map<String, String> templates, ModelSchema model) {
        return checker(templates, model).check("page.peb");
    }
}
