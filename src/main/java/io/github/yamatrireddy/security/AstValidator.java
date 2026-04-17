package io.github.yamatrireddy.security;

import io.github.yamatrireddy.api.ParserConfig;
import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.exception.RSQLValidationException;

/**
 * Walks the AST and enforces all security constraints defined in a
 * {@link ParserConfig}.
 *
 * <p>Checks performed (in order) for every {@link ComparisonNode}:
 * <ol>
 *   <li>Nesting depth does not exceed {@link ParserConfig#getMaxDepth()}.</li>
 *   <li>Field name is accepted by the {@link FieldRegistry}.</li>
 *   <li>Operator is in {@link ParserConfig#getAllowedOperators()}.</li>
 *   <li>Each value length does not exceed {@link ParserConfig#getMaxValueLength()}.</li>
 *   <li>Wildcard ({@code *}) count per value does not exceed
 *       {@link ParserConfig#getMaxWildcardsPerValue()}.</li>
 * </ol>
 */
public final class AstValidator {

    private final FieldRegistry fieldRegistry;
    private final ParserConfig config;

    public AstValidator(FieldRegistry fieldRegistry, ParserConfig config) {
        this.fieldRegistry = fieldRegistry;
        this.config        = config;
    }

    /**
     * Validates the subtree rooted at {@code node}.
     *
     * @param node  the AST node to validate (and its children recursively)
     * @param depth current nesting depth (start with {@code 0})
     * @throws RSQLValidationException if any constraint is violated
     */
    public void validate(AstNode node, int depth) {
        if (depth > config.getMaxDepth()) {
            throw new RSQLValidationException(
                    "Query nesting depth " + depth
                    + " exceeds maximum of " + config.getMaxDepth());
        }

        if (node instanceof ComparisonNode c) {
            validateComparison(c);
        } else if (node instanceof LogicalNode l) {
            for (AstNode child : l.getChildren()) {
                validate(child, depth + 1);
            }
        }
    }

    private void validateComparison(ComparisonNode c) {
        fieldRegistry.validate(c.getField());

        if (!config.getAllowedOperators().contains(c.getOperator())) {
            throw new RSQLValidationException(
                    "Operator '" + c.getOperator() + "' is not permitted. "
                    + "Allowed: " + config.getAllowedOperators());
        }

        for (String value : c.getValues()) {
            if (value.length() > config.getMaxValueLength()) {
                throw new RSQLValidationException(
                        "Value length " + value.length()
                        + " exceeds maximum of " + config.getMaxValueLength());
            }
            long wildcards = value.chars().filter(ch -> ch == '*').count();
            if (wildcards > config.getMaxWildcardsPerValue()) {
                throw new RSQLValidationException(
                        "Value '" + value + "' contains " + wildcards
                        + " wildcards; maximum is " + config.getMaxWildcardsPerValue());
            }
        }
    }
}
