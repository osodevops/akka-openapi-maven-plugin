package com.github.osodevops.akka.openapi.core.exception;

/**
 * Base exception for OpenAPI generation errors.
 *
 * @since 1.0.0
 */
public class OpenAPIGenerationException extends RuntimeException {

    /**
     * Creates a new exception with the specified message.
     *
     * @param message the error message
     */
    public OpenAPIGenerationException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public OpenAPIGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
