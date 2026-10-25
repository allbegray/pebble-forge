package io.github.allbegray.pebbleforge.contracts;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelSchemaTest {

    @Test
    void untypedVariableHasTheAnyType() {
        ModelSchema schema = ModelSchema.builder().variable("page").build();

        assertThat(schema.typeOf("page")).isEqualTo(ModelType.any());
    }

    @Test
    void typedVariableReplacesAnEarlierUntypedDeclaration() {
        ModelSchema schema = ModelSchema.builder()
                .variable("page")
                .variable("page", ModelType.string())
                .build();

        assertThat(schema.typeOf("page")).isEqualTo(ModelType.string());
        assertThat(schema.variables()).containsExactly("page");
    }

    @Test
    void unknownVariableHasNoTypeInARestrictedSchema() {
        ModelSchema schema = ModelSchema.builder().build();

        assertThat(schema.typeOf("page")).isNull();
    }

    @Test
    void permissiveSchemaTypesEveryVariableAsAny() {
        ModelSchema schema = ModelSchema.any();

        assertThat(schema.allows("page")).isTrue();
        assertThat(schema.typeOf("page")).isEqualTo(ModelType.any());
    }

    @Test
    void permissiveSchemaKeepsAnExplicitType() {
        ModelSchema schema = ModelSchema.builder()
                .variable("page", ModelType.string())
                .allowAnyVariable()
                .build();

        assertThat(schema.typeOf("page")).isEqualTo(ModelType.string());
        assertThat(schema.typeOf("other")).isEqualTo(ModelType.any());
    }

    @Test
    void describesEveryShapeName() {
        assertThat(ModelType.any().describe()).isEqualTo("any");
        assertThat(ModelType.string().describe()).isEqualTo("string");
        assertThat(ModelType.number().describe()).isEqualTo("number");
        assertThat(ModelType.booleanType().describe()).isEqualTo("boolean");
        assertThat(ModelType.array(ModelType.any()).describe()).isEqualTo("array");
        assertThat(ModelType.object(Map.of()).describe()).isEqualTo("object");
    }

    @Test
    void objectPropertiesAreCopiedAndImmutable() {
        Map<String, ModelType> properties = new LinkedHashMap<>();
        properties.put("title", ModelType.string());
        ModelType.ObjectType type = (ModelType.ObjectType) ModelType.object(properties);
        properties.put("late", ModelType.number());

        assertThat(type.properties()).containsOnlyKeys("title");
        assertThatThrownBy(() -> type.properties().put("extra", ModelType.any()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsUnknownScalarNamesAndNullParts() {
        assertThatThrownBy(() -> new ModelType.Scalar("mystery"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModelType.object(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ModelType.object(Map.of("title", null)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ModelType.array(null))
                .isInstanceOf(NullPointerException.class);
    }
}
