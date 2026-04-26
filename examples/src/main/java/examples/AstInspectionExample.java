package examples;

import io.github.yamatrireddy.api.RSQLExpression;
import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.AstVisitor;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.security.FieldRegistry;
import io.github.yamatrireddy.visitor.AstPrinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Example 07 — AST Inspection
 *
 * Demonstrates how to traverse and inspect the parsed Abstract Syntax Tree:
 *   - Pattern matching on sealed node types
 *   - Tree walking with a custom recursive visitor
 *   - Extracting all referenced fields
 *   - Counting nodes and measuring depth
 *   - Building a tree pretty-printer
 *
 * Run: mvn exec:java -Dexec.mainClass=examples.AstInspectionExample
 */
public class AstInspectionExample {

    public static void main(String[] args) {
        RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());

        String rsql = "name==Alice*;(age>=18,age<=65);role=in=(admin,user);status==active";
        RSQLExpression expr = parser.parse(rsql);

        System.out.println("Input : " + rsql);
        System.out.println("Canonical: " + expr.accept(new AstPrinter()));
        System.out.println();

        // ── 1. Pattern matching with sealed hierarchy ─────────────────────────
        System.out.println("=== 1. Pattern matching (Java 17+ switch) ===");
        printNode(expr.getAst(), 0);

        // ── 2. Extract all field names ─────────────────────────────────────
        System.out.println("\n=== 2. All referenced fields ===");
        List<String> fields = expr.accept(new FieldExtractor());
        System.out.println("  Fields: " + fields);

        // ── 3. Measure depth ─────────────────────────────────────────────────
        System.out.println("\n=== 3. AST depth ===");
        int depth = expr.accept(new DepthCounter());
        System.out.println("  Max depth: " + depth);

        // ── 4. Count nodes ────────────────────────────────────────────────────
        System.out.println("\n=== 4. Node count ===");
        int[] counts = {0, 0};
        countNodes(expr.getAst(), counts);
        System.out.println("  ComparisonNodes: " + counts[0]);
        System.out.println("  LogicalNodes   : " + counts[1]);

        // ── 5. Tree pretty-printer ────────────────────────────────────────────
        System.out.println("\n=== 5. Tree structure ===");
        System.out.println(expr.accept(new TreePrinter()));
    }

    // ── Pattern matching tree walk ────────────────────────────────────────────

    static void printNode(AstNode node, int indent) {
        String pad = "  ".repeat(indent);
        switch (node) {
            case ComparisonNode c -> System.out.printf(
                    "%sComparison: %s %s %s%n",
                    pad, c.getField(), c.getOperator(), c.getValues());
            case LogicalNode l -> {
                System.out.printf("%sLogical: %s (%d children)%n",
                        pad, l.getOperator(), l.getChildren().size());
                l.getChildren().forEach(child -> printNode(child, indent + 1));
            }
        }
    }

    static void countNodes(AstNode node, int[] counts) {
        switch (node) {
            case ComparisonNode ignored -> counts[0]++;
            case LogicalNode l -> {
                counts[1]++;
                l.getChildren().forEach(child -> countNodes(child, counts));
            }
        }
    }

    // ── Field extractor visitor ───────────────────────────────────────────────

    static class FieldExtractor implements AstVisitor<List<String>> {

        @Override
        public List<String> visitComparison(ComparisonNode node) {
            return List.of(node.getField());
        }

        @Override
        public List<String> visitLogical(LogicalNode node) {
            List<String> all = new ArrayList<>();
            node.getChildren().forEach(child -> all.addAll(child.accept(this)));
            return all;
        }
    }

    // ── Depth counter visitor ─────────────────────────────────────────────────

    static class DepthCounter implements AstVisitor<Integer> {

        @Override
        public Integer visitComparison(ComparisonNode node) {
            return 0;
        }

        @Override
        public Integer visitLogical(LogicalNode node) {
            return 1 + node.getChildren().stream()
                    .mapToInt(child -> child.accept(this))
                    .max()
                    .orElse(0);
        }
    }

    // ── Tree pretty-printer visitor ───────────────────────────────────────────

    static class TreePrinter implements AstVisitor<String> {

        @Override
        public String visitComparison(ComparisonNode node) {
            String values = node.getValues().size() == 1
                    ? node.getValues().get(0)
                    : node.getValues().toString();
            return "── " + node.getField() + " " + node.getOperator() + " " + values;
        }

        @Override
        public String visitLogical(LogicalNode node) {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(node.getOperator()).append("]\n");
            List<AstNode> children = node.getChildren();
            for (int i = 0; i < children.size(); i++) {
                boolean last = (i == children.size() - 1);
                String branch = last ? "└" : "├";
                String child  = children.get(i).accept(this);
                String indent = last ? "  " : "│ ";
                // Indent inner lines of multi-line children
                String indented = child.replace("\n", "\n" + indent);
                sb.append(branch).append(indented);
                if (!last) sb.append("\n");
            }
            return sb.toString();
        }
    }
}
