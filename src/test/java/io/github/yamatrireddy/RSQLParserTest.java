package io.github.yamatrireddy;

import io.github.yamatrireddy.api.ParserConfig;
import io.github.yamatrireddy.api.RSQLExpression;
import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.exception.RSQLParseException;
import io.github.yamatrireddy.exception.RSQLValidationException;
import io.github.yamatrireddy.security.FieldRegistry;
import io.github.yamatrireddy.visitor.AstPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RSQLParserTest {

    private RSQLParser parser;

    @BeforeEach
    void setup() {
        parser = new RSQLParser(FieldRegistry.allowAll());
    }

    // ── Simple comparisons ──────────────────────────────────────────────────

    @Test
    void simplEqualityComparison() {
        AstNode ast = parser.parse("name==Alice").getAst();
        assertThat(ast).isInstanceOf(ComparisonNode.class);
        ComparisonNode c = (ComparisonNode) ast;
        assertThat(c.getField()).isEqualTo("name");
        assertThat(c.getOperator()).isEqualTo("==");
        assertThat(c.getValues()).containsExactly("Alice");
    }

    @Test
    void notEqualComparison() {
        ComparisonNode c = (ComparisonNode) parser.parse("status!=active").getAst();
        assertThat(c.getOperator()).isEqualTo("!=");
    }

    @Test
    void greaterThanComparison() {
        ComparisonNode c = (ComparisonNode) parser.parse("age>18").getAst();
        assertThat(c.getOperator()).isEqualTo(">");
        assertThat(c.getValues()).containsExactly("18");
    }

    @Test
    void greaterThanOrEqualWithNamedOperator() {
        ComparisonNode c = (ComparisonNode) parser.parse("age=ge=18").getAst();
        assertThat(c.getOperator()).isEqualTo(">=");
    }

    @Test
    void lessThanOrEqualWithSymbolicOperator() {
        ComparisonNode c = (ComparisonNode) parser.parse("score<=100").getAst();
        assertThat(c.getOperator()).isEqualTo("<=");
    }

    // ── Multi-value operators ──────────────────────────────────────────────

    @Test
    void inOperatorWithMultipleValues() {
        ComparisonNode c = (ComparisonNode) parser.parse("role=in=(admin,user,guest)").getAst();
        assertThat(c.getOperator()).isEqualTo("=in=");
        assertThat(c.getValues()).containsExactly("admin", "user", "guest");
    }

    @Test
    void outOperatorWithTwoValues() {
        ComparisonNode c = (ComparisonNode) parser.parse("status=out=(deleted,archived)").getAst();
        assertThat(c.getOperator()).isEqualTo("=out=");
        assertThat(c.getValues()).containsExactlyInAnyOrder("deleted", "archived");
    }

    // ── Logical operators ──────────────────────────────────────────────────

    @Test
    void andWithSemicolon() {
        LogicalNode l = (LogicalNode) parser.parse("a==1;b==2").getAst();
        assertThat(l.getOperator()).isEqualTo("AND");
        assertThat(l.getChildren()).hasSize(2);
    }

    @Test
    void orWithComma() {
        LogicalNode l = (LogicalNode) parser.parse("a==1,b==2").getAst();
        assertThat(l.getOperator()).isEqualTo("OR");
    }

    @Test
    void andKeyword() {
        LogicalNode l = (LogicalNode) parser.parse("a==1 and b==2").getAst();
        assertThat(l.getOperator()).isEqualTo("AND");
    }

    @Test
    void orKeyword() {
        LogicalNode l = (LogicalNode) parser.parse("a==1 or b==2").getAst();
        assertThat(l.getOperator()).isEqualTo("OR");
    }

    @Test
    void andHasHigherPrecedenceThanOr() {
        // a==1,b==2;c==3  →  OR(a==1, AND(b==2,c==3))
        RSQLExpression expr = parser.parse("a==1,b==2;c==3");
        LogicalNode root = (LogicalNode) expr.getAst();
        assertThat(root.getOperator()).isEqualTo("OR");
        assertThat(root.getChildren().get(1)).isInstanceOf(LogicalNode.class);
        LogicalNode andNode = (LogicalNode) root.getChildren().get(1);
        assertThat(andNode.getOperator()).isEqualTo("AND");
    }

    // ── Grouping ───────────────────────────────────────────────────────────

    @Test
    void parenthesesGroupExpression() {
        RSQLExpression expr = parser.parse("(a==1,b==2);c==3");
        LogicalNode root = (LogicalNode) expr.getAst();
        assertThat(root.getOperator()).isEqualTo("AND");
        assertThat(root.getChildren().get(0)).isInstanceOf(LogicalNode.class);
    }

    @Test
    void deeplyNestedGrouping() {
        // Should not throw for valid nesting within limit
        assertThatNoException().isThrownBy(() ->
                parser.parse("((a==1;b==2),(c==3;d==4))"));
    }

    // ── Quoted values ──────────────────────────────────────────────────────

    @Test
    void doubleQuotedValueWithSpaces() {
        ComparisonNode c = (ComparisonNode) parser.parse("name==\"John Doe\"").getAst();
        assertThat(c.getValues()).containsExactly("John Doe");
    }

    @Test
    void singleQuotedValueWithEscapedQuote() {
        ComparisonNode c = (ComparisonNode) parser.parse("name=='O\\'Brien'").getAst();
        assertThat(c.getValues()).containsExactly("O'Brien");
    }

    // ── Optimiser ─────────────────────────────────────────────────────────

    @Test
    void nestedAndIsFlattened() {
        // (a==1;(b==2;c==3)) → AND(a, b, c)
        RSQLExpression expr = parser.parse("a==1;(b==2;c==3)");
        LogicalNode l = (LogicalNode) expr.getAst();
        assertThat(l.getChildren()).hasSize(3);
    }

    // ── Visitor ────────────────────────────────────────────────────────────

    @Test
    void astPrinterReconstructsExpression() {
        RSQLExpression expr = parser.parse("name==Alice;age>=18");
        String printed = expr.accept(new AstPrinter());
        assertThat(printed).isEqualTo("(name==Alice AND age>=18)");
    }

    // ── Field registry ─────────────────────────────────────────────────────

    @Nested
    class FieldRegistryTests {

        @Test
        void allowListPermitsExactNames() {
            RSQLParser strict = new RSQLParser(FieldRegistry.allowList("name", "age"));
            assertThatNoException().isThrownBy(() -> strict.parse("name==Alice;age>=18"));
        }

        @Test
        void allowListRejectsUnknownField() {
            RSQLParser strict = new RSQLParser(FieldRegistry.allowList("name"));
            assertThatThrownBy(() -> strict.parse("email==x@y.com"))
                    .isInstanceOf(RSQLValidationException.class)
                    .hasMessageContaining("email");
        }

        @Test
        void allowPatternAcceptsMatchingField() {
            RSQLParser regex = new RSQLParser(
                    FieldRegistry.allowPattern(java.util.regex.Pattern.compile("[a-z]+")));
            assertThatNoException().isThrownBy(() -> regex.parse("name==Alice"));
        }

        @Test
        void allowPatternRejectsNonMatchingField() {
            RSQLParser regex = new RSQLParser(
                    FieldRegistry.allowPattern(java.util.regex.Pattern.compile("[a-z]+")));
            assertThatThrownBy(() -> regex.parse("Name==Alice"))
                    .isInstanceOf(RSQLValidationException.class);
        }
    }

    // ── Security limits ────────────────────────────────────────────────────

    @Nested
    class SecurityLimitTests {

        @Test
        void inputExceedingMaxLengthIsRejected() {
            ParserConfig cfg = ParserConfig.builder().maxInputLength(10).build();
            RSQLParser p = new RSQLParser(FieldRegistry.allowAll(), cfg);
            assertThatThrownBy(() -> p.parse("name==VeryLongValue"))
                    .isInstanceOf(RSQLValidationException.class)
                    .hasMessageContaining("exceeds maximum");
        }

        @Test
        void valueLongerThanLimitIsRejected() {
            ParserConfig cfg = ParserConfig.builder().maxValueLength(3).build();
            RSQLParser p = new RSQLParser(FieldRegistry.allowAll(), cfg);
            assertThatThrownBy(() -> p.parse("name==Alice"))
                    .isInstanceOf(RSQLValidationException.class)
                    .hasMessageContaining("length");
        }

        @Test
        void tooManyWildcardsAreRejected() {
            ParserConfig cfg = ParserConfig.builder().maxWildcardsPerValue(1).build();
            RSQLParser p = new RSQLParser(FieldRegistry.allowAll(), cfg);
            assertThatThrownBy(() -> p.parse("name==*a*b*"))
                    .isInstanceOf(RSQLValidationException.class)
                    .hasMessageContaining("wildcard");
        }

        @Test
        void disallowedOperatorIsRejected() {
            ParserConfig cfg = ParserConfig.builder()
                    .allowedOperators(java.util.Set.of("==", "!="))
                    .build();
            RSQLParser p = new RSQLParser(FieldRegistry.allowAll(), cfg);
            assertThatThrownBy(() -> p.parse("age>18"))
                    .isInstanceOf(RSQLValidationException.class)
                    .hasMessageContaining(">");
        }

        @Test
        void excessiveDepthIsRejected() {
            ParserConfig cfg = ParserConfig.builder().maxDepth(1).build();
            RSQLParser p = new RSQLParser(FieldRegistry.allowAll(), cfg);
            // Three levels deep
            assertThatThrownBy(() -> p.parse("((a==1;b==2);c==3)"))
                    .isInstanceOf(RSQLValidationException.class)
                    .hasMessageContaining("depth");
        }
    }

    // ── Input validation ───────────────────────────────────────────────────

    @Test
    void nullInputThrowsNullPointerException() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankInputThrowsParseException() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(RSQLParseException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void syntaxErrorThrowsParseException() {
        assertThatThrownBy(() -> parser.parse("name=="))
                .isInstanceOf(RSQLParseException.class);
    }

    // ── Builder ────────────────────────────────────────────────────────────

    @Test
    void builderCreatesParserCorrectly() {
        RSQLParser built = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxDepth(5).build())
                .build();
        assertThatNoException().isThrownBy(() -> built.parse("name==Alice"));
    }

    @Test
    void builderWithoutFieldRegistryThrows() {
        assertThatThrownBy(() -> RSQLParser.builder().build())
                .isInstanceOf(NullPointerException.class);
    }
}
