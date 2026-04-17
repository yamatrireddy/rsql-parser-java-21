/**
 * RSQL Parser — a secure, enterprise-grade library for parsing RSQL/FIQL
 * filter expressions into an Abstract Syntax Tree.
 *
 * <h2>Exported packages</h2>
 * <ul>
 *   <li>{@link io.github.yamatrireddy.api} — primary entry point
 *       ({@link io.github.yamatrireddy.api.RSQLParser},
 *        {@link io.github.yamatrireddy.api.RSQLExpression},
 *        {@link io.github.yamatrireddy.api.ParserConfig})</li>
 *   <li>{@link io.github.yamatrireddy.ast} — sealed AST node hierarchy and
 *       the {@link io.github.yamatrireddy.ast.AstVisitor} interface</li>
 *   <li>{@link io.github.yamatrireddy.visitor} — built-in visitor
 *       implementations ({@link io.github.yamatrireddy.visitor.AstPrinter})</li>
 *   <li>{@link io.github.yamatrireddy.exception} — public exception types</li>
 *   <li>{@link io.github.yamatrireddy.security} — field and operator registries</li>
 * </ul>
 *
 * <p>Internal packages ({@code lexer}, {@code optimizer}, and the root parser
 * class) are <em>not</em> exported and are subject to change without notice.
 */
module io.github.yamatrireddy.rsql {

    requires org.slf4j;

    // Public API surface
    exports io.github.yamatrireddy.api;
    exports io.github.yamatrireddy.ast;
    exports io.github.yamatrireddy.visitor;
    exports io.github.yamatrireddy.exception;
    exports io.github.yamatrireddy.security;

    // Internal packages deliberately NOT exported:
    //   io.github.yamatrireddy        (Parser)
    //   io.github.yamatrireddy.lexer
    //   io.github.yamatrireddy.optimizer
}
