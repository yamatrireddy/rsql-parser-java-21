package examples;

import io.github.yamatrireddy.api.ParserConfig;
import io.github.yamatrireddy.api.RSQLExpression;
import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.ast.AstVisitor;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.security.FieldRegistry;
import io.github.yamatrireddy.visitor.AstPrinter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Example 08 — Advanced Features
 *
 * Covers advanced use-cases:
 *   1.  Multi-tenant field namespacing (per-tenant field allow-lists)
 *   2.  Request-scoped caching of parsed expressions
 *   3.  AST round-trip canonical form (AstPrinter)
 *   4.  "and" / "or" keyword syntax for logical operators
 *   5.  Dot-notation nested field paths (user.address.city)
 *   6.  Thread-safety — RSQLParser is reusable across threads
 *   7.  Boolean query builder (fluent predicate composition)
 *
 * Run: mvn exec:java -Dexec.mainClass=examples.AdvancedFeaturesExample
 */
public class AdvancedFeaturesExample {

    public static void main(String[] args) {
        multiTenantNamespacing();
        requestScopedCaching();
        roundTripCanonical();
        keywordSyntax();
        dotNotationPaths();
        threadSafety();
        fluencyAdapter();
    }

    // ── 1. Multi-tenant field namespacing ─────────────────────────────────────

    static void multiTenantNamespacing() {
        System.out.println("=== 1. Multi-tenant field namespacing ===\n");

        // Each tenant exposes a different subset of searchable fields
        Map<String, FieldRegistry> tenantRegistries = Map.of(
                "tenant-a", FieldRegistry.allowList("name", "email", "age"),
                "tenant-b", FieldRegistry.allowList("product", "price", "category"),
                "tenant-c", FieldRegistry.allowPattern(Pattern.compile("[a-z_]+"))
        );

        String rsql = "name==Alice";
        for (Map.Entry<String, FieldRegistry> entry : tenantRegistries.entrySet()) {
            RSQLParser parser = new RSQLParser(entry.getValue());
            try {
                System.out.printf("  %-10s -> %s%n",
                        entry.getKey(), parser.parse(rsql).accept(new AstPrinter()));
            } catch (Exception e) {
                System.out.printf("  %-10s -> BLOCKED: %s%n", entry.getKey(), e.getMessage());
            }
        }
        System.out.println();
    }

    // ── 2. Request-scoped expression caching ──────────────────────────────────

    static void requestScopedCaching() {
        System.out.println("=== 2. Parsed expression caching ===\n");

        // RSQLParser is thread-safe and designed to be reused.
        // CachingRSQLParser wraps it with a simple ConcurrentHashMap cache.
        CachingRSQLParser caching = new CachingRSQLParser(
                new RSQLParser(FieldRegistry.allowAll()));

        String expr1 = "name==Alice;age>=18";
        String expr2 = "role=in=(admin,user)";

        System.out.println("  First  parse: " + caching.parse(expr1).accept(new AstPrinter()));
        System.out.println("  Cached parse: " + caching.parse(expr1).accept(new AstPrinter()));
        System.out.println("  Cache size  : " + caching.cacheSize());
        System.out.println("  Second key  : " + caching.parse(expr2).accept(new AstPrinter()));
        System.out.println("  Cache size  : " + caching.cacheSize());
        System.out.println();
    }

    static class CachingRSQLParser {
        private final RSQLParser delegate;
        private final ConcurrentHashMap<String, RSQLExpression> cache = new ConcurrentHashMap<>();

        CachingRSQLParser(RSQLParser delegate) {
            this.delegate = delegate;
        }

        RSQLExpression parse(String rsql) {
            return cache.computeIfAbsent(rsql, delegate::parse);
        }

        int cacheSize() { return cache.size(); }
    }

    // ── 3. Canonical round-trip via AstPrinter ────────────────────────────────

    static void roundTripCanonical() {
        System.out.println("=== 3. Canonical round-trip (AstPrinter) ===\n");

        RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());
        AstPrinter printer = new AstPrinter();

        String[] inputs = {
                "name==Alice;age>=18",
                "name==Alice,name==Bob",
                "role=in=(admin,user);status==active",
                // FIQL semicolon/comma mixed with 'and'/'or' keywords normalise to same AST
                "name==Alice and age>=18",
        };

        for (String input : inputs) {
            String canonical = parser.parse(input).accept(printer);
            // Round-trip: parse the canonical form back → same canonical output
            String roundTrip  = parser.parse(canonical).accept(printer);
            System.out.printf("  Input    : %-40s%n", input);
            System.out.printf("  Canonical: %-40s%n", canonical);
            System.out.printf("  Stable   : %s%n%n", canonical.equals(roundTrip) ? "yes" : "DIFFERS");
        }
    }

    // ── 4. 'and' / 'or' keyword syntax ───────────────────────────────────────

    static void keywordSyntax() {
        System.out.println("=== 4. Keyword and/or syntax ===\n");

        RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());
        AstPrinter printer = new AstPrinter();

        // Semicolons/commas and keywords are interchangeable
        String[][] pairs = {
                {"name==Alice;age>=18",          "name==Alice and age>=18"},
                {"status==active,status==pending", "status==active or status==pending"},
                {"a==1;b==2,c==3",               "a==1 and b==2 or c==3"},
        };

        for (String[] pair : pairs) {
            String canon1 = parser.parse(pair[0]).accept(printer);
            String canon2 = parser.parse(pair[1]).accept(printer);
            System.out.printf("  Symbol: %-30s -> %s%n", pair[0], canon1);
            System.out.printf("  Keyword: %-29s -> %s%n", pair[1], canon2);
            System.out.printf("  Equivalent: %s%n%n", canon1.equals(canon2) ? "yes" : "NO");
        }
    }

    // ── 5. Dot-notation nested field paths ────────────────────────────────────

    static void dotNotationPaths() {
        System.out.println("=== 5. Dot-notation nested fields ===\n");

        // Allow fields matching dot-notation paths like "user.address.city"
        RSQLParser parser = new RSQLParser(
                FieldRegistry.allowPattern(Pattern.compile("[a-z][a-zA-Z0-9]*(\\.[a-z][a-zA-Z0-9]*)*")));

        String[] expressions = {
                "user.name==Alice",
                "user.address.city==NYC",
                "order.items.price>=100",
                "metadata.tags==important",
        };

        for (String rsql : expressions) {
            System.out.printf("  %-35s -> %s%n",
                    rsql, parser.parse(rsql).accept(new AstPrinter()));
        }
        System.out.println();
    }

    // ── 6. Thread-safety demonstration ───────────────────────────────────────

    static void threadSafety() throws RuntimeException {
        System.out.println("=== 6. Thread-safety (shared parser) ===\n");

        // RSQLParser is immutable and thread-safe — one shared instance for all threads
        RSQLParser sharedParser = RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder().maxDepth(5).build())
                .build();

        // Parse 100 expressions concurrently from 10 threads
        int threadCount  = 10;
        int parsesEach   = 10;
        var threads = new Thread[threadCount];
        var errors  = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int tid = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < parsesEach; i++) {
                    try {
                        String rsql = "field" + (tid * parsesEach + i) + "==value";
                        sharedParser.parse(rsql);
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
        }

        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) {
            try { thread.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        System.out.printf("  Parsed %d expressions across %d threads — errors: %d%n%n",
                threadCount * parsesEach, threadCount, errors.get());
    }

    // ── 7. Fluent boolean-predicate adapter ───────────────────────────────────

    static void fluencyAdapter() {
        System.out.println("=== 7. In-memory predicate evaluation ===\n");

        RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());

        // User data map
        Map<String, Object> alice = Map.of("name", "Alice", "age", 30, "role", "admin", "active", true);
        Map<String, Object> bob   = Map.of("name", "Bob",   "age", 17, "role", "user",  "active", false);

        String[] queries = {
                "name==Alice",
                "age>=18",
                "role=in=(admin,editor)",
                "name==Alice;age>=18",
                "active==true;role==admin",
        };

        System.out.printf("  %-30s  Alice  Bob%n", "Query");
        System.out.println("  " + "-".repeat(48));
        for (String query : queries) {
            InMemoryEvaluator ev = new InMemoryEvaluator();
            var matchFn = parser.parse(query).accept(ev);
            System.out.printf("  %-30s  %-5s  %s%n",
                    query,
                    matchFn.test(alice) ? "YES" : "no",
                    matchFn.test(bob)   ? "YES" : "no");
        }
    }

    /** Evaluates an RSQL expression against an in-memory {@code Map<String,Object>}. */
    static class InMemoryEvaluator implements AstVisitor<java.util.function.Predicate<Map<String, Object>>> {

        @Override
        public java.util.function.Predicate<Map<String, Object>> visitComparison(ComparisonNode node) {
            return record -> {
                Object raw = record.get(node.getField());
                if (raw == null) return false;
                String fieldStr = raw.toString();

                return switch (node.getOperator()) {
                    case "==" -> fieldStr.equals(node.getValues().get(0));
                    case "!=" -> !fieldStr.equals(node.getValues().get(0));
                    case ">"  -> compare(raw, node.getValues().get(0)) > 0;
                    case ">=" -> compare(raw, node.getValues().get(0)) >= 0;
                    case "<"  -> compare(raw, node.getValues().get(0)) < 0;
                    case "<=" -> compare(raw, node.getValues().get(0)) <= 0;
                    case "=in="  -> node.getValues().contains(fieldStr);
                    case "=out=" -> !node.getValues().contains(fieldStr);
                    default -> false;
                };
            };
        }

        @Override
        public java.util.function.Predicate<Map<String, Object>> visitLogical(LogicalNode node) {
            var predicates = node.getChildren().stream()
                    .map(c -> c.accept(this))
                    .toList();
            return "AND".equals(node.getOperator())
                    ? record -> predicates.stream().allMatch(p -> p.test(record))
                    : record -> predicates.stream().anyMatch(p -> p.test(record));
        }

        @SuppressWarnings("unchecked")
        private int compare(Object field, String value) {
            if (field instanceof Number n) {
                try { return Double.compare(n.doubleValue(), Double.parseDouble(value)); }
                catch (NumberFormatException e) { /* fall through */ }
            }
            return field.toString().compareTo(value);
        }
    }
}
