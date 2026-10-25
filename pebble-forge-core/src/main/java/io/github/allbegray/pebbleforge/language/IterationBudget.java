package io.github.allbegray.pebbleforge.language;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import io.pebbletemplates.pebble.template.ScopeChain;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shares one loop-iteration budget across every loop in a render.
 */
final class IterationBudget {

    private static final Map<ScopeChain, Counter> BUDGETS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private IterationBudget() {
    }

    static Counter forScope(ScopeChain scope, int max) {
        synchronized (BUDGETS) {
            Counter counter = BUDGETS.get(scope);
            if (counter == null || counter.max != max) {
                counter = new Counter(max);
                BUDGETS.put(scope, counter);
            }
            return counter;
        }
    }

    static final class Counter {

        private final int max;
        private final AtomicInteger remaining;

        private Counter(int max) {
            this.max = max;
            this.remaining = new AtomicInteger(max);
        }

        void consume(PebbleTemplateImpl self, int lineNumber) {
            if (remaining.decrementAndGet() < 0) {
                throw new PebbleException(
                        null,
                        "Total loop iteration limit of " + max + " reached",
                        lineNumber,
                        self.getName());
            }
        }
    }
}
