package io.github.allbegray.pebbleforge.sandbox;

/**
 * Resource limits that can vary by template.
 */
public record SandboxLimits(int maxLoopIterations, int maxTotalIterations, int maxIncludeDepth,
                            int maxSlotChars) {

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

    public static SandboxLimits from(SandboxPolicy policy) {
        return new SandboxLimits(
                policy.maxLoopIterations(),
                policy.maxTotalIterations(),
                policy.maxIncludeDepth(),
                policy.maxSlotChars());
    }

    public static SandboxLimits unlimited() {
        return new SandboxLimits(-1, -1, -1, -1);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int maxLoopIterations = -1;
        private int maxTotalIterations = -1;
        private int maxIncludeDepth = -1;
        private int maxSlotChars = -1;

        public Builder maxLoopIterations(int maxLoopIterations) {
            this.maxLoopIterations = maxLoopIterations;
            return this;
        }

        public Builder maxTotalIterations(int maxTotalIterations) {
            this.maxTotalIterations = maxTotalIterations;
            return this;
        }

        public Builder maxIncludeDepth(int maxIncludeDepth) {
            this.maxIncludeDepth = maxIncludeDepth;
            return this;
        }

        public Builder maxSlotChars(int maxSlotChars) {
            this.maxSlotChars = maxSlotChars;
            return this;
        }

        public SandboxLimits build() {
            return new SandboxLimits(maxLoopIterations, maxTotalIterations, maxIncludeDepth,
                    maxSlotChars);
        }
    }
}
