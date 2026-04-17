package io.github.yamatrireddy.api;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable configuration for {@link RSQLParser}.
 *
 * <p>Create an instance via {@link #defaults()} or customise using the {@link Builder}:
 * <pre>{@code
 * ParserConfig config = ParserConfig.builder()
 *         .maxDepth(5)
 *         .maxValueLength(64)
 *         .maxWildcardsPerValue(1)
 *         .maxInputLength(1024)
 *         .allowedOperators(Set.of("==", "!=", "=in=", "=out="))
 *         .build();
 * }</pre>
 */
public final class ParserConfig {

    /** Default allowed comparison operators (FIQL + common extensions). */
    public static final Set<String> DEFAULT_OPERATORS =
            Set.of("==", "!=", ">", ">=", "<", "<=", "=in=", "=out=");

    private final int maxDepth;
    private final int maxValueLength;
    private final int maxWildcardsPerValue;
    private final int maxInputLength;
    private final Set<String> allowedOperators;

    private ParserConfig(Builder b) {
        this.maxDepth            = b.maxDepth;
        this.maxValueLength      = b.maxValueLength;
        this.maxWildcardsPerValue = b.maxWildcardsPerValue;
        this.maxInputLength      = b.maxInputLength;
        this.allowedOperators    = Collections.unmodifiableSet(Set.copyOf(b.allowedOperators));
    }

    /** Returns a config with safe default limits. */
    public static ParserConfig defaults() {
        return new Builder().build();
    }

    /** Returns a new {@link Builder} pre-populated with default values. */
    public static Builder builder() {
        return new Builder();
    }

    /** Maximum nesting depth of logical operators. Default: {@code 10}. */
    public int getMaxDepth() {
        return maxDepth;
    }

    /** Maximum character length of a single comparison value. Default: {@code 128}. */
    public int getMaxValueLength() {
        return maxValueLength;
    }

    /** Maximum number of {@code *} wildcards allowed in a single value. Default: {@code 2}. */
    public int getMaxWildcardsPerValue() {
        return maxWildcardsPerValue;
    }

    /** Maximum character length of the entire input string. Default: {@code 4096}. */
    public int getMaxInputLength() {
        return maxInputLength;
    }

    /** The set of comparison operator strings that are permitted. */
    public Set<String> getAllowedOperators() {
        return allowedOperators;
    }

    @Override
    public String toString() {
        return "ParserConfig{maxDepth=" + maxDepth
                + ", maxValueLength=" + maxValueLength
                + ", maxWildcardsPerValue=" + maxWildcardsPerValue
                + ", maxInputLength=" + maxInputLength
                + ", allowedOperators=" + allowedOperators + '}';
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static final class Builder {

        private int maxDepth            = 10;
        private int maxValueLength      = 128;
        private int maxWildcardsPerValue = 2;
        private int maxInputLength      = 4096;
        private Set<String> allowedOperators = DEFAULT_OPERATORS;

        private Builder() {}

        public Builder maxDepth(int maxDepth) {
            if (maxDepth < 1) throw new IllegalArgumentException("maxDepth must be >= 1");
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder maxValueLength(int maxValueLength) {
            if (maxValueLength < 1) throw new IllegalArgumentException("maxValueLength must be >= 1");
            this.maxValueLength = maxValueLength;
            return this;
        }

        public Builder maxWildcardsPerValue(int maxWildcardsPerValue) {
            if (maxWildcardsPerValue < 0) throw new IllegalArgumentException("maxWildcardsPerValue must be >= 0");
            this.maxWildcardsPerValue = maxWildcardsPerValue;
            return this;
        }

        public Builder maxInputLength(int maxInputLength) {
            if (maxInputLength < 1) throw new IllegalArgumentException("maxInputLength must be >= 1");
            this.maxInputLength = maxInputLength;
            return this;
        }

        /** Replaces the default operator set. Must contain at least one operator. */
        public Builder allowedOperators(Set<String> allowedOperators) {
            Objects.requireNonNull(allowedOperators, "allowedOperators must not be null");
            if (allowedOperators.isEmpty()) throw new IllegalArgumentException("allowedOperators must not be empty");
            this.allowedOperators = allowedOperators;
            return this;
        }

        public ParserConfig build() {
            return new ParserConfig(this);
        }
    }
}
