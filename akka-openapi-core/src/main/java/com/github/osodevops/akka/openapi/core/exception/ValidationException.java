package com.github.osodevops.akka.openapi.core.exception;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when OpenAPI specification validation fails.
 *
 * @since 1.0.0
 */
public class ValidationException extends OpenAPIGenerationException {

    private final List<String> validationErrors;

    /**
     * Creates a new validation exception with the specified errors.
     *
     * @param validationErrors the list of validation errors
     */
    public ValidationException(List<String> validationErrors) {
        super("OpenAPI validation failed with " + validationErrors.size() + " error(s)");
        this.validationErrors = Collections.unmodifiableList(validationErrors);
    }

    /**
     * Creates a new validation exception with a message and errors.
     *
     * @param message the error message
     * @param validationErrors the list of validation errors
     */
    public ValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = Collections.unmodifiableList(validationErrors);
    }

    /**
     * Gets the list of validation errors.
     *
     * @return the validation errors
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
