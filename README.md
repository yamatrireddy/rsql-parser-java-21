# RSQL Parser

A secure, enterprise-grade Java library for parsing [RSQL/FIQL](https://github.com/jirutka/rsql-parser) filter expressions into an immutable Abstract Syntax Tree (AST).

[![CI](https://github.com/yamatrireddy/rsql-parser-java-21/actions/workflows/ci.yml/badge.svg)](https://github.com/yamatrireddy/rsql-parser-java-21/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yamatrireddy/rsql-parser)](https://central.sonatype.com/artifact/io.github.yamatrireddy/rsql-parser)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025%20%7C%2026-blue)](https://adoptium.net/)

---

## Features

- **Zero mandatory runtime dependencies** — only `slf4j-api` (consumers provide the logging implementation)
- **Immutable, sealed AST** — `ComparisonNode` and `LogicalNode` are final and thread-safe
- **Visitor pattern** — implement `AstVisitor<T>` to translate an AST into SQL, MongoDB queries, Elasticsearch filters, etc.
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

MIT License © 2024 Yamatri Reddy. See [LICENSE](LICENSE) for details.
