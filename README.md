# RSQL Parser

A secure, enterprise-grade Java library for parsing [RSQL/FIQL](https://github.com/jirutka/rsql-parser) filter expressions into an immutable Abstract Syntax Tree (AST).

[![CI](https://github.com/yamatrireddy/rsql-fiql-parser/actions/workflows/ci.yml/badge.svg)](https://github.com/yamatrireddy/rsql-fiql-parser/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yamatrireddy/rsql-parser)](https://central.sonatype.com/artifact/io.github.yamatrireddy/rsql-parser)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025%20%7C%2026-blue)](https://adoptium.net/)

---

## Features

- **Zero mandatory runtime dependencies** — only `slf4j-api` (consumers provide the logging implementation)
- **Immutable, sealed AST** — `ComparisonNode` and `LogicalNode` are final and thread-safe
- **Visitor pattern** — implement `AstVisitor<T>` to translate an AST into SQL, MongoDB queries, Elasticsearch filters, etc.
- **Built-in MongoDB support** — `MongoDbQueryBuilder` translates RSQL to MongoDB `Bson` filters (Java Driver 5.x / MongoDB 8.x)
- **Configurable security limits** — max depth, value length, wildcard count, input length, and allowed operators are all tunable via `ParserConfig`
- **Flexible field validation** — built-in `FieldRegistry` factories (`allowAll`, `allowList`, `allowPattern`) or supply your own
- **Precise error messages** — `RSQLParseException` includes character-position and original input
- **JPMS ready** — ships a `module-info.java` (`io.github.yamatrireddy.rsql`)
- **Multi-JDK support** — tested against Java 17, 21, 25, and 26

---

## Requirements

| Java version | Support level |
|---|---|
| 17 (LTS) | Minimum baseline; fully supported |
| 21 (LTS) | Fully supported |
| 25 (LTS) | Fully supported |
| 26 | Fully supported |

---

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.yamatrireddy</groupId>
    <artifactId>rsql-parser</artifactId>
    <version>2.0.0</version>
</dependency>
```

Add a SLF4J implementation to your runtime (if you don't already have one):

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.x</version>
    <scope>runtime</scope>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.yamatrireddy:rsql-parser:2.0.0'
```

#### MongoDB support (optional)

If you plan to use `MongoDbQueryBuilder`, also add the MongoDB Java Driver:

```xml
<!-- Maven -->
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>5.3.0</version>
</dependency>
```

```groovy
// Gradle
implementation 'org.mongodb:mongodb-driver-sync:5.3.0'
```

---

## Quick Start

```java
// 1. Create a parser — allow any field, default security limits
RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());

// 2. Parse an RSQL expression
RSQLExpression expr = parser.parse("name==Alice;age>=18");

// 3. Use the built-in printer visitor
String printed = expr.accept(new AstPrinter());
// → "(name==Alice AND age>=18)"

// 4. Inspect the AST directly
AstNode ast = expr.getAst();
if (ast instanceof LogicalNode l) {
    System.out.println(l.getOperator());      // AND
    System.out.println(l.getChildren().size()); // 2
}
```

---

## RSQL Syntax Reference

### Comparison operators

| Symbol | Named alias | Meaning |
|--------|-------------|---------|
| `==`   | —           | Equal |
| `!=`   | —           | Not equal |
| `>`    | `=gt=`      | Greater than |
| `>=`   | `=ge=`      | Greater than or equal |
| `<`    | `=lt=`      | Less than |
| `<=`   | `=le=`      | Less than or equal |
| `=in=` | —           | In list |
| `=out=`| —           | Not in list |

### Logical operators

| Symbol | Keyword | Precedence |
|--------|---------|------------|
| `;`    | `and`   | Higher     |
| `,`    | `or`    | Lower      |

### Examples

```
name==Alice
age>=18
name==Alice;age>=18          ← AND
name==Alice,status==active   ← OR
role=in=(admin,user,guest)
(a==1,b==2);c==3             ← grouping
name=="John Doe"             ← quoted values
name=='O\'Brien'             ← escaped quotes
```

---

## Configuration

### Field validation

```java
// Allow any field (useful in tests or trusted environments)
FieldRegistry.allowAll()

// Allowlist specific field names
FieldRegistry.allowList("name", "age", "email", "role")

// Allow fields matching a regex pattern (e.g., dot-notation paths)
FieldRegistry.allowPattern(Pattern.compile("[a-z]+(\\.[a-z]+)*"))

// Deny all — safe default that must be intentionally overridden
FieldRegistry.denyAll()

// Combine registries with AND logic
FieldRegistry.allowList("name", "age").and(myAuditRegistry)
```

### Security limits

```java
ParserConfig config = ParserConfig.builder()
        .maxDepth(5)                           // max AND/OR nesting depth (default 10)
        .maxValueLength(64)                    // max chars per value (default 128)
        .maxWildcardsPerValue(1)               // max '*' per value (default 2)
        .maxInputLength(1024)                  // max input length (default 4096)
        .allowedOperators(Set.of("==", "!=", "=in=", "=out="))  // restrict operators
        .build();

RSQLParser parser = RSQLParser.builder()
        .fieldRegistry(FieldRegistry.allowList("name", "age", "role"))
        .config(config)
        .build();
```

---

## MongoDB Integration (Java Driver 5.x / MongoDB 8.x)

`MongoDbQueryBuilder` is a built-in `AstVisitor<Bson>` that translates RSQL expressions
directly to MongoDB filter documents. It requires the optional `mongodb-driver-core`
dependency (see [Installation](#installation)).

### Basic usage

```java
RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());
MongoDbQueryBuilder builder = new MongoDbQueryBuilder();

Bson filter = parser.parse("name==Alice;age>=18").accept(builder);
// Equivalent to: { $and: [ { name: "Alice" }, { age: { $gte: 18 } } ] }
collection.find(filter).forEach(doc -> System.out.println(doc.toJson()));
```

### Operator mapping

| RSQL | Named alias | MongoDB filter |
|------|-------------|----------------|
| `==` | — | `$eq` (wildcard `*` → `$regex`) |
| `!=` | — | `$ne` |
| `>`  | `=gt=` | `$gt` |
| `>=` | `=ge=` | `$gte` |
| `<`  | `=lt=` | `$lt` |
| `<=` | `=le=` | `$lte` |
| `=in=` | — | `$in` |
| `=out=` | — | `$nin` |

### MongoDB-specific extended operators

Add `MongoDbOperators.ALL_OPERATORS` to `ParserConfig` to unlock MongoDB-native operators:

```java
ParserConfig config = ParserConfig.builder()
        .allowedOperators(MongoDbOperators.ALL_OPERATORS)
        .build();

RSQLParser parser = RSQLParser.builder()
        .fieldRegistry(FieldRegistry.allowList("name","email","tags","age","status","deletedAt"))
        .config(config)
        .build();

MongoDbQueryBuilder builder = new MongoDbQueryBuilder();
```

| RSQL | MongoDB | Example |
|------|---------|---------|
| `=regex=` | `$regex` | `name=regex=^Alice/i` |
| `=exists=` | `$exists` | `email=exists=true` |
| `=all=` | `$all` | `tags=all=(java,spring)` |
| `=size=` | `$size` | `tags=size=3` |
| `=type=` | `$type` | `age=type=int` |

### Automatic value type inference

String values from RSQL are automatically promoted to the most appropriate Java/BSON type:

| Value | Inferred type |
|-------|---------------|
| `42` | `Integer` |
| `9876543210` | `Long` |
| `3.14` | `Double` |
| `true` / `false` | `Boolean` |
| `null` | `null` |
| `2024-01-15T10:30:00Z` | `java.util.Date` (UTC) |
| `507f1f77bcf86cd799439011` | `ObjectId` |
| anything else | `String` |

### Wildcard to regex

The `==` operator automatically converts wildcard values to case-insensitive regex:

```
name==Alice*   → { name: { $regex: "^Alice.*$", $options: "i" } }
name==*Smith   → { name: { $regex: "^.*Smith$", $options: "i" } }
name==*ali*    → { name: { $regex: "^.*ali.*$", $options: "i" } }
```

Use `=regex=` for full control over the pattern and flags:

```
name=regex=^alice/i   → { name: { $regex: "^alice", $options: "i" } }
```

---

## Writing a Custom Visitor

Implement `AstVisitor<T>` to translate the AST into any target representation:

```java
public class SqlWhereVisitor implements AstVisitor<String> {

    private final List<Object> params = new ArrayList<>();

    @Override
    public String visitComparison(ComparisonNode node) {
        String col = sanitize(node.getField());
        return switch (node.getOperator()) {
            case "==" -> { params.add(node.getValues().get(0)); yield col + " = ?"; }
            case "!=" -> { params.add(node.getValues().get(0)); yield col + " != ?"; }
            case "=in=" -> {
                node.getValues().forEach(params::add);
                String placeholders = node.getValues().stream().map(v -> "?")
                        .collect(Collectors.joining(", "));
                yield col + " IN (" + placeholders + ")";
            }
            // ... other operators
            default -> throw new IllegalArgumentException("Unsupported: " + node.getOperator());
        };
    }

    @Override
    public String visitLogical(LogicalNode node) {
        String sep = "AND".equals(node.getOperator()) ? " AND " : " OR ";
        return node.getChildren().stream()
                .map(c -> c.accept(this))
                .collect(Collectors.joining(sep, "(", ")"));
    }

    public List<Object> getParams() { return Collections.unmodifiableList(params); }
}

// Usage:
SqlWhereVisitor visitor = new SqlWhereVisitor();
String whereClause = parser.parse("name==Alice;age>=18").accept(visitor);
// whereClause → "(name = ? AND age >= ?)"
// visitor.getParams() → ["Alice", "18"]
```

---

## Exception Hierarchy

```
RSQLException  (RuntimeException)
├── RSQLParseException    — syntax error; carries position and original input
└── RSQLValidationException — security/semantic violation
```

---

## Examples

The [`examples/`](examples/) directory contains runnable Java programs covering every use-case:

| Example | What it shows |
|---------|---------------|
| [`BasicUsageExample`](examples/src/main/java/examples/BasicUsageExample.java) | Parse expressions, inspect AST, all operators, quoted & wildcard values |
| [`FieldValidationExample`](examples/src/main/java/examples/FieldValidationExample.java) | `allowAll`, `allowList`, `allowPattern`, `denyAll`, combined & custom registries |
| [`SecurityConfigExample`](examples/src/main/java/examples/SecurityConfigExample.java) | All security limits, operator restrictions, production config |
| [`CustomVisitorExample`](examples/src/main/java/examples/CustomVisitorExample.java) | SQL WHERE clause visitor, Elasticsearch bool query visitor |
| [`MongoDbIntegrationExample`](examples/src/main/java/examples/MongoDbIntegrationExample.java) | Full MongoDB 8.x integration — basic filters, wildcards, type inference, extended operators |
| [`ErrorHandlingExample`](examples/src/main/java/examples/ErrorHandlingExample.java) | Syntax errors with positions, validation errors, production exception handling |
| [`AstInspectionExample`](examples/src/main/java/examples/AstInspectionExample.java) | Pattern matching, field extraction, depth counter, tree pretty-printer |
| [`AdvancedFeaturesExample`](examples/src/main/java/examples/AdvancedFeaturesExample.java) | Multi-tenant fields, expression caching, canonical round-trip, in-memory predicate |

```bash
cd examples
mvn compile
mvn exec:java -Dexec.mainClass=examples.BasicUsageExample
mvn exec:java -Dexec.mainClass=examples.MongoDbIntegrationExample
```

See [`examples/README.md`](examples/README.md) for the full guide.

---

## Publishing to Maven Central

### One-time setup

1. **Register** at [central.sonatype.com](https://central.sonatype.com) and verify your `io.github.<username>` namespace.
2. **Generate a User Token** → Account → User Token and add to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_TOKEN_USERNAME</username>
      <password>YOUR_TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

3. **Generate a GPG key** and publish it to a public keyserver:

```bash
gpg --gen-key
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### Publishing a release

```bash
# Tag the release
git tag v2.0.0
git push origin v2.0.0

# Or publish manually
mvn deploy -P release,java-17
```

The `release` Maven profile automatically:
- Attaches sources JAR
- Attaches Javadoc JAR
- Signs all artifacts with GPG
- Uploads to Sonatype Central via `central-publishing-maven-plugin`

### CI/CD (GitHub Actions)

The repository includes two workflows:

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | Push / PR | Matrix build & test on Java 17, 21, 25, 26 |
| `release.yml` | Push tag `v*.*.*` | Sign and publish to Maven Central |

Required GitHub repository secrets:

| Secret | Description |
|--------|-------------|
| `GPG_PRIVATE_KEY` | ASCII-armoured GPG private key (`gpg --armor --export-secret-keys KEY_ID`) |
| `GPG_PASSPHRASE` | Passphrase for the GPG key |
| `MAVEN_CENTRAL_TOKEN_USERNAME` | Sonatype Central user token username |
| `MAVEN_CENTRAL_TOKEN_PASSWORD` | Sonatype Central user token password |

---

## Building Locally

```bash
# Default (Java 17)
mvn verify

# Specific Java version
mvn verify -P java-21

# Skip tests
mvn package -DskipTests
```

---

## License

MIT License © 2026 Yamatri Reddy. See [LICENSE](LICENSE) for details.
