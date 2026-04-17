package io.github.yamatrireddy.exception;

/**
 * Thrown when a syntactically valid RSQL expression violates a security or
 * semantic constraint — e.g., disallowed field, unknown operator, depth limit
 * exceeded, or value length exceeded.
 */
public class RSQLValidationException extends RSQLException {

    private static final long serialVersionUID = 1L;

    public RSQLValidationException(String message) {
        super(message);
    }

    public RSQLValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
