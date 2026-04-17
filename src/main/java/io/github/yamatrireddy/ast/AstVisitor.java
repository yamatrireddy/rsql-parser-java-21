package io.github.yamatrireddy.ast;

/**
 * Visitor interface for traversing an RSQL AST without modifying it.
 *
 * <p>Implement this interface to translate, evaluate, or pretty-print an AST.
 * The sealed {@link AstNode} hierarchy guarantees exhaustive dispatch — if a
 * new node type is ever added it will require a corresponding visit method here.
 *
 * <p>Example — convert to a SQL WHERE clause:
 * <pre>{@code
 * public class SqlVisitor implements AstVisitor<String> {
 *     public String visitComparison(ComparisonNode n) {
 *         return n.getField() + " " + toSql(n.getOperator()) + " ?";
 *     }
 *     public String visitLogical(LogicalNode n) {
 *         String sep = n.getOperator().equals("AND") ? " AND " : " OR ";
 *         return n.getChildren().stream()
 *                 .map(c -> c.accept(this))
 *                 .collect(Collectors.joining(sep, "(", ")"));
 *     }
 * }
 * }</pre>
 *
 * @param <T> the return type produced by each visit method
 */
public interface AstVisitor<T> {

    /**
     * Visit a leaf comparison node (e.g., {@code name==Alice}).
     *
     * @param node the comparison node
     * @return visitor result
     */
    T visitComparison(ComparisonNode node);

    /**
     * Visit a logical node (AND or OR) containing two or more children.
     *
     * @param node the logical node
     * @return visitor result
     */
    T visitLogical(LogicalNode node);
}
