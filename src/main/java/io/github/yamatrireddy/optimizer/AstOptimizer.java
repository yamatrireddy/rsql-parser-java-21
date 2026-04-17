package io.github.yamatrireddy.optimizer;

import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.LogicalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Structural optimiser that simplifies the AST without changing its semantics.
 *
 * <p>Two transformations are applied recursively:
 * <ol>
 *   <li><b>Flattening</b> — nested {@link LogicalNode}s with the same operator are
 *       merged into a single node:
 *       {@code (a AND (b AND c))} → {@code (a AND b AND c)}.</li>
 *   <li><b>Unwrapping</b> — a {@link LogicalNode} that ends up with only one child
 *       after flattening is replaced by that child directly.</li>
 * </ol>
 *
 * <p>All operations produce new node instances; input nodes are never mutated.
 */
public final class AstOptimizer {

    /**
     * Returns an optimised copy of {@code node}.  The original tree is unchanged.
     *
     * @param node the AST node to optimise (may be a leaf or a subtree root)
     * @return an equivalent, possibly simplified, AST node
     */
    public AstNode optimize(AstNode node) {
        if (!(node instanceof LogicalNode l)) {
            return node;    // leaf ComparisonNodes are already optimal
        }

        List<AstNode> flattened = new ArrayList<>();
        for (AstNode child : l.getChildren()) {
            AstNode optimisedChild = optimize(child);
            // Inline same-operator children (flattening)
            if (optimisedChild instanceof LogicalNode ol
                    && ol.getOperator().equals(l.getOperator())) {
                flattened.addAll(ol.getChildren());
            } else {
                flattened.add(optimisedChild);
            }
        }

        // Unwrap single-element logical nodes
        if (flattened.size() == 1) {
            return flattened.get(0);    // Java 17 compatible — no getFirst()
        }

        return new LogicalNode(l.getOperator(), flattened);
    }
}
