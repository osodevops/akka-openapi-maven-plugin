package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response format.
 *
 * <p>Provides consistent error information for API error responses including
 * status codes, messages, and optional field-level validation errors.</p>
 */
public class ErrorResponse {

    /**
     * The HTTP status code.
     */
    private int status;

    /**
     * The error type or code.
     */
    private String error;

    /**
     * A human-readable error message.
     */
    private String message;

    /**
     * The request path that caused the error.
     */
    private String path;

    /**
     * The timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * List of field-level validation errors.
     */
    @JsonProperty("validationErrors")
    private List<FieldError> validationErrors;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<FieldError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<FieldError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Represents a field-level validation error.
     */
    public static class FieldError {

        /**
         * The field name that has the error.
         */
        private String field;

        /**
         * The rejected value.
         */
        @JsonProperty("rejectedValue")
        private Object rejectedValue;

        /**
         * The error message for this field.
         */
        private String message;

        public FieldError() {
        }

        public FieldError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
