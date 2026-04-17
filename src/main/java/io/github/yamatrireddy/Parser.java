package io.github.yamatrireddy;

import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.exception.RSQLParseException;
import io.github.yamatrireddy.lexer.Lexer;
import io.github.yamatrireddy.lexer.Token;
import io.github.yamatrireddy.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser that converts a token stream from {@link Lexer} into
 * an {@link AstNode} tree.
 *
 * <p>Grammar (simplified EBNF):
 * <pre>
 *   expression  ::= or
 *   or          ::= and (OR and)*
 *   and         ::= constraint (AND constraint)*
 *   constraint  ::= '(' expression ')' | comparison
 *   comparison  ::= VALUE OPERATOR VALUE
 *                 | VALUE OPERATOR '(' VALUE (',' VALUE)* ')'
 * </pre>
 *
 * <p>This class is not thread-safe and is designed for single-use per input string.
 * Use the public {@link io.github.yamatrireddy.api.RSQLParser} facade instead of
 * instantiating this class directly.
 */
public final class Parser {

    private Token cur;
    private final Lexer lexer;
    private final String input;

    public Parser(Lexer lexer, String input) {
        this.lexer = lexer;
        this.input = input;
        this.cur = this.lexer.next();
    }

    /** Parse the full expression and return the root AST node. */
    public AstNode parse() {
        AstNode node = or();
        if (cur.getType() != TokenType.EOF) {
            throw new RSQLParseException(
                    "Unexpected token '" + cur.getValue() + "' after expression",
                    cur.getPosition(), input);
        }
        return node;
    }

    // ── Grammar rules ────────────────────────────────────────────────────────

    private AstNode or() {
        AstNode left = and();
        if (cur.getType() != TokenType.OR) {
            return left;
        }
        List<AstNode> children = new ArrayList<>();
        children.add(left);
        while (cur.getType() == TokenType.OR) {
            eat(TokenType.OR);
            children.add(and());
        }
        return new LogicalNode("OR", children);
    }

    private AstNode and() {
        AstNode left = constraint();
        if (cur.getType() != TokenType.AND) {
            return left;
        }
        List<AstNode> children = new ArrayList<>();
        children.add(left);
        while (cur.getType() == TokenType.AND) {
            eat(TokenType.AND);
            children.add(constraint());
        }
        return new LogicalNode("AND", children);
    }

    private AstNode constraint() {
        if (cur.getType() == TokenType.L_PAREN) {
            eat(TokenType.L_PAREN);
            AstNode node = or();
            eat(TokenType.R_PAREN);
            return node;
        }
        return comparison();
    }

    private AstNode comparison() {
        String field    = eat(TokenType.VALUE).getValue();
        String operator = eat(TokenType.OPERATOR).getValue();

        List<String> values = new ArrayList<>();
        if (cur.getType() == TokenType.L_PAREN) {
            // Multi-value form: field=op=(v1,v2,...)
            eat(TokenType.L_PAREN);
            values.add(eat(TokenType.VALUE).getValue());
            while (cur.getType() == TokenType.OR) {
                eat(TokenType.OR);
                values.add(eat(TokenType.VALUE).getValue());
            }
            eat(TokenType.R_PAREN);
        } else {
            values.add(eat(TokenType.VALUE).getValue());
        }
        return new ComparisonNode(field, operator, values);
    }

    // ── Token consumption ────────────────────────────────────────────────────

    private Token eat(TokenType expected) {
        if (cur.getType() != expected) {
            throw new RSQLParseException(
                    "Expected " + expected + " but got " + cur.getType()
                            + " ('" + cur.getValue() + "')",
                    cur.getPosition(), input);
        }
        Token consumed = cur;
        cur = lexer.next();
        return consumed;
    }
}
