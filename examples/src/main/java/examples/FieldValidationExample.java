package examples;

import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.exception.RSQLValidationException;
import io.github.yamatrireddy.security.FieldRegistry;
import io.github.yamatrireddy.visitor.AstPrinter;

import java.util.regex.Pattern;

/**
 * Example 02 — Field Validation
 *
 * Demonstrates all FieldRegistry factory methods and how combining them
 * protects against arbitrary field injection.
 *
 * Run: mvn exec:java -Dexec.mainClass=examples.FieldValidationExample
 */
public class FieldValidationExample {

    public static void main(String[] args) {

        // ── 1. allowAll() — no field restrictions ───────────────────────────
        System.out.println("=== allowAll() ===");
        RSQLParser allowAll = new RSQLParser(FieldRegistry.allowAll());
        System.out.println(allowAll.parse("anyField==value").accept(new AstPrinter()));
        System.out.println(allowAll.parse("nested.field==value").accept(new AstPrinter()));

        // ── 2. allowList() — exact field names ──────────────────────────────
        System.out.println("\n=== allowList(\"name\", \"age\", \"email\") ===");
        RSQLParser allowList = new RSQLParser(FieldRegistry.allowList("name", "age", "email"));

        System.out.println("  name==Alice  -> " + allowList.parse("name==Alice").accept(new AstPrinter()));
        System.out.println("  age>=18      -> " + allowList.parse("age>=18").accept(new AstPrinter()));

        tryParse(allowList, "password==secret",
                "  password==secret -> BLOCKED (not in allow-list)");

        // ── 3. allowPattern() — regex-based field validation ─────────────────
        System.out.println("\n=== allowPattern(\"[a-z]+(\\\\.[a-z]+)*\") ===");
        // Allows dot-notation paths like "user.address.city"
        RSQLParser patternParser = new RSQLParser(
                FieldRegistry.allowPattern(Pattern.compile("[a-z]+(\\.[a-z]+)*")));

        System.out.println("  name==Alice           -> " +
                patternParser.parse("name==Alice").accept(new AstPrinter()));
        System.out.println("  user.address.city==NY -> " +
                patternParser.parse("user.address.city==NY").accept(new AstPrinter()));

        tryParse(patternParser, "UPPER==value",
                "  UPPER==value          -> BLOCKED (uppercase not in pattern)");
        tryParse(patternParser, "__proto__==polluted",
                "  __proto__==polluted   -> BLOCKED (special chars not in pattern)");

        // ── 4. denyAll() — reject every field ───────────────────────────────
        System.out.println("\n=== denyAll() ===");
        RSQLParser denyAll = new RSQLParser(FieldRegistry.denyAll());

        tryParse(denyAll, "name==Alice",
                "  name==Alice -> BLOCKED (all fields denied)");

        // ── 5. Combining registries with .and() ──────────────────────────────
        System.out.println("\n=== allowList.and(allowPattern) — must pass both ===");
        // Allow only "name" and "age", AND must match lowercase pattern
        FieldRegistry combined = FieldRegistry.allowList("name", "age", "email")
                .and(FieldRegistry.allowPattern(Pattern.compile("[a-z]+")));
        RSQLParser combinedParser = new RSQLParser(combined);

        System.out.println("  name==Alice -> " +
                combinedParser.parse("name==Alice").accept(new AstPrinter()));

        tryParse(combinedParser, "Name==Alice",
                "  Name==Alice -> BLOCKED (fails pattern even though pattern allows it)");

        // ── 6. Custom FieldRegistry lambda ──────────────────────────────────
        System.out.println("\n=== Custom FieldRegistry (prefix filter) ===");
        // Only allow fields that start with "user_"
        FieldRegistry prefixed = field -> field.startsWith("user_");
        RSQLParser prefixParser = new RSQLParser(prefixed);

        System.out.println("  user_name==Alice -> " +
                prefixParser.parse("user_name==Alice").accept(new AstPrinter()));

        tryParse(prefixParser, "admin_flag==true",
                "  admin_flag==true -> BLOCKED (doesn't start with user_)");
    }

    private static void tryParse(RSQLParser parser, String rsql, String label) {
        try {
            parser.parse(rsql);
            System.out.println(label + " [unexpectedly ALLOWED]");
        } catch (RSQLValidationException e) {
            System.out.println(label + " -> ValidationException: " + e.getMessage());
        }
    }
}
