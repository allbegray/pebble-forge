package io.pebbleforge.component;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

/**
 * Returns captured slot HTML without escaping it a second time.
 */
public final class SlotFunction implements Function {

    @Override
    public List<String> getArgumentNames() {
        return List.of("name");
    }

    @Override
    public Object execute(
            Map<String, Object> args,
            PebbleTemplate self,
            EvaluationContext context,
            int lineNumber) {
        Object rawName = args.get("name");
        String name = rawName == null ? "body" : String.valueOf(rawName);
        Object slots = context.getVariable("__slots");
        if (!(slots instanceof Map<?, ?> map)) {
            return new SafeString("");
        }
        Object value = map.get(name);
        if (value == null) {
            return new SafeString("");
        }
        return value instanceof SafeString ? value : new SafeString(String.valueOf(value));
    }
}
