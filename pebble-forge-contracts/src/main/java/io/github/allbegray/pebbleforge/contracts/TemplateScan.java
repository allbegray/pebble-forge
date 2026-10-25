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
    final Set<String> rootDeclarations = new LinkedHashSet<>();
    final Map<String, Set<String>> blockDeclarations = new LinkedHashMap<>();
    final List<VariableRead> reads = new ArrayList<>();
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
