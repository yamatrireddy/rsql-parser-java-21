package io.github.yamatrireddy.lexer;

import java.util.Objects;

/**
 * An immutable lexer token produced by {@link Lexer}.
 *
 * <p>Each token carries its {@link TokenType type}, the raw {@link #getValue() value}
 * string, and the zero-based {@link #getPosition() position} at which it begins in
 * the original input — used for precise error reporting.
 */
public final class Token {

    private final TokenType type;
    private final String value;
    private final int position;

    public Token(TokenType type, String value, int position) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = value != null ? value : "";
        this.position = position;
    }

    /** The semantic category of this token. */
    public TokenType getType() {
        return type;
    }

    /** The raw text this token was scanned from. */
    public String getValue() {
        return value;
    }

    /** Zero-based index of the first character of this token in the original input. */
    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Token{type=" + type + ", value='" + value + "', position=" + position + '}';
    }
}
