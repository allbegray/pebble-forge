package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.component.PropsNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class TemplateScan {

    final String templateName;
    /**
     * Names definitely visible when the root flow finishes. A declaration
     * inside a branch or a loop appears here only when every path leaves it
     * visible.
     */
    final Set<String> rootDeclarations = new LinkedHashSet<>();
    /**
     * Per-block names newly visible when that block's flow finishes. A name is
     * absent when the block never declares it or declares it on only some
     * paths. Nested block declarations stay with the nested block.
     */
    final Map<String, Set<String>> blockDeclarations = new LinkedHashMap<>();
    /**
     * The source line where each block starts. Root reads may see a block's
     * declarations only when the block starts at or before the read line.
     */
    final Map<String, Integer> blockLines = new LinkedHashMap<>();
    /**
     * The earliest template-level line that declares each root summary name.
     */
    final Map<String, Integer> rootDeclarationLines = new LinkedHashMap<>();
    final List<VariableRead> reads = new ArrayList<>();
    final List<AttributeRead> attributeReads = new ArrayList<>();
    final List<ComponentCall> componentCalls = new ArrayList<>();
    final List<IncludeEdge> includes = new ArrayList<>();
    final Map<String, Boolean> props = new LinkedHashMap<>();
    ParentCall parent;
    Failure failure;

    TemplateScan(String templateName) {
        this.templateName = templateName;
    }

    record VariableRead(String name, int line, String block, Set<String> visible,
                        boolean closed, boolean complete) {
        VariableRead {
            visible = Set.copyOf(visible);
            if (line < 0) {
                throw new IllegalArgumentException("line must not be negative");
            }
        }
    }

    /**
     * One attribute chain rooted at a context variable. The path holds only
     * literal string segments; {@code complete} is false when any segment has
     * a dynamic name.
     */
    record AttributeRead(String variable, List<String> path, int line, String block,
                         boolean complete, Set<String> visible, boolean closed) {
        AttributeRead {
            Objects.requireNonNull(variable, "variable");
            path = List.copyOf(path);
            visible = Set.copyOf(visible);
            if (line < 0) {
                throw new IllegalArgumentException("line must not be negative");
            }
        }
    }

    record ComponentCall(String name, Set<String> props, boolean dynamicName,
                         boolean dynamicProps, int line, String block) {
    }

    sealed interface IncludeTarget permits StaticTarget, DynamicTarget {
    }

    record StaticTarget(String name) implements IncludeTarget {
        StaticTarget {
            Objects.requireNonNull(name, "name");
        }
    }

    record DynamicTarget() implements IncludeTarget {
    }

    record IncludeEdge(IncludeTarget target, String literalTarget,
                       String resolutionError, Set<String> withKeys, boolean dynamicWithKeys,
                       Set<String> visibleAtCall, boolean scopeIncomplete, int line,
                       String block) {
        IncludeEdge {
            Objects.requireNonNull(target, "target");
            withKeys = Set.copyOf(withKeys);
            visibleAtCall = Set.copyOf(visibleAtCall);
            if (line < 0) {
                throw new IllegalArgumentException("line must not be negative");
            }
        }
    }

    record ParentCall(String literalTarget, String resolvedTarget, String resolutionError,
                      boolean dynamicTarget, int line) {
        ParentCall {
            if (line < 0) {
                throw new IllegalArgumentException("line must not be negative");
            }
        }
    }

    sealed interface Failure permits LoadFailure, ParseFailure {
        String message();
    }

    record LoadFailure(String message) implements Failure {
        LoadFailure {
            Objects.requireNonNull(message, "message");
        }
    }

    record ParseFailure(String message, int line) implements Failure {
        ParseFailure {
            Objects.requireNonNull(message, "message");
            if (line < 0) {
                throw new IllegalArgumentException("line must not be negative");
            }
        }
    }

    void declareProps(List<PropsNode.Prop> declared) {
        for (var prop : declared) {
            props.put(prop.name(), prop.defaultValue() == null);
        }
    }
}
