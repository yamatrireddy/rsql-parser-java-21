package io.github.yamatrireddy.mongodb;

import io.github.yamatrireddy.api.ParserConfig;
import io.github.yamatrireddy.api.RSQLExpression;
import io.github.yamatrireddy.api.RSQLParser;
import io.github.yamatrireddy.security.FieldRegistry;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("MongoDbQueryBuilder")
class MongoDbQueryBuilderTest {

    private RSQLParser parser;
    private MongoDbQueryBuilder builder;

    @BeforeEach
    void setUp() {
        parser  = new RSQLParser(FieldRegistry.allowAll());
        builder = new MongoDbQueryBuilder();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Bson parse(String rsql) {
        return parser.parse(rsql).accept(builder);
    }

    private String render(Bson bson) {
        return bson.toBsonDocument(Document.class,
                com.mongodb.MongoClientSettings.getDefaultCodecRegistry()).toJson();
    }

    // ── Equality ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Equality operators")
    class EqualityTests {

        @Test
        @DisplayName("== produces $eq with string value")
        void eqString() {
            Bson filter = parse("name==Alice");
            assertThat(render(filter)).contains("\"name\"").contains("\"Alice\"");
        }

        @Test
        @DisplayName("== with integer value infers Integer type")
        void eqInteger() {
            Bson filter = parse("age==25");
            assertThat(render(filter)).contains("\"age\"").contains("25");
        }

        @Test
        @DisplayName("== with boolean value infers Boolean type")
        void eqBoolean() {
            Bson filter = parse("active==true");
            assertThat(render(filter)).contains("\"active\"").contains("true");
        }

        @Test
        @DisplayName("== with null literal produces null value")
        void eqNull() {
            Bson filter = parse("deletedAt==null");
            assertThat(render(filter)).contains("\"deletedAt\"").contains("null");
        }

        @Test
        @DisplayName("== with wildcard produces $regex filter")
        void eqWildcard() {
            Bson filter = parse("name==Alice*");
            String json = render(filter);
            assertThat(json).contains("$regularExpression").contains("Alice");
        }

        @Test
        @DisplayName("!= produces $ne")
        void neq() {
            Bson filter = parse("status!=inactive");
            assertThat(render(filter)).contains("$ne").contains("inactive");
        }
    }

    // ── Comparison ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Comparison operators")
    class ComparisonTests {

        @Test
        @DisplayName("> produces $gt")
        void gt() {
            assertThat(render(parse("age>18"))).contains("$gt").contains("18");
        }

        @Test
        @DisplayName(">= produces $gte")
        void gte() {
            assertThat(render(parse("age>=18"))).contains("$gte").contains("18");
        }

        @Test
        @DisplayName("< produces $lt")
        void lt() {
            assertThat(render(parse("score<100"))).contains("$lt").contains("100");
        }

        @Test
        @DisplayName("<= produces $lte")
        void lte() {
            assertThat(render(parse("score<=100"))).contains("$lte").contains("100");
        }

        @Test
        @DisplayName("=gt= named alias produces $gt")
        void gtNamed() {
            RSQLParser extParser = RSQLParser.builder()
                    .fieldRegistry(FieldRegistry.allowAll())
                    .config(ParserConfig.builder()
                            .allowedOperators(MongoDbOperators.STANDARD_OPERATORS)
                            .build())
                    .build();
            Bson filter = extParser.parse("age=gt=18").accept(builder);
            assertThat(render(filter)).contains("$gt").contains("18");
        }

        @Test
        @DisplayName("=ge= named alias produces $gte")
        void geNamed() {
            RSQLParser extParser = RSQLParser.builder()
                    .fieldRegistry(FieldRegistry.allowAll())
                    .config(ParserConfig.builder()
                            .allowedOperators(MongoDbOperators.STANDARD_OPERATORS)
                            .build())
                    .build();
            Bson filter = extParser.parse("age=ge=18").accept(builder);
            assertThat(render(filter)).contains("$gte").contains("18");
        }
    }

    // ── List operators ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("List operators")
    class ListTests {

        @Test
        @DisplayName("=in= produces $in")
        void in() {
            Bson filter = parse("role=in=(admin,user,guest)");
            String json = render(filter);
            assertThat(json).contains("$in").contains("admin").contains("user").contains("guest");
        }

        @Test
        @DisplayName("=out= produces $nin")
        void out() {
            Bson filter = parse("role=out=(banned,suspended)");
            String json = render(filter);
            assertThat(json).contains("$nin").contains("banned").contains("suspended");
        }

        @Test
        @DisplayName("=in= with numeric values infers Integer type")
        void inNumeric() {
            Bson filter = parse("score=in=(90,95,100)");
            String json = render(filter);
            assertThat(json).contains("$in").contains("90").contains("95");
        }
    }

    // ── Logical operators ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Logical operators")
    class LogicalTests {

        @Test
        @DisplayName("; (AND) produces $and")
        void and() {
            Bson filter = parse("name==Alice;age>=18");
            assertThat(render(filter)).contains("$and");
        }

        @Test
        @DisplayName(", (OR) produces $or")
        void or() {
            Bson filter = parse("status==active,status==pending");
            assertThat(render(filter)).contains("$or");
        }

        @Test
        @DisplayName("Nested AND/OR produces nested $and/$or")
        void nested() {
            Bson filter = parse("(name==Alice,name==Bob);age>=18");
            String json = render(filter);
            assertThat(json).contains("$and").contains("$or");
        }

        @Test
        @DisplayName("Three-clause AND flattens to single $and with three children")
        void flattenedAnd() {
            Bson filter = parse("a==1;b==2;c==3");
            String json = render(filter);
            assertThat(json).contains("$and");
        }
    }

    // ── MongoDB-specific operators ────────────────────────────────────────────

    @Nested
    @DisplayName("MongoDB extended operators")
    class ExtendedOperatorTests {

        private RSQLParser extParser;

        @BeforeEach
        void setUpExtParser() {
            extParser = RSQLParser.builder()
                    .fieldRegistry(FieldRegistry.allowAll())
                    .config(ParserConfig.builder()
                            .allowedOperators(MongoDbOperators.ALL_OPERATORS)
                            .build())
                    .build();
        }

        @Test
        @DisplayName("=regex= produces $regex without flags")
        void regex() {
            Bson filter = extParser.parse("name=regex=^Alice").accept(builder);
            assertThat(render(filter)).contains("$regularExpression").contains("Alice");
        }

        @Test
        @DisplayName("=regex= with /flags produces $regex with $options")
        void regexWithFlags() {
            Bson filter = extParser.parse("name=regex=^alice/i").accept(builder);
            String json = render(filter);
            assertThat(json).contains("$regularExpression");
            assertThat(json).contains("alice");
        }

        @Test
        @DisplayName("=exists=true produces $exists: true")
        void existsTrue() {
            Bson filter = extParser.parse("email=exists=true").accept(builder);
            assertThat(render(filter)).contains("$exists").contains("true");
        }

        @Test
        @DisplayName("=exists=false produces $exists: false")
        void existsFalse() {
            Bson filter = extParser.parse("deletedAt=exists=false").accept(builder);
            assertThat(render(filter)).contains("$exists").contains("false");
        }

        @Test
        @DisplayName("=all= produces $all")
        void all() {
            Bson filter = extParser.parse("tags=all=(java,mongodb)").accept(builder);
            String json = render(filter);
            assertThat(json).contains("$all").contains("java").contains("mongodb");
        }

        @Test
        @DisplayName("=size= produces $size with integer")
        void size() {
            Bson filter = extParser.parse("tags=size=3").accept(builder);
            assertThat(render(filter)).contains("$size").contains("3");
        }

        @Test
        @DisplayName("=type=string produces $type: string")
        void typeString() {
            Bson filter = extParser.parse("name=type=string").accept(builder);
            assertThat(render(filter)).contains("$type");
        }

        @Test
        @DisplayName("=type=int produces $type: int")
        void typeInt() {
            Bson filter = extParser.parse("age=type=int").accept(builder);
            assertThat(render(filter)).contains("$type");
        }

        @Test
        @DisplayName("=type= with unknown alias throws IllegalArgumentException")
        void typeUnknown() {
            Bson filter = assertDoesNotThrow(() ->
                    extParser.parse("age=type=16").accept(builder));
            assertThat(render(filter)).contains("$type").contains("16");
        }
    }

    // ── Value type inference ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Value type inference")
    class TypeInferenceTests {

        @Test
        @DisplayName("Integer string infers Integer")
        void inferInteger() {
            assertThat(builder.convertValue("42")).isEqualTo(42);
        }

        @Test
        @DisplayName("Long string infers Long")
        void inferLong() {
            assertThat(builder.convertValue("9876543210")).isEqualTo(9876543210L);
        }

        @Test
        @DisplayName("Double string infers Double")
        void inferDouble() {
            assertThat(builder.convertValue("3.14")).isEqualTo(3.14);
        }

        @Test
        @DisplayName("Boolean true infers Boolean")
        void inferBooleanTrue() {
            assertThat(builder.convertValue("true")).isEqualTo(Boolean.TRUE);
        }

        @Test
        @DisplayName("Boolean false infers Boolean")
        void inferBooleanFalse() {
            assertThat(builder.convertValue("false")).isEqualTo(Boolean.FALSE);
        }

        @Test
        @DisplayName("null literal infers null")
        void inferNull() {
            assertThat(builder.convertValue("null")).isNull();
        }

        @Test
        @DisplayName("ISO-8601 instant infers Date")
        void inferDate() {
            Object result = builder.convertValue("2024-01-15T10:30:00Z");
            assertThat(result).isInstanceOf(Date.class);
        }

        @Test
        @DisplayName("24-hex string infers ObjectId")
        void inferObjectId() {
            Object result = builder.convertValue("507f1f77bcf86cd799439011");
            assertThat(result).isInstanceOf(org.bson.types.ObjectId.class);
        }

        @Test
        @DisplayName("Plain string stays as String")
        void inferString() {
            assertThat(builder.convertValue("hello")).isEqualTo("hello");
        }
    }

    // ── Unsupported operator ──────────────────────────────────────────────────

    @Test
    @DisplayName("Unknown operator throws IllegalArgumentException")
    void unknownOperator() {
        // Build a ComparisonNode directly with a bogus operator to test the fallback
        io.github.yamatrireddy.ast.ComparisonNode bogus =
                new io.github.yamatrireddy.ast.ComparisonNode("f", "=bogus=", List.of("v"));
        assertThatThrownBy(() -> bogus.accept(builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("=bogus=");
    }
}
