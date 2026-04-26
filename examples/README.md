# RSQL Parser — Examples

Runnable examples demonstrating every use-case of the `rsql-parser` library.

---

## Examples

| File | Description |
|------|-------------|
| [BasicUsageExample.java](src/main/java/examples/BasicUsageExample.java) | Parse expressions, inspect AST nodes, all comparison operators, quoted and wildcard values |
| [FieldValidationExample.java](src/main/java/examples/FieldValidationExample.java) | `allowAll`, `allowList`, `allowPattern`, `denyAll`, combined registries, custom lambda |
| [SecurityConfigExample.java](src/main/java/examples/SecurityConfigExample.java) | `maxDepth`, `maxValueLength`, `maxWildcardsPerValue`, `maxInputLength`, `allowedOperators`, production config |
| [CustomVisitorExample.java](src/main/java/examples/CustomVisitorExample.java) | SQL WHERE clause visitor (parameterised), Elasticsearch bool query visitor |
| [MongoDbIntegrationExample.java](src/main/java/examples/MongoDbIntegrationExample.java) | MongoDB 8.x filter translation, wildcard→regex, type inference, extended operators |
| [ErrorHandlingExample.java](src/main/java/examples/ErrorHandlingExample.java) | Syntax errors, validation errors, production exception handling pattern |
| [AstInspectionExample.java](src/main/java/examples/AstInspectionExample.java) | Pattern matching, field extraction, depth counting, tree pretty-printer |
| [AdvancedFeaturesExample.java](src/main/java/examples/AdvancedFeaturesExample.java) | Multi-tenant namespacing, expression caching, round-trip canonical form, in-memory predicate |

---

## Running

### Prerequisites

- Java 17 or later
- Maven 3.8+
- The library published locally (or available on Maven Central)

### Build and run a single example

```bash
# From the examples/ directory
mvn compile

# Run any example
mvn exec:java -Dexec.mainClass=examples.BasicUsageExample
mvn exec:java -Dexec.mainClass=examples.FieldValidationExample
mvn exec:java -Dexec.mainClass=examples.SecurityConfigExample
mvn exec:java -Dexec.mainClass=examples.CustomVisitorExample
mvn exec:java -Dexec.mainClass=examples.MongoDbIntegrationExample
mvn exec:java -Dexec.mainClass=examples.ErrorHandlingExample
mvn exec:java -Dexec.mainClass=examples.AstInspectionExample
mvn exec:java -Dexec.mainClass=examples.AdvancedFeaturesExample
```

### If the library is not yet on Maven Central

Build and install the library locally first:

```bash
# From the project root
mvn install -DskipTests

# Then run examples
cd examples
mvn exec:java -Dexec.mainClass=examples.BasicUsageExample
```

---

## RSQL / FIQL Syntax Quick Reference

### Comparison operators

| RSQL | Named alias | MongoDB | SQL | Meaning |
|------|-------------|---------|-----|---------|
| `==` | — | `$eq` / `$regex` | `= ?` / `LIKE ?` | Equal (wildcard `*` → regex/LIKE) |
| `!=` | — | `$ne` | `!= ?` | Not equal |
| `>`  | `=gt=` | `$gt` | `> ?` | Greater than |
| `>=` | `=ge=` | `$gte` | `>= ?` | Greater than or equal |
| `<`  | `=lt=` | `$lt` | `< ?` | Less than |
| `<=` | `=le=` | `$lte` | `<= ?` | Less than or equal |
| `=in=` | — | `$in` | `IN (?, ?)` | In list |
| `=out=` | — | `$nin` | `NOT IN (?, ?)` | Not in list |

### MongoDB-specific extended operators

These require `MongoDbOperators.ALL_OPERATORS` in `ParserConfig.allowedOperators()`:

| RSQL | MongoDB | Notes |
|------|---------|-------|
| `=regex=` | `$regex` | Value: `pattern` or `pattern/flags` |
| `=exists=` | `$exists` | Value: `true` or `false` |
| `=all=` | `$all` | Array contains all specified values |
| `=size=` | `$size` | Array length equals value |
| `=type=` | `$type` | BSON type: `string`, `int`, `long`, `double`, `bool`, `date`, `objectId`, `array`, `null`, etc. |

### Logical operators

| Symbol | Keyword | Precedence | MongoDB | SQL |
|--------|---------|------------|---------|-----|
| `;`    | `and`   | Higher | `$and` | `AND` |
| `,`    | `or`    | Lower  | `$or`  | `OR`  |

### Examples

```
name==Alice                          Simple equality
name!=inactive                       Inequality
age>=18                              Comparison
role=in=(admin,user,guest)           In list
role=out=(banned,suspended)          Not in list
name==Alice*                         Wildcard (prefix match)
name==*Smith                         Wildcard (suffix match)
name==*ali*                          Wildcard (substring match)
name==Alice;age>=18                  AND
status==active,status==pending       OR
(name==Alice,name==Bob);age>=18      Grouped OR + AND
name=="John Doe"                     Quoted value with spaces
a==1;b==2;c==3                       Three-way AND (flattened)

# MongoDB extended
email=exists=true                    Field must exist
tags=all=(java,spring)               Array must contain both
tags=size=3                          Array has exactly 3 elements
name=regex=^Alice/i                  Regex with case-insensitive flag
age=type=int                         Field must be BSON int32
```

---

## MongoDB 8.x Integration

The `MongoDbQueryBuilder` is a built-in `AstVisitor<Bson>` that translates RSQL to
MongoDB filter documents compatible with the **MongoDB Java Driver 5.x** targeting
**MongoDB Server 8.x**.

### Dependency

Add to your project `pom.xml`:

```xml
<!-- RSQL Parser -->
<dependency>
    <groupId>io.github.yamatrireddy</groupId>
    <artifactId>rsql-parser</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- MongoDB Java Driver 5.x (MongoDB 8.x compatible) -->
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>5.3.0</version>
</dependency>
```

### Basic usage

```java
RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());
MongoDbQueryBuilder builder = new MongoDbQueryBuilder();

Bson filter = parser.parse("name==Alice;age>=18").accept(builder);
collection.find(filter).forEach(doc -> System.out.println(doc.toJson()));
```

### With MongoDB-specific operators

```java
ParserConfig config = ParserConfig.builder()
        .allowedOperators(MongoDbOperators.ALL_OPERATORS)
        .build();

RSQLParser parser = RSQLParser.builder()
        .fieldRegistry(FieldRegistry.allowList("name","email","tags","age","status"))
        .config(config)
        .build();

Bson filter = parser.parse("email=exists=true;tags=all=(java,spring);age=type=int")
        .accept(new MongoDbQueryBuilder());
```

### Type inference

Values are automatically converted to the most appropriate BSON type:

| Value string | Java/BSON type |
|---|---|
| `"42"` | `Integer` |
| `"9876543210"` | `Long` |
| `"3.14"` | `Double` |
| `"true"` / `"false"` | `Boolean` |
| `"null"` | `null` |
| `"2024-01-15T10:30:00Z"` | `java.util.Date` (UTC) |
| `"507f1f77bcf86cd799439011"` | `ObjectId` |
| anything else | `String` |
