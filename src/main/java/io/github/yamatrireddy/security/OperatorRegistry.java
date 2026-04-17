package io.github.yamatrireddy.security;

import io.github.yamatrireddy.exception.RSQLValidationException;

import java.util.Set;

/**
 * Utility class for operator validation.
 *
 * <p>Operator allow-listing is now managed through {@link io.github.yamatrireddy.api.ParserConfig};
 * this class is retained as a convenience for code that only needs a one-off check
 * against the default operator set.
 *
 * @see io.github.yamatrireddy.api.ParserConfig#getAllowedOperators()
 */
public final class OperatorRegistry {

    /** The default set of permitted comparison operators. */
    public static final Set<String> DEFAULT_OPERATORS =
            Set.of("==", "!=", ">", ">=", "<", "<=", "=in=", "=out=");

    private OperatorRegistry() {}

    /**
     * Validates that {@code op} is in the default operator set.
     *
     * @param op the operator string to check
     * @throws RSQLValidationException if the operator is not permitted
     */
    public static void validate(String op) {
        if (!DEFAULT_OPERATORS.contains(op)) {
            throw new RSQLValidationException(
                    "Operator '" + op + "' is not permitted. Allowed: " + DEFAULT_OPERATORS);
        }
    }

    /**
     * Validates that {@code op} is in the supplied {@code allowedOperators} set.
     *
     * @param op               the operator string to check
     * @param allowedOperators the custom set to check against
     * @throws RSQLValidationException if the operator is not in the set
     */
    public static void validate(String op, Set<String> allowedOperators) {
        if (!allowedOperators.contains(op)) {
            throw new RSQLValidationException(
                    "Operator '" + op + "' is not permitted. Allowed: " + allowedOperators);
        }
    }
}
