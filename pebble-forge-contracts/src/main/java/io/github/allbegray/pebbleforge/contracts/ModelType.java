package io.github.allbegray.pebbleforge.contracts;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The attribute shape of one model variable.
 *
 * <p>This is a static shape layer, not a full expression type checker. It
 * describes which properties may be read from an object and which item shape
 * an array holds. Instances are immutable.
 */
public sealed interface ModelType {

    /**
     * Returns the permissive type that accepts every attribute read.
     *
     * @return the any type
     */
    static ModelType any() {
        return new Any();
    }

    /**
     * Returns the string type.
     *
     * @return the string type
     */
    static ModelType string() {
        return new Scalar("string");
    }

    /**
     * Returns the number type.
     *
     * @return the number type
     */
    static ModelType number() {
        return new Scalar("number");
    }

    /**
     * Returns the boolean type.
     *
     * @return the boolean type
     */
    static ModelType booleanType() {
        return new Scalar("boolean");
    }

    /**
     * Returns an object shape with the declared properties.
     *
     * @param properties the property names and their types
     * @return the object type
     */
    static ModelType object(Map<String, ModelType> properties) {
        return new ObjectType(properties);
    }

    /**
     * Returns an array shape with the declared item type.
     *
     * @param items the item type
     * @return the array type
     */
    static ModelType array(ModelType items) {
        return new ArrayType(items);
    }

    /**
     * Returns the shape name used in diagnostics.
     *
     * @return one of any, string, number, boolean, object, or array
     */
    String describe();

    /**
     * A type that accepts every attribute read.
     */
    record Any() implements ModelType {

        @Override
        public String describe() {
            return "any";
        }
    }

    /**
     * A scalar type.
     *
     * @param name the scalar name: string, number, or boolean
     */
    record Scalar(String name) implements ModelType {

        private static final Set<String> NAMES = Set.of("string", "number", "boolean");

        /**
         * Creates a scalar type.
         *
         * @param name the scalar name
         */
        public Scalar {
            Objects.requireNonNull(name, "name");
            if (!NAMES.contains(name)) {
                throw new IllegalArgumentException("Unsupported scalar type [" + name + "]");
            }
        }

        @Override
        public String describe() {
            return name;
        }
    }

    /**
     * An object shape.
     *
     * @param properties the property names and their types
     */
    record ObjectType(Map<String, ModelType> properties) implements ModelType {

        /**
         * Creates an object shape. The property map is copied.
         *
         * @param properties the property names and their types
         */
        public ObjectType {
            Objects.requireNonNull(properties, "properties");
            Map<String, ModelType> copy = new LinkedHashMap<>();
            for (Map.Entry<String, ModelType> property : properties.entrySet()) {
                String name = Objects.requireNonNull(property.getKey(), "propertyName");
                if (name.isBlank()) {
                    throw new IllegalArgumentException("Property name must not be empty");
                }
                copy.put(name, Objects.requireNonNull(property.getValue(), "propertyType"));
            }
            properties = Collections.unmodifiableMap(copy);
        }

        @Override
        public String describe() {
            return "object";
        }
    }

    /**
     * An array shape.
     *
     * @param items the item type
     */
    record ArrayType(ModelType items) implements ModelType {

        /**
         * Creates an array shape.
         *
         * @param items the item type
         */
        public ArrayType {
            Objects.requireNonNull(items, "items");
        }

        @Override
        public String describe() {
            return "array";
        }
    }
}
