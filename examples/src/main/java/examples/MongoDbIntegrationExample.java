package examples;

import com.mongodb.client.model.Filters;
import io.github.yamatrireddy.api.ParserConfig;
import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.mongodb.MongoDbOperators;
import io.github.yamatrireddy.mongodb.MongoDbQueryBuilder;
import io.github.yamatrireddy.security.FieldRegistry;
import io.github.yamatrireddy.visitor.AstPrinter;
import org.bson.conversions.Bson;

import java.util.Set;

/**
 * Example 05 — MongoDB Integration (MongoDB Java Driver 5.x / MongoDB Server 8.x)
 *
 * Demonstrates translating RSQL/FIQL expressions into MongoDB Bson filter documents
 * using the built-in MongoDbQueryBuilder visitor.
 *
 * Features covered:
 *   - Standard RSQL → MongoDB filter translation
 *   - Wildcard values → $regex
 *   - Type inference (int, long, double, boolean, null, ObjectId, ISO date)
 *   - MongoDB-specific operators: =regex=, =exists=, =all=, =size=, =type=
 *   - Field allowlisting for production safety
 *   - Using the filter with a live MongoCollection (commented out)
 *
 * Run: mvn exec:java -Dexec.mainClass=examples.MongoDbIntegrationExample
 */
public class MongoDbIntegrationExample {

    public static void main(String[] args) {
        System.out.println("=== RSQL → MongoDB Filter Translation ===\n");

        basicTranslation();
        wildcardAndRegex();
        typeInference();
        logicalOperators();
        mongoDbExtendedOperators();
        productionConfig();
        // liveMongoExample();  // uncomment with a running MongoDB instance
    }

    // ── 1. Basic translation ──────────────────────────────────────────────────

    static void basicTranslation() {
        System.out.println("--- 1. Basic operator translation ---");
        RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());
        MongoDbQueryBuilder builder = new MongoDbQueryBuilder();

        String[][] cases = {
                {"name==Alice",          "Equality ($eq)"},
                {"name!=Alice",          "Inequality ($ne)"},
                {"age>18",               "Greater than ($gt)"},
                {"age>=18",              "Greater than or equal ($gte)"},
                {"score<100",            "Less than ($lt)"},
                {"score<=100",           "Less than or equal ($lte)"},
                {"role=in=(admin,user)", "In list ($in)"},
                {"role=out=(banned)",    "Not in list ($nin)"},
        };

        for (String[] c : cases) {
            Bson filter = parser.parse(c[0]).accept(builder);
            System.out.printf("  %-30s -> %-35s [%s]%n",
                    c[0], renderFilter(filter), c[1]);
        }
        System.out.println();
    }

    // ── 2. Wildcard and regex ─────────────────────────────────────────────────

    static void wildcardAndRegex() {
        System.out.println("--- 2. Wildcard (* in value) and explicit regex ---");

        RSQLParser stdParser  = new RSQLParser(FieldRegistry.allowAll());
        RSQLParser extParser  = extendedParser();
        MongoDbQueryBuilder   builder = new MongoDbQueryBuilder();

        // Wildcard via == operator
        Bson prefix  = stdParser.parse("name==Alice*").accept(builder);
        Bson suffix  = stdParser.parse("name==*Smith").accept(builder);
        Bson contains = stdParser.parse("name==*alice*").accept(builder);

        System.out.println("  name==Alice*      -> " + renderFilter(prefix));
        System.out.println("  name==*Smith      -> " + renderFilter(suffix));
        System.out.println("  name==*alice*     -> " + renderFilter(contains));

        // Explicit =regex= operator with optional flags
        Bson regexNoFlags = extParser.parse("name=regex=^alice").accept(builder);
        Bson regexCaseI   = extParser.parse("name=regex=^alice/i").accept(builder);
        Bson regexMulti   = extParser.parse("description=regex=first.*last/si").accept(builder);

        System.out.println("  name=regex=^alice     -> " + renderFilter(regexNoFlags));
        System.out.println("  name=regex=^alice/i   -> " + renderFilter(regexCaseI));
        System.out.println("  desc=regex=first.*last/si -> " + renderFilter(regexMulti));
        System.out.println();
    }

    // ── 3. Automatic type inference ───────────────────────────────────────────

    static void typeInference() {
        System.out.println("--- 3. Value type inference ---");
        RSQLParser parser  = new RSQLParser(FieldRegistry.allowAll());
        MongoDbQueryBuilder builder = new MongoDbQueryBuilder();

        // Show the inferred Java/BSON type for various value strings
        String[] values = {"42", "9876543210", "3.14", "true", "false",
                           "null", "2024-01-15T10:30:00Z", "507f1f77bcf86cd799439011", "Alice"};
        System.out.println("  Value                          -> Inferred Java type");
        System.out.println("  " + "-".repeat(55));
        for (String v : values) {
            Object converted = builder.convertValue(v);
            String type = converted == null ? "null" : converted.getClass().getSimpleName();
            System.out.printf("  %-30s -> %s%n", v, type);
        }

        // Show how inferred types affect the filter
        System.out.println();
        System.out.println("  age==25 (integer)  -> " + renderFilter(parser.parse("age==25").accept(builder)));
        System.out.println("  active==true (bool)-> " + renderFilter(parser.parse("active==true").accept(builder)));
        System.out.println();
    }

    // ── 4. Logical operators ──────────────────────────────────────────────────

    static void logicalOperators() {
        System.out.println("--- 4. Logical operators ($and, $or) ---");
        RSQLParser parser  = new RSQLParser(FieldRegistry.allowAll());
        MongoDbQueryBuilder builder = new MongoDbQueryBuilder();

        Bson and     = parser.parse("name==Alice;age>=18").accept(builder);
        Bson or      = parser.parse("status==active,status==pending").accept(builder);
        Bson complex = parser.parse("(name==Alice,name==Bob);age>=18").accept(builder);
        Bson flat    = parser.parse("a==1;b==2;c==3").accept(builder);

        System.out.println("  name==Alice;age>=18               -> $and: " + renderFilter(and));
        System.out.println("  status==active,status==pending    -> $or:  " + renderFilter(or));
        System.out.println("  (name==Alice,name==Bob);age>=18   ->       " + renderFilter(complex));
        System.out.println("  a==1;b==2;c==3 (flattened 3-way)  ->       " + renderFilter(flat));
        System.out.println();
    }

    // ── 5. MongoDB-specific extended operators ────────────────────────────────

    static void mongoDbExtendedOperators() {
        System.out.println("--- 5. MongoDB-specific extended operators ---");
        RSQLParser parser  = extendedParser();
        MongoDbQueryBuilder builder = new MongoDbQueryBuilder();

        // $exists
        Bson emailExists   = parser.parse("email=exists=true").accept(builder);
        Bson deletedMissing = parser.parse("deletedAt=exists=false").accept(builder);
        System.out.println("  email=exists=true        -> " + renderFilter(emailExists));
        System.out.println("  deletedAt=exists=false   -> " + renderFilter(deletedMissing));

        // $all — array contains all values
        Bson allTags = parser.parse("tags=all=(java,mongodb,spring)").accept(builder);
        System.out.println("  tags=all=(java,mongodb,spring) -> " + renderFilter(allTags));

        // $size — array length
        Bson exactlyThree = parser.parse("tags=size=3").accept(builder);
        System.out.println("  tags=size=3              -> " + renderFilter(exactlyThree));

        // $type — BSON type assertion
        Bson nameIsString = parser.parse("name=type=string").accept(builder);
        Bson ageIsInt     = parser.parse("age=type=int").accept(builder);
        Bson tsIsDate     = parser.parse("createdAt=type=date").accept(builder);
        System.out.println("  name=type=string         -> " + renderFilter(nameIsString));
        System.out.println("  age=type=int             -> " + renderFilter(ageIsInt));
        System.out.println("  createdAt=type=date      -> " + renderFilter(tsIsDate));

        // Combined: all extended operators in one expression
        Bson combined = parser.parse("email=exists=true;tags=size=3;status==active").accept(builder);
        System.out.println("  email=exists=true;tags=size=3;status==active ->");
        System.out.println("    " + renderFilter(combined));
        System.out.println();
    }

    // ── 6. Production configuration ───────────────────────────────────────────

    static void productionConfig() {
        System.out.println("--- 6. Production-grade configuration ---");

        // Strict allow-list of queryable fields
        FieldRegistry fields = FieldRegistry.allowList(
                "name", "email", "age", "status", "role", "tags",
                "createdAt", "updatedAt", "deletedAt");

        // Full operator set including MongoDB extensions, tight security limits
        ParserConfig config = ParserConfig.builder()
                .maxDepth(5)
                .maxValueLength(128)
                .maxWildcardsPerValue(1)
                .maxInputLength(1024)
                .allowedOperators(MongoDbOperators.ALL_OPERATORS)
                .build();

        RSQLParser parser  = RSQLParser.builder()
                .fieldRegistry(fields)
                .config(config)
                .build();
        MongoDbQueryBuilder builder = new MongoDbQueryBuilder();

        String rsql = "name==Alice*;age>=18;status=in=(active,pending);tags=all=(java,spring)";
        Bson filter = parser.parse(rsql).accept(builder);

        System.out.println("  RSQL  : " + rsql);
        System.out.println("  AST   : " + parser.parse(rsql).accept(new AstPrinter()));
        System.out.println("  Bson  : " + renderFilter(filter));
        System.out.println();
    }

    // ── Live MongoDB example (requires running server) ────────────────────────

    /*
    static void liveMongoExample() {
        // Requires MongoDB 8.x running on localhost:27017
        try (com.mongodb.client.MongoClient client =
                     com.mongodb.client.MongoClients.create("mongodb://localhost:27017")) {

            com.mongodb.client.MongoDatabase db = client.getDatabase("demo");
            com.mongodb.client.MongoCollection<org.bson.Document> collection =
                    db.getCollection("users");

            RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());
            MongoDbQueryBuilder builder = new MongoDbQueryBuilder();

            Bson filter = parser.parse("name==Alice*;age>=18;status=in=(active,pending)")
                    .accept(builder);

            System.out.println("Running query against MongoDB 8.x...");
            collection.find(filter).forEach(doc -> System.out.println("  " + doc.toJson()));
        }
    }
    */

    // ── Helper ────────────────────────────────────────────────────────────────

    private static RSQLParser extendedParser() {
        return RSQLParser.builder()
                .fieldRegistry(FieldRegistry.allowAll())
                .config(ParserConfig.builder()
                        .allowedOperators(MongoDbOperators.ALL_OPERATORS)
                        .build())
                .build();
    }

    private static String renderFilter(Bson bson) {
        return bson.toBsonDocument(
                org.bson.Document.class,
                com.mongodb.MongoClientSettings.getDefaultCodecRegistry()
        ).toJson();
    }
}
