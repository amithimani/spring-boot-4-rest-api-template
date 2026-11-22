package me.dio.exception;

import java.time.Duration;

/**
 * An upstream service cannot be used right now (down, timing out, rate limiting us,
 * or its circuit breaker is open). Mapped to HTTP 503.
 */
public class ExternalServiceUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Duration retryAfter;

    public ExternalServiceUnavailableException(String message, Duration retryAfter, Throwable cause) {
        super(message, cause);
        this.retryAfter = retryAfter;
    }

    /** Suggested wait before retrying, or {@code null} if unknown. */
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
