package io.github.yamatrireddy.api;

import io.github.yamatrireddy.Parser;
import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.exception.RSQLParseException;
import io.github.yamatrireddy.exception.RSQLValidationException;
import io.github.yamatrireddy.lexer.Lexer;
import io.github.yamatrireddy.optimizer.AstOptimizer;
import io.github.yamatrireddy.security.AstValidator;
import io.github.yamatrireddy.security.FieldRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Primary entry point for parsing RSQL filter expressions.
 *
 * <p>The pipeline for each {@link #parse(String)} call is:
 * <ol>
 *   <li>{@link Lexer} — tokenise the input string.</li>
 *   <li>{@link Parser} — build a raw AST via recursive descent.</li>
 *   <li>{@link AstValidator} — validate security constraints (depth, value length,
 *       wildcard count, allowed fields and operators).</li>
 *   <li>{@link AstOptimizer} — flatten nested same-operator nodes and eliminate
 *       single-child logical wrappers.</li>
 * </ol>
 *
 * <h3>Quick start</h3>
 * <pre>{@code
 * // Allow any field, default security limits
 * RSQLParser parser = new RSQLParser(FieldRegistry.allowAll());
 * RSQLExpression expr = parser.parse("name==Alice;age>=18");
 *
 * // Restrict to a known field set with custom limits
 * RSQLParser strict = RSQLParser.builder()
 *         .fieldRegistry(FieldRegistry.allowList("name", "age", "role"))
 *         .config(ParserConfig.builder().maxDepth(5).build())
 *         .build();
 * }</pre>
 *
 * <p>Instances are thread-safe and should be reused across requests.
 *
 * @see FieldRegistry
 * @see ParserConfig
 * @see RSQLExpression
 */
public final class RSQLParser {

    private static final Logger log = LoggerFactory.getLogger(RSQLParser.class);

    private final FieldRegistry fieldRegistry;
    private final ParserConfig config;

    /**
     * Creates a parser with the given field registry and default security limits.
     *
     * @param fieldRegistry field-name validation strategy (non-null)
     */
    public RSQLParser(FieldRegistry fieldRegistry) {
        this(fieldRegistry, ParserConfig.defaults());
    }

    /**
     * Creates a parser with explicit field registry and security configuration.
     *
     * @param fieldRegistry field-name validation strategy (non-null)
     * @param config        security and limit configuration (non-null)
     */
    public RSQLParser(FieldRegistry fieldRegistry, ParserConfig config) {
        this.fieldRegistry = Objects.requireNonNull(fieldRegistry, "fieldRegistry must not be null");
        this.config        = Objects.requireNonNull(config, "config must not be null");
    }

    /** Returns a {@link Builder} for fluent construction. */
    public static Builder builder() {
        return new Builder();
    }

    // ── Core parse method ────────────────────────────────────────────────────

    /**
     * Parses an RSQL expression and returns the validated, optimised AST.
     *
     * @param input the RSQL filter string (non-null, non-blank)
     * @return the parsed expression
     * @throws NullPointerException     if {@code input} is null
     * @throws RSQLParseException       if the input has syntax errors
     * @throws RSQLValidationException  if validation constraints are violated
     */
    public RSQLExpression parse(String input) {
        Objects.requireNonNull(input, "input must not be null");

        if (input.isBlank()) {
            throw new RSQLParseException("Input must not be blank", 0, input);
        }
        if (input.length() > config.getMaxInputLength()) {
            throw new RSQLValidationException(
                    "Input length " + input.length()
                    + " exceeds maximum of " + config.getMaxInputLength());
        }

        log.debug("Parsing RSQL: {}", input);

        AstNode raw       = new Parser(new Lexer(input), input).parse();
        new AstValidator(fieldRegistry, config).validate(raw, 0);
        AstNode optimised = new AstOptimizer().optimize(raw);

        log.debug("Parsed AST: {}", optimised);
        return new RSQLExpression(optimised);
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static final class Builder {

        private FieldRegistry fieldRegistry;
        private ParserConfig config = ParserConfig.defaults();

        private Builder() {}

        public Builder fieldRegistry(FieldRegistry fieldRegistry) {
            this.fieldRegistry = Objects.requireNonNull(fieldRegistry);
            return this;
        }

        public Builder config(ParserConfig config) {
            this.config = Objects.requireNonNull(config);
            return this;
        }

        public RSQLParser build() {
            Objects.requireNonNull(fieldRegistry,
                    "fieldRegistry must be set — use FieldRegistry.allowAll() to permit any field");
            return new RSQLParser(fieldRegistry, config);
        }
    }
}
