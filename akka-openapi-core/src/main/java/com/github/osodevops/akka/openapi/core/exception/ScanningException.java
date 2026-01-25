package com.github.osodevops.akka.openapi.core.exception;

/**
 * Exception thrown when classpath scanning fails.
 *
 * @since 1.0.0
 */
public class ScanningException extends OpenAPIGenerationException {

    /**
     * Creates a new scanning exception with the specified message.
     *
     * @param message the error message
     */
    public ScanningException(String message) {
        super(message);
    }

    /**
     * Creates a new scanning exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ScanningException(String message, Throwable cause) {
        super(message, cause);
    }
}
