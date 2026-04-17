package io.github.yamatrireddy.ast;

/**
 * Root of the RSQL Abstract Syntax Tree.
 *
 * <p>The hierarchy is sealed so that all node types are known at compile time,
 * enabling exhaustive {@code switch} expressions and pattern-matching in Java 17+.
 * Only {@link ComparisonNode} and {@link LogicalNode} are permitted subclasses.
 */
public sealed abstract class AstNode permits ComparisonNode, LogicalNode {

    /**
     * Accept a visitor and return its result.
     *
     * @param <T>     return type of the visitor
     * @param visitor the visitor implementation
     * @return the value produced by the visitor
     */
    public abstract <T> T accept(AstVisitor<T> visitor);
}
