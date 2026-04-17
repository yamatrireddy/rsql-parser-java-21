package io.github.yamatrireddy;

import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.optimizer.AstOptimizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AstOptimizerTest {

    private final AstOptimizer optimizer = new AstOptimizer();

    private static ComparisonNode cmp(String field) {
        return new ComparisonNode(field, "==", List.of("v"));
    }

    @Test
    void leafNodeIsReturnedUnchanged() {
        ComparisonNode leaf = cmp("name");
        assertThat(optimizer.optimize(leaf)).isSameAs(leaf);
    }

    @Test
    void nestedAndIsFlattened() {
        // AND(a, AND(b, c))  →  AND(a, b, c)
        LogicalNode inner = new LogicalNode("AND", List.of(cmp("b"), cmp("c")));
        LogicalNode outer = new LogicalNode("AND", List.of(cmp("a"), inner));

        AstNode result = optimizer.optimize(outer);

        assertThat(result).isInstanceOf(LogicalNode.class);
        LogicalNode flat = (LogicalNode) result;
        assertThat(flat.getOperator()).isEqualTo("AND");
        assertThat(flat.getChildren()).hasSize(3);
    }

    @Test
    void nestedOrIsFlattened() {
        LogicalNode inner = new LogicalNode("OR", List.of(cmp("b"), cmp("c")));
        LogicalNode outer = new LogicalNode("OR", List.of(cmp("a"), inner));

        AstNode result = optimizer.optimize(outer);

        LogicalNode flat = (LogicalNode) result;
        assertThat(flat.getOperator()).isEqualTo("OR");
        assertThat(flat.getChildren()).hasSize(3);
    }

    @Test
    void mixedOperatorsAreNotFlattened() {
        // OR(a, AND(b, c))  —  should stay as is
        LogicalNode and = new LogicalNode("AND", List.of(cmp("b"), cmp("c")));
        LogicalNode or  = new LogicalNode("OR",  List.of(cmp("a"), and));

        AstNode result = optimizer.optimize(or);

        LogicalNode root = (LogicalNode) result;
        assertThat(root.getOperator()).isEqualTo("OR");
        assertThat(root.getChildren()).hasSize(2);
        assertThat(root.getChildren().get(1)).isInstanceOf(LogicalNode.class);
    }

    @Test
    void inputNodeIsNotMutated() {
        // Verify the optimiser creates new objects instead of mutating
        LogicalNode inner = new LogicalNode("AND", List.of(cmp("b"), cmp("c")));
        LogicalNode outer = new LogicalNode("AND", List.of(cmp("a"), inner));

        optimizer.optimize(outer);

        // Original nodes must be unchanged
        assertThat(outer.getChildren()).hasSize(2);
        assertThat(inner.getChildren()).hasSize(2);
    }

    @Test
    void deeplyNestedSameOperatorIsFullyFlattened() {
        // AND(AND(AND(a, b), c), d)  →  AND(a, b, c, d)
        LogicalNode l1 = new LogicalNode("AND", List.of(cmp("a"), cmp("b")));
        LogicalNode l2 = new LogicalNode("AND", List.of(l1, cmp("c")));
        LogicalNode l3 = new LogicalNode("AND", List.of(l2, cmp("d")));

        AstNode result = optimizer.optimize(l3);

        LogicalNode flat = (LogicalNode) result;
        assertThat(flat.getChildren()).hasSize(4);
    }
}
