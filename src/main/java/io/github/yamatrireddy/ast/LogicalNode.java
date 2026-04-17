package io.github.yamatrireddy.ast;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An interior AST node combining two or more child nodes with a logical operator.
 *
 * <p>The {@link #getOperator() operator} is either {@code "AND"} or {@code "OR"}.
 * After optimizer flattening, nested same-operator nodes are collapsed so that
 * {@code a AND (b AND c)} becomes a single {@code LogicalNode("AND", [a, b, c])}.
 *
 * <p>Instances are immutable. The {@link #getChildren() children} list is
 * unmodifiable and always contains at least two elements post-optimisation.
 */
public final class LogicalNode extends AstNode {

    private final String operator;
    private final List<AstNode> children;

    /**
     * Creates a new LogicalNode.
     *
     * @param operator {@code "AND"} or {@code "OR"} (non-null)
     * @param children two or more child nodes (non-null, size &ge; 2)
     */
    public LogicalNode(String operator, List<AstNode> children) {
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(children, "children must not be null");
        if (children.size() < 2) {
            throw new IllegalArgumentException("LogicalNode requires at least 2 children");
        }
        this.operator = operator;
        this.children = Collections.unmodifiableList(List.copyOf(children));
    }

    /** {@code "AND"} or {@code "OR"}. */
    public String getOperator() {
        return operator;
    }

    /** The child {@link AstNode}s joined by this operator. */
    public List<AstNode> getChildren() {
        return children;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitLogical(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogicalNode l)) return false;
        return operator.equals(l.operator) && children.equals(l.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, children);
    }

    @Override
    public String toString() {
        return "LogicalNode{operator='" + operator + "', children=" + children + '}';
    }
}
