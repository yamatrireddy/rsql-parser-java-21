package io.github.yamatrireddy.exception;

/**
 * Base unchecked exception for all RSQL parsing and validation errors.
 *
 * <p>Callers may catch this single type to handle all RSQL failures, or catch
 * the more specific subtypes {@link RSQLParseException} and
 * {@link RSQLValidationException} for finer-grained error handling.
 */
public class RSQLException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RSQLException(String message) {
        super(message);
    }

    public RSQLException(String message, Throwable cause) {
        super(message, cause);
    }
}
