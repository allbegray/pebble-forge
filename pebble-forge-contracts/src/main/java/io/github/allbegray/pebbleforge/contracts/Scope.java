package io.github.allbegray.pebbleforge.contracts;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

record Scope(Set<String> names, boolean complete) {

    static final Scope EMPTY = new Scope(Set.of(), true);

    Scope {
        names = Set.copyOf(Objects.requireNonNull(names, "names"));
    }

    Scope plus(Collection<String> more) {
        if (more.isEmpty()) {
            return this;
        }
        Set<String> merged = new LinkedHashSet<>(names);
        merged.addAll(more);
        return new Scope(merged, complete);
    }

    Scope incomplete() {
        return complete ? new Scope(names, false) : this;
    }
}
