package me.dio.exception;

/**
 * An upstream service answered, but with something we cannot use (e.g. it rejected
 * our request or returned an invalid body). Retrying will not help. Mapped to HTTP 502.
 */
public class ExternalServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
