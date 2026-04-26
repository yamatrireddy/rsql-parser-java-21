package examples;

import io.github.yamatrireddy.api.RSQLExpression;
import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.security.FieldRegistry;
import io.github.yamatrireddy.visitor.AstPrinter;

/**
 * Example 01 — Basic Usage
 *
 * Demonstrates the minimal steps to parse an RSQL/FIQL expression and
 * inspect the resulting Abstract Syntax Tree.
 *
 * Run: mvn exec:java -Dexec.mainClass=examples.BasicUsageExample
 */
public class BasicUsageExample {

    public static void main(String[] args) {
        // ── 1. Create a parser (allow all field names, default security limits) ─
        RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());

        // ── 2. Parse simple equality ─────────────────────────────────────────
        RSQLExpression expr = parser.parse("name==Alice");
        System.out.println("Expression : name==Alice");
        System.out.println("Printed    : " + expr.accept(new AstPrinter()));
        System.out.println();

        // ── 3. Parse AND expression ──────────────────────────────────────────
        RSQLExpression andExpr = parser.parse("name==Alice;age>=18");
        System.out.println("Expression : name==Alice;age>=18");
        System.out.println("Printed    : " + andExpr.accept(new AstPrinter()));
        System.out.println();

        // ── 4. Parse OR expression ───────────────────────────────────────────
        RSQLExpression orExpr = parser.parse("status==active,status==pending");
        System.out.println("Expression : status==active,status==pending");
        System.out.println("Printed    : " + orExpr.accept(new AstPrinter()));
        System.out.println();

        // ── 5. Parse IN list ─────────────────────────────────────────────────
        RSQLExpression inExpr = parser.parse("role=in=(admin,user,guest)");
        System.out.println("Expression : role=in=(admin,user,guest)");
        System.out.println("Printed    : " + inExpr.accept(new AstPrinter()));
        System.out.println();

        // ── 6. Parse grouped expression ──────────────────────────────────────
        RSQLExpression grouped = parser.parse("(a==1,b==2);c==3");
        System.out.println("Expression : (a==1,b==2);c==3");
        System.out.println("Printed    : " + grouped.accept(new AstPrinter()));
        System.out.println();

        // ── 7. Inspect AST nodes directly ───────────────────────────────────
        RSQLExpression complex = parser.parse("name==Alice;age>=18");
        AstNode root = complex.getAst();

        if (root instanceof LogicalNode l) {
            System.out.println("Root is LogicalNode: " + l.getOperator());
            System.out.println("Children count     : " + l.getChildren().size());

            l.getChildren().forEach(child -> {
                if (child instanceof ComparisonNode c) {
                    System.out.printf("  -> ComparisonNode: field=%s, op=%s, values=%s%n",
                            c.getField(), c.getOperator(), c.getValues());
                }
            });
        }

        // ── 8. All supported comparison operators ────────────────────────────
        System.out.println();
        System.out.println("=== All comparison operators ===");
        String[] operators = {
                "field==value",
                "field!=value",
                "age>18",
                "age>=18",
                "age<65",
                "age<=65",
                "role=in=(admin,user)",
                "role=out=(banned,suspended)"
        };
        for (String op : operators) {
            System.out.printf("  %-30s -> %s%n", op, parser.parse(op).accept(new AstPrinter()));
        }

        // ── 9. Quoted values ─────────────────────────────────────────────────
        System.out.println();
        System.out.println("=== Quoted values ===");
        RSQLExpression quoted = parser.parse("name==\"John Doe\"");
        System.out.println("  name==\"John Doe\" -> " + quoted.accept(new AstPrinter()));

        // ── 10. Wildcard values ──────────────────────────────────────────────
        RSQLExpression wildcard = parser.parse("name==Alice*");
        System.out.println("  name==Alice*     -> " + wildcard.accept(new AstPrinter()));
    }
}
