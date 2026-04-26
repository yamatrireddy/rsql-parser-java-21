package io.github.yamatrireddy.mongodb;

import com.mongodb.client.model.Filters;
import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.AstVisitor;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Translates a parsed RSQL AST into a MongoDB {@link Bson} filter document
 * compatible with the MongoDB Java Driver 5.x and MongoDB Server 8.x.
 *
 * <h2>Operator mapping</h2>
 * <table border="1">
 *   <tr><th>RSQL</th><th>MongoDB</th><th>Notes</th></tr>
 *   <tr><td>{@code ==}</td><td>{@code $eq} / {@code $regex}</td><td>Wildcard {@code *} triggers regex</td></tr>
 *   <tr><td>{@code !=}</td><td>{@code $ne}</td><td></td></tr>
 *   <tr><td>{@code >}, {@code =gt=}</td><td>{@code $gt}</td><td></td></tr>
 *   <tr><td>{@code >=}, {@code =ge=}</td><td>{@code $gte}</td><td></td></tr>
 *   <tr><td>{@code <}, {@code =lt=}</td><td>{@code $lt}</td><td></td></tr>
 *   <tr><td>{@code <=}, {@code =le=}</td><td>{@code $lte}</td><td></td></tr>
 *   <tr><td>{@code =in=}</td><td>{@code $in}</td><td></td></tr>
 *   <tr><td>{@code =out=}</td><td>{@code $nin}</td><td></td></tr>
 *   <tr><td>{@code =regex=}</td><td>{@code $regex}</td><td>Pattern[/flags] format</td></tr>
 *   <tr><td>{@code =exists=}</td><td>{@code $exists}</td><td>Value: true/false</td></tr>
 *   <tr><td>{@code =all=}</td><td>{@code $all}</td><td>Array contains-all</td></tr>
 *   <tr><td>{@code =size=}</td><td>{@code $size}</td><td>Array length</td></tr>
 *   <tr><td>{@code =type=}</td><td>{@code $type}</td><td>BSON type alias or code</td></tr>
 * </table>
 *
 * <h2>Type inference</h2>
 * <p>String values are coerced in this order: {@code null} literal → {@code Boolean} →
 * {@code Integer} → {@code Long} → {@code Double} → ISO-8601 {@link Instant} →
 * {@link ObjectId} → {@code String}.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());
 * Bson filter = parser.parse("name==Alice;age>=18").accept(new MongoDbQueryBuilder());
 * collection.find(filter);
 * }</pre>
 *
 * <h2>With MongoDB-specific operators</h2>
 * <pre>{@code
 * ParserConfig config = ParserConfig.builder()
 *         .allowedOperators(MongoDbOperators.ALL_OPERATORS)
 *         .build();
 * RSQLParser parser = RSQLParser.builder()
 *         .fieldRegistry(FieldRegistry.allowAll())
 *         .config(config)
 *         .build();
 * Bson filter = parser.parse("tags=all=(java,spring);email=exists=true")
 *         .accept(new MongoDbQueryBuilder());
 * }</pre>
 *
 * <p>Instances are stateless and thread-safe.
 *
 * @see MongoDbOperators
 */
public final class MongoDbQueryBuilder implements AstVisitor<Bson> {

    /** Maps BSON type alias names to their {@link BsonType} equivalents. */
    private static final Map<String, BsonType> BSON_TYPE_ALIASES = Map.ofEntries(
            Map.entry("double",    BsonType.DOUBLE),
            Map.entry("string",    BsonType.STRING),
            Map.entry("object",    BsonType.DOCUMENT),
            Map.entry("array",     BsonType.ARRAY),
            Map.entry("binData",   BsonType.BINARY),
            Map.entry("objectId",  BsonType.OBJECT_ID),
            Map.entry("bool",      BsonType.BOOLEAN),
            Map.entry("date",      BsonType.DATE_TIME),
            Map.entry("null",      BsonType.NULL),
            Map.entry("regex",     BsonType.REGULAR_EXPRESSION),
            Map.entry("int",       BsonType.INT32),
            Map.entry("long",      BsonType.INT64),
            Map.entry("decimal",   BsonType.DECIMAL128),
            Map.entry("timestamp", BsonType.TIMESTAMP)
    );

    // ── AstVisitor implementation ─────────────────────────────────────────────

    @Override
    public Bson visitComparison(ComparisonNode node) {
        String field = node.getField();
        String op    = node.getOperator();
        List<String> values = node.getValues();

        return switch (op) {
            case "==", "=eq=" -> buildEqFilter(field, values.get(0));
            case "!=", "=neq=" -> Filters.ne(field, convertValue(values.get(0)));
            case ">",  "=gt=" -> Filters.gt(field, convertValue(values.get(0)));
            case ">=", "=ge=" -> Filters.gte(field, convertValue(values.get(0)));
            case "<",  "=lt=" -> Filters.lt(field, convertValue(values.get(0)));
            case "<=", "=le=" -> Filters.lte(field, convertValue(values.get(0)));
            case "=in="  -> Filters.in(field,  values.stream().map(this::convertValue).toList());
            case "=out=" -> Filters.nin(field, values.stream().map(this::convertValue).toList());
            case "=regex="     -> buildRegexFilter(field, values.get(0));
            case "=exists="    -> Filters.exists(field, Boolean.parseBoolean(values.get(0)));
            case "=all="       -> Filters.all(field, values.stream().map(this::convertValue).toList());
            case "=size="      -> Filters.size(field, Integer.parseInt(values.get(0)));
            case "=type="      -> buildTypeFilter(field, values.get(0));
            default -> throw new IllegalArgumentException(
                    "Unsupported operator for MongoDB translation: '" + op + "'");
        };
    }

    @Override
    public Bson visitLogical(LogicalNode node) {
        List<Bson> children = node.getChildren().stream()
                .map(child -> child.accept(this))
                .toList();

        return switch (node.getOperator()) {
            case "AND" -> Filters.and(children);
            case "OR"  -> Filters.or(children);
            default -> throw new IllegalArgumentException(
                    "Unsupported logical operator: '" + node.getOperator() + "'");
        };
    }

    // ── Convenience factory ───────────────────────────────────────────────────

    /**
     * Convenience method — accepts an {@link AstNode} directly without wrapping it in
     * an {@link io.github.yamatrireddy.api.RSQLExpression}.
     *
     * @param node the root AST node
     * @return the equivalent MongoDB {@link Bson} filter
     */
    public Bson build(AstNode node) {
        return node.accept(this);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Bson buildEqFilter(String field, String value) {
        if (value.contains("*")) {
            // Wildcard value → case-insensitive prefix/suffix/substring regex
            String pattern = "^" + Pattern.quote(value).replace("\\*", ".*") + "$";
            return Filters.regex(field, pattern, "i");
        }
        return Filters.eq(field, convertValue(value));
    }

    /**
     * Builds a {@code $regex} filter from a value that may contain an optional
     * flags suffix separated by {@code /}.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "^alice"} → {@code {$regex: "^alice"}}</li>
     *   <li>{@code "^alice/i"} → {@code {$regex: "^alice", $options: "i"}}</li>
     * </ul>
     */
    private Bson buildRegexFilter(String field, String value) {
        int slashIdx = value.lastIndexOf('/');
        if (slashIdx > 0 && slashIdx < value.length() - 1) {
            String pattern = value.substring(0, slashIdx);
            String flags   = value.substring(slashIdx + 1);
            return Filters.regex(field, pattern, flags);
        }
        return Filters.regex(field, value);
    }

    /**
     * Builds a {@code $type} filter accepting either a BSON type alias name
     * (e.g., {@code "string"}, {@code "int"}) or a numeric BSON type code.
     */
    private Bson buildTypeFilter(String field, String typeValue) {
        BsonType bsonType = BSON_TYPE_ALIASES.get(typeValue);
        if (bsonType != null) {
            return Filters.type(field, bsonType);
        }
        try {
            // Fall back to numeric BSON type code
            int typeCode = Integer.parseInt(typeValue);
            return new Document(field, new Document("$type", typeCode));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Unknown BSON type alias or code: '" + typeValue + "'");
        }
    }

    /**
     * Converts a raw string value from an RSQL expression to the most appropriate
     * Java type for MongoDB BSON encoding.
     *
     * <p>Conversion order:
     * <ol>
     *   <li>{@code "null"} → {@code null}</li>
     *   <li>{@code "true"} / {@code "false"} → {@code Boolean}</li>
     *   <li>Integer range → {@code Integer}</li>
     *   <li>Long range → {@code Long}</li>
     *   <li>Decimal → {@code Double}</li>
     *   <li>ISO-8601 instant → {@link java.util.Date}</li>
     *   <li>24-char hex string → {@link ObjectId}</li>
     *   <li>Fallback → {@code String}</li>
     * </ol>
     */
    Object convertValue(String raw) {
        if (raw == null || "null".equalsIgnoreCase(raw)) {
            return null;
        }

        // Boolean
        if ("true".equalsIgnoreCase(raw))  return Boolean.TRUE;
        if ("false".equalsIgnoreCase(raw)) return Boolean.FALSE;

        // Integer
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {}

        // Long
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {}

        // Double
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {}

        // ISO-8601 instant (e.g., "2024-01-15T10:30:00Z")
        try {
            return java.util.Date.from(Instant.parse(raw));
        } catch (DateTimeParseException ignored) {}

        // MongoDB ObjectId (24 hex characters)
        if (ObjectId.isValid(raw)) {
            return new ObjectId(raw);
        }

        return raw;
    }
}
