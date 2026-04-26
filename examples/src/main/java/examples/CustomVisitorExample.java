package examples;

import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.ast.AstVisitor;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.security.FieldRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Example 04 — Custom Visitors
 *
 * Shows two custom AstVisitor implementations:
 *   1. SqlWhereVisitor — translates RSQL to a parameterised SQL WHERE clause
 *   2. ElasticsearchVisitor — translates RSQL to an ES query DSL JSON string
 *
 * Run: mvn exec:java -Dexec.mainClass=examples.CustomVisitorExample
 */
public class CustomVisitorExample {

    public static void main(String[] args) {
        RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());

        System.out.println("=== SQL WHERE visitor ===");
        runSqlExample(parser);

        System.out.println("\n=== Elasticsearch visitor ===");
        runElasticsearchExample(parser);
    }

    // ── SQL Visitor demo ──────────────────────────────────────────────────────

    static void runSqlExample(RSQLParser parser) {
        String[] expressions = {
                "name==Alice",
                "name==Alice;age>=18",
                "role=in=(admin,user)",
                "status!=inactive,status==pending",
                "(name==Alice,name==Bob);age>=18"
        };

        for (String rsql : expressions) {
            SqlWhereVisitor visitor = new SqlWhereVisitor();
            String where = parser.parse(rsql).accept(visitor);
            System.out.printf("  RSQL  : %s%n", rsql);
            System.out.printf("  SQL   : %s%n", where);
            System.out.printf("  Params: %s%n%n", visitor.getParams());
        }
    }

    /**
     * Translates an RSQL AST into a parameterised SQL WHERE clause.
     * Values are collected into a params list; the clause uses positional {@code ?}.
     */
    static class SqlWhereVisitor implements AstVisitor<String> {

        private final List<Object> params = new ArrayList<>();

        @Override
        public String visitComparison(ComparisonNode node) {
            String col = sanitize(node.getField());
            return switch (node.getOperator()) {
                case "==" -> {
                    String val = node.getValues().get(0);
                    if (val.contains("*")) {
                        params.add(val.replace("*", "%"));
                        yield col + " LIKE ?";
                    }
                    params.add(val);
                    yield col + " = ?";
                }
                case "!=" -> {
                    params.add(node.getValues().get(0));
                    yield col + " != ?";
                }
                case ">" -> {
                    params.add(node.getValues().get(0));
                    yield col + " > ?";
                }
                case ">=" -> {
                    params.add(node.getValues().get(0));
                    yield col + " >= ?";
                }
                case "<" -> {
                    params.add(node.getValues().get(0));
                    yield col + " < ?";
                }
                case "<=" -> {
                    params.add(node.getValues().get(0));
                    yield col + " <= ?";
                }
                case "=in=" -> {
                    node.getValues().forEach(params::add);
                    String placeholders = node.getValues().stream()
                            .map(v -> "?").collect(Collectors.joining(", "));
                    yield col + " IN (" + placeholders + ")";
                }
                case "=out=" -> {
                    node.getValues().forEach(params::add);
                    String placeholders = node.getValues().stream()
                            .map(v -> "?").collect(Collectors.joining(", "));
                    yield col + " NOT IN (" + placeholders + ")";
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported SQL operator: " + node.getOperator());
            };
        }

        @Override
        public String visitLogical(LogicalNode node) {
            String sep = "AND".equals(node.getOperator()) ? " AND " : " OR ";
            return node.getChildren().stream()
                    .map(c -> c.accept(this))
                    .collect(Collectors.joining(sep, "(", ")"));
        }

        /** Prevents SQL injection by allowing only word characters and dots. */
        private String sanitize(String field) {
            if (!field.matches("[\\w.]+")) {
                throw new IllegalArgumentException("Invalid field name: " + field);
            }
            return field;
        }

        public List<Object> getParams() {
            return Collections.unmodifiableList(params);
        }
    }

    // ── Elasticsearch Visitor demo ────────────────────────────────────────────

    static void runElasticsearchExample(RSQLParser parser) {
        String[] expressions = {
                "name==Alice",
                "age>=18;status==active",
                "role=in=(admin,editor)",
                "name==Alice*"
        };

        ElasticsearchVisitor visitor = new ElasticsearchVisitor();
        for (String rsql : expressions) {
            String query = parser.parse(rsql).accept(visitor);
            System.out.printf("  RSQL : %s%n", rsql);
            System.out.printf("  ES   : %s%n%n", query);
        }
    }

    /**
     * Translates an RSQL AST into a simplified Elasticsearch bool query JSON string.
     */
    static class ElasticsearchVisitor implements AstVisitor<String> {

        @Override
        public String visitComparison(ComparisonNode node) {
            String field = node.getField();
            return switch (node.getOperator()) {
                case "==" -> {
                    String val = node.getValues().get(0);
                    if (val.contains("*")) {
                        yield "{\"wildcard\":{\"%s\":{\"value\":\"%s\",\"case_insensitive\":true}}}"
                                .formatted(field, val);
                    }
                    yield "{\"term\":{\"%s\":\"%s\"}}".formatted(field, val);
                }
                case "!=" -> "{\"bool\":{\"must_not\":{\"term\":{\"%s\":\"%s\"}}}}"
                        .formatted(field, node.getValues().get(0));
                case ">" -> "{\"range\":{\"%s\":{\"gt\":\"%s\"}}}".formatted(field, node.getValues().get(0));
                case ">=" -> "{\"range\":{\"%s\":{\"gte\":\"%s\"}}}".formatted(field, node.getValues().get(0));
                case "<" -> "{\"range\":{\"%s\":{\"lt\":\"%s\"}}}".formatted(field, node.getValues().get(0));
                case "<=" -> "{\"range\":{\"%s\":{\"lte\":\"%s\"}}}".formatted(field, node.getValues().get(0));
                case "=in=" -> {
                    String terms = node.getValues().stream()
                            .map(v -> "\"" + v + "\"").collect(Collectors.joining(","));
                    yield "{\"terms\":{\"%s\":[%s]}}".formatted(field, terms);
                }
                case "=out=" -> {
                    String terms = node.getValues().stream()
                            .map(v -> "\"" + v + "\"").collect(Collectors.joining(","));
                    yield "{\"bool\":{\"must_not\":{\"terms\":{\"%s\":[%s]}}}}".formatted(field, terms);
                }
                default -> throw new IllegalArgumentException("Unsupported: " + node.getOperator());
            };
        }

        @Override
        public String visitLogical(LogicalNode node) {
            String clause = "AND".equals(node.getOperator()) ? "must" : "should";
            String children = node.getChildren().stream()
                    .map(c -> c.accept(this))
                    .collect(Collectors.joining(","));
            return "{\"bool\":{\"%s\":[%s]}}".formatted(clause, children);
        }
    }
}
