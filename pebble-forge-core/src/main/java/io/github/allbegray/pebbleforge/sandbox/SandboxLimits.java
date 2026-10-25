package io.github.allbegray.pebbleforge.sandbox;

/**
 * Resource limits that can vary by template.
 *
 * @param maxLoopIterations the maximum iterations of one loop, or -1 for no limit
 * @param maxTotalIterations the maximum iterations across all nested loops, or -1 for no limit
 * @param maxIncludeDepth the maximum nested include depth, or -1 for no limit
 * @param maxSlotChars the maximum slot content length, or -1 for no limit
 */
public record SandboxLimits(int maxLoopIterations, int maxTotalIterations, int maxIncludeDepth,
                            int maxSlotChars) {

    /**
     * Validates the limits.
     *
     * @throws IllegalArgumentException if a limit is less than -1
     */
    public SandboxLimits {
        if (maxLoopIterations < -1) {
            throw new IllegalArgumentException("maxLoopIterations must be -1 or greater");
        }
        if (maxTotalIterations < -1) {
            throw new IllegalArgumentException("maxTotalIterations must be -1 or greater");
        }
        if (maxIncludeDepth < -1) {
            throw new IllegalArgumentException("maxIncludeDepth must be -1 or greater");
        }
        if (maxSlotChars < -1) {
            throw new IllegalArgumentException("maxSlotChars must be -1 or greater");
        }
    }

    /**
     * Copies the limits from a policy.
     *
     * @param policy the policy to read
     * @return the limits
     */
    public static SandboxLimits from(SandboxPolicy policy) {
        return new SandboxLimits(
                policy.maxLoopIterations(),
                policy.maxTotalIterations(),
                policy.maxIncludeDepth(),
                policy.maxSlotChars());
    }

    /**
     * Returns limits with no caps.
     *
     * @return unlimited limits
     */
    public static SandboxLimits unlimited() {
        return new SandboxLimits(-1, -1, -1, -1);
    }

    /**
     * Creates a limits builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link SandboxLimits} value.
     */
    public static final class Builder {

        /**
         * Creates a builder with no limits. Prefer {@link SandboxLimits#builder()}.
         */
        public Builder() {
        }

        private int maxLoopIterations = -1;
        private int maxTotalIterations = -1;
        private int maxIncludeDepth = -1;
        private int maxSlotChars = -1;

        /**
         * Sets the maximum iterations of one loop.
         *
         * @param maxLoopIterations the limit, or -1 for no limit
         * @return this builder
         */
        public Builder maxLoopIterations(int maxLoopIterations) {
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
            this.maxIncludeDepth = maxIncludeDepth;
            return this;
        }

        /**
         * Sets the maximum slot content length.
         *
         * @param maxSlotChars the limit, or -1 for no limit
         * @return this builder
         */
        public Builder maxSlotChars(int maxSlotChars) {
            this.maxSlotChars = maxSlotChars;
            return this;
        }

        /**
         * Builds the limits.
         *
         * @return the limits
         */
        public SandboxLimits build() {
            return new SandboxLimits(maxLoopIterations, maxTotalIterations, maxIncludeDepth,
                    maxSlotChars);
        }
    }
}
