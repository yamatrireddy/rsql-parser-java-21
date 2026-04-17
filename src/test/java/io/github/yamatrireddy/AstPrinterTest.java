package io.github.yamatrireddy;

import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.security.FieldRegistry;
import io.github.yamatrireddy.visitor.AstPrinter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AstPrinterTest {

    private final AstPrinter printer = new AstPrinter();
    private final RSQLParser parser  = new RSQLParser(FieldRegistry.allowAll());

    @Test
    void singleComparisonIsPrinted() {
        ComparisonNode node = new ComparisonNode("name", "==", List.of("Alice"));
        assertThat(printer.visitComparison(node)).isEqualTo("name==Alice");
    }

    @Test
    void multiValueComparisonUsesBrackets() {
        ComparisonNode node = new ComparisonNode("role", "=in=", List.of("admin", "user"));
        assertThat(printer.visitComparison(node)).isEqualTo("role=in=(admin,user)");
    }

    @Test
    void andNodeHasAndSeparator() {
        LogicalNode and = new LogicalNode("AND", List.of(
                new ComparisonNode("a", "==", List.of("1")),
                new ComparisonNode("b", "==", List.of("2"))));
        assertThat(printer.visitLogical(and)).isEqualTo("(a==1 AND b==2)");
    }

    @Test
    void orNodeHasOrSeparator() {
        LogicalNode or = new LogicalNode("OR", List.of(
                new ComparisonNode("a", "==", List.of("1")),
                new ComparisonNode("b", "==", List.of("2"))));
        assertThat(printer.visitLogical(or)).isEqualTo("(a==1 OR b==2)");
    }

    @Test
    void nestedLogicalNodeIsPrintedRecursively() {
        String printed = parser.parse("a==1;b==2,c==3").accept(printer);
        // (a==1 AND b==2) OR c==3  — after flattening the structure depends on precedence
        assertThat(printed).contains("AND").contains("OR");
    }

    @Test
    void acceptHelperOnExpressionWorks() {
        String result = parser.parse("name==Alice").accept(printer);
        assertThat(result).isEqualTo("name==Alice");
    }
}
