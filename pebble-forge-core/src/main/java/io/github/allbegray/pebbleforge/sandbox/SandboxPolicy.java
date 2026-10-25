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
    private final int maxTotalIterations;
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
        this.maxTotalIterations = builder.maxTotalIterations;
        this.maxIncludeDepth = builder.maxIncludeDepth;
        this.maxSlotChars = builder.maxSlotChars;
        this.methodAccessValidator = builder.methodAccessValidator;
        this.templateNameValidator = builder.templateNameValidator;
        this.denyPublicFields = builder.denyPublicFields;
        this.forceAutoEscaping = builder.forceAutoEscaping;
    }

    /**
     * Creates a builder with the strict default policy.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the strict default policy.
     *
     * @return the default policy
     */
    public static SandboxPolicy strict() {
        return builder().build();
    }

    /**
     * Returns whether a tag is allowed.
     *
     * @param tag the tag name
     * @return true when the tag may run
     */
    public boolean isTagAllowed(String tag) {
        return !deniedTags.contains(tag) && (allTags || allowedTags.contains(tag));
    }

    /**
     * Returns whether a filter is allowed.
     *
     * @param filter the filter name
     * @return true when the filter may run
     */
    public boolean isFilterAllowed(String filter) {
        return !deniedFilters.contains(filter) && (allFilters || allowedFilters.contains(filter));
    }

    /**
     * Returns whether a function is allowed.
     *
     * @param function the function name
     * @return true when the function may run
     */
    public boolean isFunctionAllowed(String function) {
        return !deniedFunctions.contains(function)
                && (allFunctions || allowedFunctions.contains(function));
    }

    /**
     * Returns whether a test is allowed.
     *
     * @param test the test name
     * @return true when the test may run
     */
    public boolean isTestAllowed(String test) {
        return !deniedTests.contains(test) && (allTests || allowedTests.contains(test));
    }

    /**
     * Returns whether a global variable is exposed to templates.
     *
     * @param name the variable name
     * @return true when the variable is visible
     */
    public boolean isGlobalVariableAllowed(String name) {
        return allGlobalVariables || allowedGlobalVariables.contains(name);
    }

    /**
     * Returns the maximum rendered output size.
     *
     * @return the limit in characters, or -1 for no limit
     */
    public int maxRenderedChars() {
        return maxRenderedChars;
    }

    /**
     * Returns the maximum iterations of one loop.
     *
     * @return the limit, or -1 for no limit
     */
    public int maxLoopIterations() {
        return maxLoopIterations;
    }

    /**
     * Returns the maximum iterations across all nested loops.
     *
     * @return the limit, or -1 for no limit
     */
    public int maxTotalIterations() {
        return maxTotalIterations;
    }

    /**
     * Returns the maximum nested include depth.
     *
     * @return the limit, or -1 for no limit
     */
    public int maxIncludeDepth() {
        return maxIncludeDepth;
    }

    /**
     * Returns the maximum slot content length.
     *
     * @return the limit in characters, or -1 for no limit
     */
    public int maxSlotChars() {
        return maxSlotChars;
    }

    /**
     * Returns the validator for method access.
     *
     * @return the method access validator
     */
    public MethodAccessValidator methodAccessValidator() {
        return methodAccessValidator;
    }

    /**
     * Returns the validator for template names.
     *
     * @return the template name validator
     */
    public Predicate<String> templateNameValidator() {
        return templateNameValidator;
    }

    /**
     * Returns whether public field access is denied.
     *
     * @return true when public fields are denied
     */
    public boolean denyPublicFields() {
        return denyPublicFields;
    }

    /**
     * Returns whether the policy forces auto-escaping.
     *
     * @return true when auto-escaping is forced
     */
    public boolean forceAutoEscaping() {
        return forceAutoEscaping;
    }

    /**
     * Builds a {@link SandboxPolicy}. The defaults allow the safe tag, filter, function, and test
     * sets and deny dangerous method access.
     */
    public static final class Builder {

        /**
         * Creates a builder with the strict default policy. Prefer {@link SandboxPolicy#builder()}.
         */
        public Builder() {
        }

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
        private int maxTotalIterations = 1_000_000;
        private int maxIncludeDepth = 16;
        private int maxSlotChars = 1_000_000;
        private MethodAccessValidator methodAccessValidator = new SandboxMethodAccessValidator();
        private Predicate<String> templateNameValidator = name -> true;
        private boolean denyPublicFields = true;
        private boolean forceAutoEscaping = true;

        /**
         * Allows tags in addition to the current set.
         *
         * @param tags the tag names
         * @return this builder
         */
        public Builder allowTags(String... tags) {
            for (String tag : tags) {
                allowedTags.add(requireName(tag, "tag"));
                deniedTags.remove(tag);
            }
            return this;
        }

        /**
         * Replaces the allowed tag set.
         *
         * @param tags the only tag names that may run
         * @return this builder
         */
        public Builder allowOnlyTags(String... tags) {
            allowedTags.clear();
            deniedTags.clear();
            allTags = false;
            return allowTags(tags);
        }

        /**
         * Denies tags, including tags in the safe default set.
         *
         * @param tags the tag names to deny
         * @return this builder
         */
        public Builder denyTags(String... tags) {
            for (String tag : tags) {
                deniedTags.add(requireName(tag, "tag"));
            }
            return this;
        }

        /**
         * Allows every tag except explicitly denied tags.
         *
         * @return this builder
         */
        public Builder allowAllTags() {
            allTags = true;
            return this;
        }

        /**
         * Allows filters in addition to the current set.
         *
         * @param filters the filter names
         * @return this builder
         */
        public Builder allowFilters(String... filters) {
            for (String filter : filters) {
                allowedFilters.add(requireName(filter, "filter"));
                deniedFilters.remove(filter);
            }
            return this;
        }

        /**
         * Replaces the allowed filter set. The {@code escape} filter always stays allowed.
         *
         * @param filters the only filter names that may run
         * @return this builder
         */
        public Builder allowOnlyFilters(String... filters) {
            allowedFilters.clear();
            deniedFilters.clear();
            allFilters = false;
            allowedFilters.add("escape");
            return allowFilters(filters);
        }

        /**
         * Denies filters. The {@code escape} filter cannot be denied.
         *
         * @param filters the filter names to deny
         * @return this builder
         */
        public Builder denyFilters(String... filters) {
            for (String filter : filters) {
                if ("escape".equals(filter)) {
                    throw new IllegalArgumentException(
                            "The escape filter is required by autoescaping");
                }
                deniedFilters.add(requireName(filter, "filter"));
            }
            return this;
        }

        /**
         * Allows every filter except explicitly denied filters.
         *
         * @return this builder
         */
        public Builder allowAllFilters() {
            allFilters = true;
            return this;
        }

        /**
         * Allows the {@code raw} filter, which skips escaping.
         *
         * @return this builder
         */
        public Builder allowRawOutput() {
            return allowFilters("raw");
        }

        /**
         * Allows functions in addition to the current set.
         *
         * @param functions the function names
         * @return this builder
         */
        public Builder allowFunctions(String... functions) {
            for (String function : functions) {
                allowedFunctions.add(requireName(function, "function"));
                deniedFunctions.remove(function);
            }
            return this;
        }

        /**
         * Replaces the allowed function set.
         *
         * @param functions the only function names that may run
         * @return this builder
         */
        public Builder allowOnlyFunctions(String... functions) {
            allowedFunctions.clear();
            deniedFunctions.clear();
            allFunctions = false;
            return allowFunctions(functions);
        }

        /**
         * Denies functions, including functions in the safe default set.
         *
         * @param functions the function names to deny
         * @return this builder
         */
        public Builder denyFunctions(String... functions) {
            for (String function : functions) {
                deniedFunctions.add(requireName(function, "function"));
            }
            return this;
        }

        /**
         * Allows every function except explicitly denied functions.
         *
         * @return this builder
         */
        public Builder allowAllFunctions() {
            allFunctions = true;
            return this;
        }

        /**
         * Allows tests in addition to the current set.
         *
         * @param tests the test names
         * @return this builder
         */
        public Builder allowTests(String... tests) {
            for (String test : tests) {
                allowedTests.add(requireName(test, "test"));
                deniedTests.remove(test);
            }
            return this;
        }

        /**
         * Replaces the allowed test set.
         *
         * @param tests the only test names that may run
         * @return this builder
         */
        public Builder allowOnlyTests(String... tests) {
            allowedTests.clear();
            deniedTests.clear();
            allTests = false;
            return allowTests(tests);
        }

        /**
         * Denies tests, including tests in the safe default set.
         *
         * @param tests the test names to deny
         * @return this builder
         */
        public Builder denyTests(String... tests) {
            for (String test : tests) {
                deniedTests.add(requireName(test, "test"));
            }
            return this;
        }

        /**
         * Allows every test except explicitly denied tests.
         *
         * @return this builder
         */
        public Builder allowAllTests() {
            allTests = true;
            return this;
        }

        /**
         * Exposes global variables to templates.
         *
         * @param variables the global variable names
         * @return this builder
         */
        public Builder allowGlobalVariables(String... variables) {
            for (String variable : variables) {
                allowedGlobalVariables.add(requireName(variable, "global variable"));
            }
            return this;
        }

        /**
         * Exposes every global variable.
         *
         * @return this builder
         */
        public Builder allowAllGlobalVariables() {
            allGlobalVariables = true;
            return this;
        }

        /**
         * Allows the component, slot, and props tags and the slot function.
         *
         * @return this builder
         */
        public Builder allowComponents() {
            return allowTags("component", "slot", "props").allowFunctions("slot");
        }

        /**
         * Sets the maximum rendered output size.
         *
         * @param maxRenderedChars the limit in characters
         * @return this builder
         */
        public Builder maxRenderedChars(int maxRenderedChars) {
            if (maxRenderedChars < 0) {
                throw new IllegalArgumentException("maxRenderedChars must not be negative");
            }
            this.maxRenderedChars = maxRenderedChars;
            return this;
        }

        /**
         * Sets the maximum iterations of one loop.
         *
         * @param maxLoopIterations the limit, or -1 for no limit
         * @return this builder
         */
        public Builder maxLoopIterations(int maxLoopIterations) {
            if (maxLoopIterations < -1) {
                throw new IllegalArgumentException("maxLoopIterations must be -1 or greater");
            }
            this.maxLoopIterations = maxLoopIterations;
            return this;
        }

        /**
         * Sets the maximum iterations across all nested loops.
         *
         * @param maxTotalIterations the limit, or -1 for no limit
         * @return this builder
         */
        public Builder maxTotalIterations(int maxTotalIterations) {
            if (maxTotalIterations < -1) {
                throw new IllegalArgumentException("maxTotalIterations must be -1 or greater");
            }
            this.maxTotalIterations = maxTotalIterations;
            return this;
        }

        /**
         * Sets the maximum nested include depth.
         *
         * @param maxIncludeDepth the limit, or -1 for no limit
         * @return this builder
         */
        public Builder maxIncludeDepth(int maxIncludeDepth) {
            if (maxIncludeDepth < -1) {
                throw new IllegalArgumentException("maxIncludeDepth must be -1 or greater");
            }
            this.maxIncludeDepth = maxIncludeDepth;
            return this;
        }

        /**
         * Sets the maximum slot content length.
         *
         * @param maxSlotChars the limit in characters, or -1 for no limit
         * @return this builder
         */
        public Builder maxSlotChars(int maxSlotChars) {
            if (maxSlotChars < -1) {
                throw new IllegalArgumentException("maxSlotChars must be -1 or greater");
            }
            this.maxSlotChars = maxSlotChars;
            return this;
        }

        /**
         * Sets the validator for method access.
         *
         * @param validator the validator
         * @return this builder
         */
        public Builder methodAccessValidator(MethodAccessValidator validator) {
            this.methodAccessValidator = Objects.requireNonNull(validator, "validator");
            return this;
        }

        /**
         * Sets the validator for template names.
         *
         * @param validator the validator
         * @return this builder
         */
        public Builder templateNameValidator(Predicate<String> validator) {
            this.templateNameValidator = Objects.requireNonNull(validator, "validator");
            return this;
        }

        /**
         * Sets whether public field access is denied.
         *
         * @param denyPublicFields true to deny public fields
         * @return this builder
         */
        public Builder denyPublicFields(boolean denyPublicFields) {
            this.denyPublicFields = denyPublicFields;
            return this;
        }

        /**
         * Sets whether the policy forces auto-escaping.
         *
         * @param forceAutoEscaping true to force auto-escaping
         * @return this builder
         */
        public Builder forceAutoEscaping(boolean forceAutoEscaping) {
            this.forceAutoEscaping = forceAutoEscaping;
            return this;
        }

        /**
         * Builds the policy.
         *
         * @return the policy
         */
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
