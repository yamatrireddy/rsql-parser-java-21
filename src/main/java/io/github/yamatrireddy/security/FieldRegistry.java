package io.github.yamatrireddy.security;

import io.github.yamatrireddy.exception.RSQLValidationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strategy interface for validating field names found in an RSQL expression.
 *
 * <p>Implement this interface to enforce an allowlist, delegate to a schema
 * registry, or apply any other field-level access-control policy.
 *
 * <p>Several built-in factory methods cover the most common use cases:
 * <pre>{@code
 * // Permit any field (useful in tests or trusted environments)
 * FieldRegistry any = FieldRegistry.allowAll();
 *
 * // Restrict to a fixed set of field names
 * FieldRegistry safe = FieldRegistry.allowList("name", "age", "email");
 *
 * // Allow dot-notation paths like "address.city"
 * FieldRegistry paths = FieldRegistry.allowPattern(Pattern.compile("[a-z]+(\\.[a-z]+)*"));
 * }</pre>
 */
@FunctionalInterface
public interface FieldRegistry {

    /**
     * Validates the given field name.
     *
     * @param field the field name from the RSQL expression (never null)
     * @throws RSQLValidationException if the field is not permitted
     */
    void validate(String field);

    // ── Factory methods ───────────────────────────────────────────────────────

    /** Accepts every field without restriction. */
    static FieldRegistry allowAll() {
        return field -> {};
    }

    /** Rejects every field. Useful as a safe default that must be overridden. */
    static FieldRegistry denyAll() {
        return field -> {
            throw new RSQLValidationException("No fields are permitted in this context");
        };
    }

    /**
     * Accepts only the explicitly listed field names (case-sensitive).
     *
     * @param fields one or more permitted field names
     * @throws IllegalArgumentException if {@code fields} is empty
     */
    static FieldRegistry allowList(String... fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        if (fields.length == 0) throw new IllegalArgumentException("fields must not be empty");
        Set<String> allowed = Set.copyOf(new HashSet<>(Arrays.asList(fields)));
        return field -> {
            if (!allowed.contains(field)) {
                throw new RSQLValidationException("Field not permitted: '" + field + "'");
            }
        };
    }

    /**
     * Accepts field names that fully match the given {@link Pattern}.
     *
     * @param pattern regex pattern that the whole field name must match
     */
    static FieldRegistry allowPattern(Pattern pattern) {
        Objects.requireNonNull(pattern, "pattern must not be null");
        return field -> {
            if (!pattern.matcher(field).matches()) {
                throw new RSQLValidationException(
                        "Field '" + field + "' does not match allowed pattern: " + pattern.pattern());
            }
        };
    }

    /**
     * Combines this registry with another using AND logic — both must pass.
     *
     * @param other secondary registry
     * @return a combined registry
     */
    default FieldRegistry and(FieldRegistry other) {
        Objects.requireNonNull(other, "other must not be null");
        return field -> {
            this.validate(field);
            other.validate(field);
        };
    }
}
