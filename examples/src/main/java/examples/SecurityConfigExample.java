package examples;

import io.github.yamatrireddy.api.ParserConfig;
import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.exception.RSQLValidationException;
import io.github.yamatrireddy.security.FieldRegistry;
import io.github.yamatrireddy.visitor.AstPrinter;

import java.util.Set;

/**
 * Example 03 — Security Configuration
 *
 * Demonstrates all ParserConfig limits and how to tune them for
 * different threat models (public API, internal service, admin console).
 *
 * Run: mvn exec:java -Dexec.mainClass=examples.SecurityConfigExample
 */
public class SecurityConfigExample {

    public static void main(String[] args) {

        // ── 1. Default limits ────────────────────────────────────────────────
        System.out.println("=== Default limits ===");
        System.out.println(ParserConfig.defaults());

        // ── 2. maxDepth — limit logical nesting ─────────────────────────────
        System.out.println("\n=== maxDepth(2) ===");
        RSQLParser shallowParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxDepth(2).build())
                .build();

        System.out.println("  a==1;b==2       -> " +
                shallowParser.parse("a==1;b==2").accept(new AstPrinter()));

        tryParse(shallowParser, "(a==1;b==2);(c==3;d==4)",
                "  depth-3 nesting   -> BLOCKED");

        // ── 3. maxValueLength — limit per-value size ─────────────────────────
        System.out.println("\n=== maxValueLength(10) ===");
        RSQLParser shortValueParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxValueLength(10).build())
                .build();

        System.out.println("  name==Alice     -> " +
                shortValueParser.parse("name==Alice").accept(new AstPrinter()));

        tryParse(shortValueParser, "name==VeryLongNameThatExceedsLimit",
                "  31-char value    -> BLOCKED");

        // ── 4. maxWildcardsPerValue — limit wildcard injection ────────────────
        System.out.println("\n=== maxWildcardsPerValue(1) ===");
        RSQLParser oneWildcardParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxWildcardsPerValue(1).build())
                .build();

        System.out.println("  name==Alice*    -> " +
                oneWildcardParser.parse("name==Alice*").accept(new AstPrinter()));

        tryParse(oneWildcardParser, "name==*Alice*",
                "  name==*Alice*    -> BLOCKED (2 wildcards)");

        // ── 5. maxInputLength — overall input length guard ───────────────────
        System.out.println("\n=== maxInputLength(50) ===");
        RSQLParser shortInputParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxInputLength(50).build())
                .build();

        System.out.println("  name==Alice     -> " +
                shortInputParser.parse("name==Alice").accept(new AstPrinter()));

        String longInput = "name==Alice;age>=18;role==admin;status==active;dept==eng";
        tryParse(shortInputParser, longInput,
                "  55-char input    -> BLOCKED");

        // ── 6. allowedOperators — restrict to safe subset ────────────────────
        System.out.println("\n=== allowedOperators(==, !=, =in=, =out=) ===");
        RSQLParser readOnlyParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder()
                        .allowedOperators(Set.of("==", "!=", "=in=", "=out="))
                        .build())
                .build();

        System.out.println("  name==Alice     -> " +
                readOnlyParser.parse("name==Alice").accept(new AstPrinter()));
        System.out.println("  role=in=(a,b)   -> " +
                readOnlyParser.parse("role=in=(a,b)").accept(new AstPrinter()));

        tryParse(readOnlyParser, "age>18",
                "  age>18           -> BLOCKED (> not in allowed set)");

        // ── 7. Production-grade config for a public REST API ─────────────────
        System.out.println("\n=== Production config (public API) ===");
        ParserConfig publicApiConfig = ParserConfig.builder()
                .maxDepth(4)
                .maxValueLength(64)
                .maxWildcardsPerValue(1)
                .maxInputLength(512)
                .allowedOperators(Set.of("==", "!=", ">", ">=", "<", "<=", "=in=", "=out="))
                .build();

        RSQLParser publicParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowList(
                        "name", "email", "age", "status", "role", "createdAt"))
                .config(publicApiConfig)
                .build();

        System.out.println("  " + publicParser
                .parse("name==Alice;age>=18;status=in=(active,pending)")
                .accept(new AstPrinter()));
    }

    private static void tryParse(RSQLParser parser, String rsql, String label) {
        try {
            parser.parse(rsql);
            System.out.println(label + " [unexpectedly ALLOWED]");
        } catch (RSQLValidationException | io.github.yamatrireddy.exception.RSQLParseException e) {
            System.out.println(label + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
