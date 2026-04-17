package io.github.yamatrireddy.exception;

/**
 * Thrown when the lexer or parser encounters a syntax error in the RSQL input.
 *
 * <p>Provides the character {@link #getPosition() position} and the original
 * {@link #getInput() input string} so callers can produce precise diagnostics.
 */
public class RSQLParseException extends RSQLException {

    private static final long serialVersionUID = 1L;

    private final int position;
    private final String input;

    public RSQLParseException(String message, int position, String input) {
        super(buildMessage(message, position, input));
        this.position = position;
        this.input = input;
    }

    public RSQLParseException(String message, int position, String input, Throwable cause) {
        super(buildMessage(message, position, input), cause);
        this.position = position;
        this.input = input;
    }

    /** Zero-based character offset in the input where the error was detected. */
    public int getPosition() {
        return position;
    }

    /** The original RSQL input string that caused the error. */
    public String getInput() {
        return input;
    }

    private static String buildMessage(String message, int position, String input) {
        return String.format("%s — position %d in: %s", message, position, input);
    }
}
