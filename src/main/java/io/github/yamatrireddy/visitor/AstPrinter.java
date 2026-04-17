package io.github.yamatrireddy.visitor;

import io.github.yamatrireddy.ast.AstVisitor;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;

import java.util.stream.Collectors;

/**
 * A utility {@link AstVisitor} that reconstructs the RSQL expression from an AST.
 *
 * <p>The output is a canonical, human-readable string using the {@code AND}/{@code OR}
 * keyword form and explicit parentheses around every logical group. This is useful
 * for logging, debugging, and round-trip testing.
 *
 * <p>Example:
 * <pre>{@code
 * RSQLExpression expr = parser.parse("name==Alice;age>=18");
 * String printed = expr.accept(new AstPrinter());
 * // → "(name==Alice AND age>=18)"
 * }</pre>
 */
public final class AstPrinter implements AstVisitor<String> {

    @Override
    public String visitComparison(ComparisonNode node) {
        String valPart = node.getValues().size() == 1
                ? node.getValues().get(0)
                : "(" + String.join(",", node.getValues()) + ")";
        return node.getField() + node.getOperator() + valPart;
    }

    @Override
    public String visitLogical(LogicalNode node) {
        String separator = "AND".equals(node.getOperator()) ? " AND " : " OR ";
        return node.getChildren().stream()
                .map(child -> child.accept(this))
                .collect(Collectors.joining(separator, "(", ")"));
    }
}
