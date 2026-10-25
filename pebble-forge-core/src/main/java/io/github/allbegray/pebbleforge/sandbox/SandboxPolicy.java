package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.methodaccess.MethodAccessValidator;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Capability policy for templates that come from an untrusted source.
 */
public final class SandboxPolicy {

    private static final Set<String> SAFE_TAGS = Set.of(
            "if", "for", "set", "verbatim", "include", "component", "slot", "props",
            "switch", "break", "continue", "with");

    private static final Set<String> SAFE_FILTERS = Set.of(
            "escape", "default", "length", "join", "upper", "lower", "capitalize", "title",
            "trim", "replace", "slice", "first", "last", "reverse", "sort", "split", "abs",
            "numberformat", "date", "abbreviate", "urlencode");

    private static final Set<String> SAFE_FUNCTIONS = Set.of(
            "slot", "i18n", "max", "min");

    private static final Set<String> SAFE_TESTS = Set.of(
            "empty", "null", "iterable", "even", "odd", "map");

    private final Set<String> allowedTags;
    private final Set<String> deniedTags;
    private final boolean allTags;

    private final Set<String> allowedFilters;
    private final Set<String> deniedFilters;
    private final boolean allFilters;

    private final Set<String> allowedFunctions;
    private final Set<String> deniedFunctions;
    private final boolean allFunctions;

    private final Set<String> allowedTests;
    private final Set<String> deniedTests;
    private final boolean allTests;

    private final Set<String> allowedGlobalVariables;
    private final boolean allGlobalVariables;

    private final int maxRenderedChars;
    private final int maxLoopIterations;
    private final int maxIncludeDepth;
    private final int maxSlotChars;
    private final MethodAccessValidator methodAccessValidator;
    private final Predicate<String> templateNameValidator;
    private final boolean denyPublicFields;
    private final boolean forceAutoEscaping;

    private SandboxPolicy(Builder builder) {
        this.allowedTags = Set.copyOf(builder.allowedTags);
        this.deniedTags = Set.copyOf(builder.deniedTags);
        this.allTags = builder.allTags;
        this.allowedFilters = Set.copyOf(builder.allowedFilters);
        this.deniedFilters = Set.copyOf(builder.deniedFilters);
        this.allFilters = builder.allFilters;
        this.allowedFunctions = Set.copyOf(builder.allowedFunctions);
        this.deniedFunctions = Set.copyOf(builder.deniedFunctions);
        this.allFunctions = builder.allFunctions;
        this.allowedTests = Set.copyOf(builder.allowedTests);
        this.deniedTests = Set.copyOf(builder.deniedTests);
        this.allTests = builder.allTests;
        this.allowedGlobalVariables = Set.copyOf(builder.allowedGlobalVariables);
        this.allGlobalVariables = builder.allGlobalVariables;
        this.maxRenderedChars = builder.maxRenderedChars;
        this.maxLoopIterations = builder.maxLoopIterations;
        this.maxIncludeDepth = builder.maxIncludeDepth;
        this.maxSlotChars = builder.maxSlotChars;
        this.methodAccessValidator = builder.methodAccessValidator;
        this.templateNameValidator = builder.templateNameValidator;
        this.denyPublicFields = builder.denyPublicFields;
        this.forceAutoEscaping = builder.forceAutoEscaping;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SandboxPolicy strict() {
        return builder().build();
    }

    public boolean isTagAllowed(String tag) {
        return !deniedTags.contains(tag) && (allTags || allowedTags.contains(tag));
    }

    public boolean isFilterAllowed(String filter) {
        return !deniedFilters.contains(filter) && (allFilters || allowedFilters.contains(filter));
    }

    public boolean isFunctionAllowed(String function) {
        return !deniedFunctions.contains(function)
                && (allFunctions || allowedFunctions.contains(function));
    }

    public boolean isTestAllowed(String test) {
        return !deniedTests.contains(test) && (allTests || allowedTests.contains(test));
    }

    public boolean isGlobalVariableAllowed(String name) {
        return allGlobalVariables || allowedGlobalVariables.contains(name);
    }

    public int maxRenderedChars() {
        return maxRenderedChars;
    }

    public int maxLoopIterations() {
        return maxLoopIterations;
    }

    public int maxIncludeDepth() {
        return maxIncludeDepth;
    }

    public int maxSlotChars() {
        return maxSlotChars;
    }

    public MethodAccessValidator methodAccessValidator() {
        return methodAccessValidator;
    }

    public Predicate<String> templateNameValidator() {
        return templateNameValidator;
    }

    public boolean denyPublicFields() {
        return denyPublicFields;
    }

    public boolean forceAutoEscaping() {
        return forceAutoEscaping;
    }

    public static final class Builder {

        private final Set<String> allowedTags = new LinkedHashSet<>(SAFE_TAGS);
        private final Set<String> deniedTags = new LinkedHashSet<>();
        private boolean allTags;

        private final Set<String> allowedFilters = new LinkedHashSet<>(SAFE_FILTERS);
        private final Set<String> deniedFilters = new LinkedHashSet<>();
        private boolean allFilters;

        private final Set<String> allowedFunctions = new LinkedHashSet<>(SAFE_FUNCTIONS);
        private final Set<String> deniedFunctions = new LinkedHashSet<>();
        private boolean allFunctions;

        private final Set<String> allowedTests = new LinkedHashSet<>(SAFE_TESTS);
        private final Set<String> deniedTests = new LinkedHashSet<>();
        private boolean allTests;

        private final Set<String> allowedGlobalVariables = new LinkedHashSet<>();
        private boolean allGlobalVariables;

        private int maxRenderedChars = 1_000_000;
        private int maxLoopIterations = 100_000;
        private int maxIncludeDepth = 16;
        private int maxSlotChars = 1_000_000;
        private MethodAccessValidator methodAccessValidator = new SandboxMethodAccessValidator();
        private Predicate<String> templateNameValidator = name -> true;
        private boolean denyPublicFields = true;
        private boolean forceAutoEscaping = true;

        public Builder allowTags(String... tags) {
            for (String tag : tags) {
                allowedTags.add(requireName(tag, "tag"));
                deniedTags.remove(tag);
            }
            return this;
        }

        public Builder allowOnlyTags(String... tags) {
            allowedTags.clear();
            deniedTags.clear();
            allTags = false;
            return allowTags(tags);
        }

        public Builder denyTags(String... tags) {
            for (String tag : tags) {
                deniedTags.add(requireName(tag, "tag"));
            }
            return this;
        }

        public Builder allowAllTags() {
            allTags = true;
            return this;
        }

        public Builder allowFilters(String... filters) {
            for (String filter : filters) {
                allowedFilters.add(requireName(filter, "filter"));
                deniedFilters.remove(filter);
            }
            return this;
        }

        public Builder allowOnlyFilters(String... filters) {
            allowedFilters.clear();
            deniedFilters.clear();
            allFilters = false;
            allowedFilters.add("escape");
            return allowFilters(filters);
        }

        public Builder denyFilters(String... filters) {
            for (String filter : filters) {
                if ("escape".equals(filter)) {
                    throw new IllegalArgumentException("The escape filter is required by autoescaping");
                }
                deniedFilters.add(requireName(filter, "filter"));
            }
            return this;
        }

        public Builder allowAllFilters() {
            allFilters = true;
            return this;
        }

        public Builder allowRawOutput() {
            return allowFilters("raw");
        }

        public Builder allowFunctions(String... functions) {
            for (String function : functions) {
                allowedFunctions.add(requireName(function, "function"));
                deniedFunctions.remove(function);
            }
            return this;
        }

        public Builder allowOnlyFunctions(String... functions) {
            allowedFunctions.clear();
            deniedFunctions.clear();
            allFunctions = false;
            return allowFunctions(functions);
        }

        public Builder denyFunctions(String... functions) {
            for (String function : functions) {
                deniedFunctions.add(requireName(function, "function"));
            }
            return this;
        }

        public Builder allowAllFunctions() {
            allFunctions = true;
            return this;
        }

        public Builder allowTests(String... tests) {
            for (String test : tests) {
                allowedTests.add(requireName(test, "test"));
                deniedTests.remove(test);
            }
            return this;
        }

        public Builder allowOnlyTests(String... tests) {
            allowedTests.clear();
            deniedTests.clear();
            allTests = false;
            return allowTests(tests);
        }

        public Builder denyTests(String... tests) {
            for (String test : tests) {
                deniedTests.add(requireName(test, "test"));
            }
            return this;
        }

        public Builder allowAllTests() {
            allTests = true;
            return this;
        }

        public Builder allowGlobalVariables(String... variables) {
            for (String variable : variables) {
                allowedGlobalVariables.add(requireName(variable, "global variable"));
            }
            return this;
        }

        public Builder allowAllGlobalVariables() {
            allGlobalVariables = true;
            return this;
        }

        public Builder allowComponents() {
            return allowTags("component", "slot", "props").allowFunctions("slot");
        }

        public Builder maxRenderedChars(int maxRenderedChars) {
            if (maxRenderedChars < 0) {
                throw new IllegalArgumentException("maxRenderedChars must not be negative");
            }
            this.maxRenderedChars = maxRenderedChars;
            return this;
        }

        public Builder maxLoopIterations(int maxLoopIterations) {
            if (maxLoopIterations < -1) {
                throw new IllegalArgumentException("maxLoopIterations must be -1 or greater");
            }
            this.maxLoopIterations = maxLoopIterations;
            return this;
        }

        public Builder maxIncludeDepth(int maxIncludeDepth) {
            if (maxIncludeDepth < -1) {
                throw new IllegalArgumentException("maxIncludeDepth must be -1 or greater");
            }
            this.maxIncludeDepth = maxIncludeDepth;
            return this;
        }

        public Builder maxSlotChars(int maxSlotChars) {
            if (maxSlotChars < -1) {
                throw new IllegalArgumentException("maxSlotChars must be -1 or greater");
            }
            this.maxSlotChars = maxSlotChars;
            return this;
        }

        public Builder methodAccessValidator(MethodAccessValidator validator) {
            this.methodAccessValidator = Objects.requireNonNull(validator, "validator");
            return this;
        }

        public Builder templateNameValidator(Predicate<String> validator) {
            this.templateNameValidator = Objects.requireNonNull(validator, "validator");
            return this;
        }

        public Builder denyPublicFields(boolean denyPublicFields) {
            this.denyPublicFields = denyPublicFields;
            return this;
        }

        public Builder forceAutoEscaping(boolean forceAutoEscaping) {
            this.forceAutoEscaping = forceAutoEscaping;
            return this;
        }

        public SandboxPolicy build() {
            return new SandboxPolicy(this);
        }

        private static String requireName(String value, String kind) {
            Objects.requireNonNull(value, kind);
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("The " + kind + " name must not be empty");
            }
            return trimmed;
        }
    }
}
