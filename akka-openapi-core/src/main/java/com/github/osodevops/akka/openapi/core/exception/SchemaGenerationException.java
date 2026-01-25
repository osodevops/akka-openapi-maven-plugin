package com.github.osodevops.akka.openapi.core.exception;

/**
 * Exception thrown when schema generation fails.
 *
 * @since 1.0.0
 */
public class SchemaGenerationException extends OpenAPIGenerationException {

    /**
     * Creates a new schema generation exception with the specified message.
     *
     * @param message the error message
     */
    public SchemaGenerationException(String message) {
        super(message);
    }

    /**
     * Creates a new schema generation exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public SchemaGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
