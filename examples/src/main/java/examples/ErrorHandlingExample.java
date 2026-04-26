package examples;

import io.github.yamatrireddy.api.ParserConfig;
import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.exception.RSQLException;
import io.github.yamatrireddy.exception.RSQLParseException;
import io.github.yamatrireddy.exception.RSQLValidationException;
import io.github.yamatrireddy.security.FieldRegistry;

import java.util.Set;

/**
 * Example 06 — Error Handling
 *
 * Demonstrates the exception hierarchy and how to handle every error case:
 *   - RSQLParseException   : syntax errors (bad tokens, unmatched parens, etc.)
 *   - RSQLValidationException : security constraint violations
 *
 * Run: mvn exec:java -Dexec.mainClass=examples.ErrorHandlingExample
 */
public class ErrorHandlingExample {

    public static void main(String[] args) {
        RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());

        syntaxErrors(parser);
        validationErrors();
        productionErrorHandling(parser);
    }

    // ── 1. Syntax (parse) errors ──────────────────────────────────────────────

    static void syntaxErrors(RSQLParser parser) {
        System.out.println("=== Syntax errors (RSQLParseException) ===\n");

        String[] badInputs = {
                "",                    // blank input
                "name",                // missing operator and value
                "name==",              // missing value
                "==value",             // missing field
                "name==Alice;",        // trailing semicolon
                "(name==Alice",        // unclosed parenthesis
                "name==Alice)",        // unexpected close paren
                "a==1;;b==2",          // double semicolon
        };

        for (String input : badInputs) {
            try {
                parser.parse(input);
                System.out.printf("  %-30s -> [ALLOWED - unexpected]%n", show(input));
            } catch (RSQLParseException e) {
                System.out.printf("  %-30s -> ParseException at pos %d: %s%n",
                        show(input), e.getPosition(), e.getMessage());
            }
        }
    }

    // ── 2. Validation errors ──────────────────────────────────────────────────

    static void validationErrors() {
        System.out.println("\n=== Validation errors (RSQLValidationException) ===\n");

        // Field not in allow-list
        System.out.println("  Field not in allow-list:");
        RSQLParser fieldParser = new RSQLParser(FieldRegistry.allowList("name", "age"));
        tryValidation(fieldParser, "password==secret",
                "    password==secret -> BLOCKED");

        // Depth limit exceeded
        System.out.println("\n  Depth limit (maxDepth=2) exceeded:");
        RSQLParser depthParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxDepth(2).build())
                .build();
        tryValidation(depthParser, "(a==1;b==2);(c==3;d==4)",
                "    4-level nesting -> BLOCKED");

        // Value too long
        System.out.println("\n  Value too long (maxValueLength=10):");
        RSQLParser lenParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxValueLength(10).build())
                .build();
        tryValidation(lenParser, "name==VeryLongNameThatExceeds",
                "    28-char value -> BLOCKED");

        // Too many wildcards
        System.out.println("\n  Too many wildcards (maxWildcardsPerValue=1):");
        RSQLParser wildcardParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxWildcardsPerValue(1).build())
                .build();
        tryValidation(wildcardParser, "name==*alice*",
                "    *alice* (2 wildcards) -> BLOCKED");

        // Input too long
        System.out.println("\n  Input too long (maxInputLength=20):");
        RSQLParser inputParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxInputLength(20).build())
                .build();
        tryValidation(inputParser, "name==Alice;age>=18;role==admin",
                "    31-char input -> BLOCKED");

        // Operator not allowed
        System.out.println("\n  Operator not in allowed set:");
        RSQLParser opParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder()
                        .allowedOperators(Set.of("==", "!="))
                        .build())
                .build();
        tryValidation(opParser, "age>18",
                "    > (not in {==, !=}) -> BLOCKED");
    }

    // ── 3. Production error handling pattern ─────────────────────────────────

    static void productionErrorHandling(RSQLParser parser) {
        System.out.println("\n=== Production error handling pattern ===\n");

        String[] inputs = {
                "name==Alice;age>=18",   // valid
                "name==Alice;",          // syntax error
                null                     // null input
        };

        for (String input : inputs) {
            try {
                var expr = parser.parse(input);
                System.out.println("  OK   : " + input);
            } catch (RSQLParseException e) {
                // Return HTTP 400 Bad Request
                System.out.printf("  400  : Syntax error at position %d in '%s': %s%n",
                        e.getPosition(), e.getInput(), e.getMessage());
            } catch (RSQLValidationException e) {
                // Return HTTP 400 Bad Request
                System.out.println("  400  : Validation failed: " + e.getMessage());
            } catch (RSQLException e) {
                // Catch-all for any future RSQL exception subtypes
                System.out.println("  400  : RSQL error: " + e.getMessage());
            } catch (NullPointerException e) {
                // Null input produces NullPointerException
                System.out.println("  400  : input must not be null");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void tryValidation(RSQLParser parser, String input, String label) {
        try {
            parser.parse(input);
            System.out.println(label + " [ALLOWED - unexpected]");
        } catch (RSQLValidationException e) {
            System.out.println(label + " -> ValidationException: " + e.getMessage());
        } catch (RSQLParseException e) {
            System.out.println(label + " -> ParseException: " + e.getMessage());
        }
    }

    private static String show(String s) {
        return s.isEmpty() ? "(empty)" : "\"" + s + "\"";
    }
}
