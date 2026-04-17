package io.github.yamatrireddy.ast;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A leaf AST node representing a single field comparison.
 *
 * <p>Examples: {@code name==Alice}, {@code age>=18}, {@code role=in=(admin,user)}.
 *
 * <p>Instances are immutable. The {@link #getValues() values} list is unmodifiable
 * and always contains at least one element.
 */
public final class ComparisonNode extends AstNode {

    private final String field;
    private final String operator;
    private final List<String> values;

    /**
     * Creates a new ComparisonNode.
     *
     * @param field    field name (non-null, non-empty)
     * @param operator comparison operator (non-null)
     * @param values   one or more comparison values (non-null, non-empty list)
     */
    public ComparisonNode(String field, String operator, List<String> values) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(values, "values must not be null");
        if (field.isBlank()) throw new IllegalArgumentException("field must not be blank");
        if (values.isEmpty()) throw new IllegalArgumentException("values must not be empty");

        this.field = field;
        this.operator = operator;
        this.values = Collections.unmodifiableList(List.copyOf(values));
    }

    /** The field name as it appears in the RSQL expression. */
    public String getField() {
        return field;
    }

    /** The operator string, e.g., {@code ==}, {@code !=}, {@code =in=}. */
    public String getOperator() {
        return operator;
    }

    /**
     * The comparison values. Single-value comparisons return a one-element list;
     * multi-value operators ({@code =in=}, {@code =out=}) may return more.
     */
    public List<String> getValues() {
        return values;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitComparison(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComparisonNode c)) return false;
        return field.equals(c.field)
                && operator.equals(c.operator)
                && values.equals(c.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, operator, values);
    }

    @Override
    public String toString() {
        return "ComparisonNode{field='" + field
                + "', operator='" + operator
                + "', values=" + values + '}';
    }
}
