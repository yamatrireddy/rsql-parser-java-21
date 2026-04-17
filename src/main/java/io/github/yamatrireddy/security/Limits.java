package io.github.yamatrireddy.security;

/**
 * Named constants for the default security limits used by {@link io.github.yamatrireddy.api.ParserConfig}.
 *
 * <p>These values are provided for documentation and reuse in custom
 * {@link io.github.yamatrireddy.api.ParserConfig} builders. The authoritative
 * source of truth at runtime is the {@code ParserConfig} instance passed to
 * {@link io.github.yamatrireddy.api.RSQLParser}.
 */
public final class Limits {

    /** Default maximum nesting depth of logical operators. */
    public static final int MAX_DEPTH = 10;

    /** Default maximum character length of a single comparison value. */
    public static final int MAX_VALUE_LENGTH = 128;

    /** Default maximum number of {@code *} wildcards per value. */
    public static final int MAX_WILDCARDS = 2;

    /** Default maximum character length of the full RSQL input string. */
    public static final int MAX_INPUT_LENGTH = 4096;

    private Limits() {}
}
