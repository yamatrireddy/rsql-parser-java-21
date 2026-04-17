package io.github.yamatrireddy.api;

import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.AstVisitor;

import java.util.Objects;

/**
 * The result of a successful {@link RSQLParser#parse(String)} call.
 *
 * <p>Wraps the root {@link AstNode} of the optimised AST and provides a
 * convenience {@link #accept(AstVisitor)} method so callers do not need to
 * unbox the node manually:
 *
 * <pre>{@code
 * RSQLExpression expr = parser.parse("name==Alice;age>=18");
 * String sql = expr.accept(new SqlWhereVisitor());
 * }</pre>
 */
public final class RSQLExpression {

    private final AstNode ast;

    public RSQLExpression(AstNode ast) {
        this.ast = Objects.requireNonNull(ast, "ast must not be null");
    }

    /** Returns the root AST node of the parsed expression. */
    public AstNode getAst() {
        return ast;
    }

    /**
     * Shorthand for {@code getAst().accept(visitor)}.
     *
     * @param <T>     return type of the visitor
     * @param visitor visitor implementation
     * @return the value produced by the visitor
     */
    public <T> T accept(AstVisitor<T> visitor) {
        return ast.accept(visitor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RSQLExpression e)) return false;
        return ast.equals(e.ast);
    }

    @Override
    public int hashCode() {
        return ast.hashCode();
    }

    @Override
    public String toString() {
        return "RSQLExpression{ast=" + ast + '}';
    }
}
