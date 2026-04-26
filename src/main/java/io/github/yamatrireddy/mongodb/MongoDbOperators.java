package io.github.yamatrireddy.mongodb;

import java.util.Set;

/**
 * Operator constants used with {@link MongoDbQueryBuilder}.
 *
 * <p>Standard RSQL operators ({@code ==}, {@code !=}, etc.) are translated automatically.
 * The extended operators below map to MongoDB-specific query constructs and must be
 * explicitly added to {@link io.github.yamatrireddy.api.ParserConfig#getAllowedOperators()}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ParserConfig config = ParserConfig.builder()
 *         .allowedOperators(MongoDbOperators.ALL_OPERATORS)
 *         .build();
 * RSQLParser parser = RSQLParser.builder()
 *         .fieldRegistry(FieldRegistry.allowAll())
 *         .config(config)
 *         .build();
 * }</pre>
 */
public final class MongoDbOperators {

    // ── Standard RSQL operators ──────────────────────────────────────────────

    /** Equal — maps to {@code $eq}. Wildcard {@code *} triggers a {@code $regex} match. */
    public static final String EQ  = "==";
    /** Not equal — maps to {@code $ne}. */
    public static final String NEQ = "!=";
    /** Greater than — maps to {@code $gt}. */
    public static final String GT  = ">";
    /** Greater than or equal — maps to {@code $gte}. */
    public static final String GTE = ">=";
    /** Less than — maps to {@code $lt}. */
    public static final String LT  = "<";
    /** Less than or equal — maps to {@code $lte}. */
    public static final String LTE = "<=";
    /** In list — maps to {@code $in}. */
    public static final String IN  = "=in=";
    /** Not in list — maps to {@code $nin}. */
    public static final String OUT = "=out=";

    // ── MongoDB extended operators ───────────────────────────────────────────

    /**
     * Regular expression match — maps to {@code $regex}.
     * <br>Value format: {@code pattern} or {@code pattern/flags} (e.g., {@code alice.*/i}).
     * <br>Example: {@code name=regex=^Alice}
     */
    public static final String REGEX = "=regex=";

    /**
     * Field existence check — maps to {@code $exists}.
     * <br>Values: {@code true} (field must exist) or {@code false} (field must not exist).
     * <br>Example: {@code email=exists=true}
     */
    public static final String EXISTS = "=exists=";

    /**
     * Array contains all specified values — maps to {@code $all}.
     * <br>Example: {@code tags=all=(java,mongodb)}
     */
    public static final String ALL = "=all=";

    /**
     * Array field size — maps to {@code $size}.
     * <br>Value must be a non-negative integer.
     * <br>Example: {@code tags=size=3}
     */
    public static final String SIZE = "=size=";

    /**
     * Array element match — maps to {@code $elemMatch}.
     * <br>Value is an embedded RSQL expression (parsed recursively).
     * <br>Example: {@code scores=elemMatch=value>=90}
     */
    public static final String ELEM_MATCH = "=elemMatch=";

    /**
     * BSON type check — maps to {@code $type}.
     * <br>Value is a BSON type alias: {@code double}, {@code string}, {@code object},
     * {@code array}, {@code binData}, {@code objectId}, {@code bool}, {@code date},
     * {@code null}, {@code regex}, {@code int}, {@code long}, {@code decimal},
     * or the numeric BSON type code.
     * <br>Example: {@code age=type=int}
     */
    public static final String TYPE = "=type=";

    /**
     * Named alias for {@code >=} — maps to {@code $gte}.
     * <br>Example: {@code age=ge=18}
     */
    public static final String GE = "=ge=";

    /**
     * Named alias for {@code >} — maps to {@code $gt}.
     * <br>Example: {@code age=gt=18}
     */
    public static final String GT_NAMED = "=gt=";

    /**
     * Named alias for {@code <=} — maps to {@code $lte}.
     * <br>Example: {@code age=le=65}
     */
    public static final String LE = "=le=";

    /**
     * Named alias for {@code <} — maps to {@code $lt}.
     * <br>Example: {@code age=lt=65}
     */
    public static final String LT_NAMED = "=lt=";

    // ── Convenience sets ──────────────────────────────────────────────────────

    /** All standard RSQL operators without MongoDB extensions. */
    public static final Set<String> STANDARD_OPERATORS = Set.of(
            EQ, NEQ, GT, GTE, LT, LTE, IN, OUT, GT_NAMED, GE, LT_NAMED, LE);

    /** All standard operators plus MongoDB-specific extensions. */
    public static final Set<String> ALL_OPERATORS = Set.of(
            EQ, NEQ, GT, GTE, LT, LTE, IN, OUT,
            GT_NAMED, GE, LT_NAMED, LE,
            REGEX, EXISTS, ALL, SIZE, TYPE);

    private MongoDbOperators() {}
}
