package io.github.yamatrireddy.lexer;

import io.github.yamatrireddy.exception.RSQLParseException;

import java.util.Objects;

/**
 * Hand-written lexer that tokenises a single RSQL expression string.
 *
 * <p>The lexer is single-pass and not thread-safe. Create a new instance per
 * parse invocation. Call {@link #next()} repeatedly until a token with type
 * {@link TokenType#EOF} is returned.
 *
 * <h3>Supported syntax</h3>
 * <ul>
 *   <li>Values — bare identifiers, single-quoted, or double-quoted strings
 *       with {@code \} escape sequences.</li>
 *   <li>Comparison operators — {@code ==}, {@code !=}, {@code >}, {@code >=},
 *       {@code <}, {@code <=}, {@code =in=}, {@code =out=},
 *       {@code =gt=}, {@code =ge=}, {@code =lt=}, {@code =le=}.</li>
 *   <li>Logical AND — {@code ;} or the keyword {@code and}.</li>
 *   <li>Logical OR  — {@code ,} or the keyword {@code or}.</li>
 *   <li>Grouping    — {@code (} and {@code )}.</li>
 * </ul>
 */
public final class Lexer {

    private final String input;
    private int pos;

    public Lexer(String input) {
        this.input = Objects.requireNonNull(input, "input must not be null");
    }

    // ── public API ──────────────────────────────────────────────────────────

    /**
     * Returns the next {@link Token}. When input is exhausted every subsequent
     * call returns a {@link TokenType#EOF} token at the end position.
     *
     * @throws RSQLParseException on unterminated strings or unexpected characters
     */
    public Token next() {
        skipWhitespace();

        if (pos >= input.length()) {
            return new Token(TokenType.EOF, "", pos);
        }

        char c = input.charAt(pos);

        // Grouping
        if (c == '(') return take(TokenType.L_PAREN, "(");
        if (c == ')') return take(TokenType.R_PAREN, ")");

        // Short-form logical operators
        if (c == ';') { int p = pos++; return new Token(TokenType.AND, ";", p); }
        if (c == ',') { int p = pos++; return new Token(TokenType.OR,  ",", p); }

        // Quoted string literals
        if (c == '"')  return quoted('"');
        if (c == '\'') return quoted('\'');

        // Named / symbolic comparison operators — longest-match first
        if (match("=in="))  return op("=in=",  4);
        if (match("=out=")) return op("=out=", 5);
        if (match("=gt="))  return op(">",     4);
        if (match("=ge="))  return op(">=",    4);
        if (match("=lt="))  return op("<",     4);
        if (match("=le="))  return op("<=",    4);
        if (match("=="))    return op("==",    2);
        if (match("!="))    return op("!=",    2);
        if (match(">="))    return op(">=",    2);
        if (match("<="))    return op("<=",    2);
        if (match(">"))     return op(">",     1);
        if (match("<"))     return op("<",     1);

        // Keyword logical operators
        if (matchWord("and")) { int p = pos; pos += 3; return new Token(TokenType.AND, "and", p); }
        if (matchWord("or"))  { int p = pos; pos += 2; return new Token(TokenType.OR,  "or",  p); }

        // Bare value / field name
        return value();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }

    private Token quoted(char quote) {
        int startPos = pos++;   // advance past opening quote
        StringBuilder sb = new StringBuilder();

        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (c == '\\' && pos + 1 < input.length()) {
                sb.append(input.charAt(pos + 1));
                pos += 2;
                continue;
            }

            if (c == quote) {
                pos++;          // advance past closing quote
                return new Token(TokenType.VALUE, sb.toString(), startPos);
            }

            sb.append(c);
            pos++;
        }

        throw new RSQLParseException(
                "Unterminated string literal starting with '" + quote + "'",
                startPos, input);
    }

    private Token value() {
        int start = pos;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if ("()=<>!;,".indexOf(c) >= 0 || Character.isWhitespace(c)) break;
            pos++;
        }
        if (pos == start) {
            throw new RSQLParseException(
                    "Unexpected character '" + input.charAt(pos) + "'",
                    pos, input);
        }
        return new Token(TokenType.VALUE, input.substring(start, pos), start);
    }

    private boolean match(String s) {
        return input.startsWith(s, pos);
    }

    /** Matches a keyword only when followed by whitespace, punctuation, or end-of-input. */
    private boolean matchWord(String w) {
        int end = pos + w.length();
        if (end > input.length()) return false;
        if (!input.regionMatches(pos, w, 0, w.length())) return false;
        if (end == input.length()) return true;
        char next = input.charAt(end);
        return Character.isWhitespace(next) || next == '(' || next == ')' || next == ';' || next == ',';
    }

    private Token take(TokenType type, String s) {
        int p = pos;
        pos += s.length();
        return new Token(type, s, p);
    }

    private Token op(String canonical, int consumed) {
        int p = pos;
        pos += consumed;
        return new Token(TokenType.OPERATOR, canonical, p);
    }
}
