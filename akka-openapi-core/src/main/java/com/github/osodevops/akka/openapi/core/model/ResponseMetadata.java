package com.github.osodevops.akka.openapi.core.model;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents metadata for an operation response.
 *
 * @since 1.0.0
 */
public final class ResponseMetadata {

    private final String statusCode;
    private final String description;
    private final Type responseType;
    private final String mediaType;
    private final Map<String, Object> examples;

    private ResponseMetadata(Builder builder) {
        this.statusCode = Objects.requireNonNull(builder.statusCode, "statusCode must not be null");
        this.description = builder.description != null ? builder.description : "";
        this.responseType = builder.responseType;
        this.mediaType = builder.mediaType != null ? builder.mediaType : "application/json";
        this.examples = Collections.unmodifiableMap(new LinkedHashMap<>(builder.examples));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the status code (e.g., "200", "404")
     */
    public String getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the response description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the Java type of the response body.
     *
     * @return the response type, or null for no body
     */
    public Type getResponseType() {
        return responseType;
    }

    /**
     * Checks if this response has a body.
     *
     * @return true if response has a body type
     */
    public boolean hasBody() {
        return responseType != null &&
               !responseType.equals(Void.class) &&
               !responseType.equals(void.class);
    }

    /**
     * Gets the media type for the response.
     *
     * @return the media type (default: application/json)
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Gets example values for this response.
     *
     * @return an unmodifiable map of examples
     */
    public Map<String, Object> getExamples() {
        return examples;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseMetadata that = (ResponseMetadata) o;
        return Objects.equals(statusCode, that.statusCode) &&
               Objects.equals(responseType, that.responseType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, responseType);
    }

    @Override
    public String toString() {
        return "ResponseMetadata{" +
               "statusCode='" + statusCode + '\'' +
               ", description='" + description + '\'' +
               ", responseType=" + (responseType != null ? responseType.getTypeName() : "null") +
               '}';
    }

    public static final class Builder {
        private String statusCode;
        private String description;
        private Type responseType;
        private String mediaType = "application/json";
        private Map<String, Object> examples = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder statusCode(String statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder responseType(Type responseType) {
            this.responseType = responseType;
            return this;
        }

        public Builder mediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder examples(Map<String, Object> examples) {
            this.examples = examples != null ? new LinkedHashMap<>(examples) : new LinkedHashMap<>();
            return this;
        }

        public Builder addExample(String name, Object example) {
            if (name != null && example != null) {
                this.examples.put(name, example);
            }
            return this;
        }

        public ResponseMetadata build() {
            return new ResponseMetadata(this);
        }
    }
}
