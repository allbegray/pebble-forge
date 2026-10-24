package io.github.allbegray.pebbleforge.contracts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TemplateScan {

    final String templateName;
    final Set<String> declaredVariables = new LinkedHashSet<>();
    final List<VariableRead> reads = new ArrayList<>();
    final List<ComponentCall> componentCalls = new ArrayList<>();
    final Map<String, Boolean> props = new LinkedHashMap<>();
    boolean failed;
    boolean complete;

    TemplateScan(String templateName) {
        this.templateName = templateName;
    }

    record VariableRead(String name, int line) {
    }

    record ComponentCall(String name, Set<String> props, boolean dynamicName,
                         boolean dynamicProps, int line) {
    }

    void declareProps(List<io.github.allbegray.pebbleforge.component.PropsNode.Prop> declared) {
        for (var prop : declared) {
            props.put(prop.name(), prop.defaultValue() == null);
            declaredVariables.add(prop.name());
        }
    }
}
