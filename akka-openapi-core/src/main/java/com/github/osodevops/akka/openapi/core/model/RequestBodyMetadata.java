package com.github.osodevops.akka.openapi.core.model;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Represents metadata for an operation's request body.
 *
 * @since 1.0.0
 */
public final class RequestBodyMetadata {

    private final Type javaType;
    private final String description;
    private final boolean required;
    private final String mediaType;
    private final Object example;

    private RequestBodyMetadata(Builder builder) {
        this.javaType = Objects.requireNonNull(builder.javaType, "javaType must not be null");
        this.description = builder.description != null ? builder.description : "";
        this.required = builder.required;
        this.mediaType = builder.mediaType != null ? builder.mediaType : "application/json";
        this.example = builder.example;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the Java type of the request body.
     *
     * @return the Java type
     */
    public Type getJavaType() {
        return javaType;
    }

    /**
     * Gets the request body description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if the request body is required.
     *
     * @return true if required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Gets the media type for the request body.
     *
     * @return the media type (default: application/json)
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Gets an example value for the request body.
     *
     * @return the example, or null if none
     */
    public Object getExample() {
        return example;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestBodyMetadata that = (RequestBodyMetadata) o;
        return required == that.required &&
               Objects.equals(javaType, that.javaType) &&
               Objects.equals(mediaType, that.mediaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaType, required, mediaType);
    }

    @Override
    public String toString() {
        return "RequestBodyMetadata{" +
               "javaType=" + javaType.getTypeName() +
               ", required=" + required +
               ", mediaType='" + mediaType + '\'' +
               '}';
    }

    public static final class Builder {
        private Type javaType;
        private String description;
        private boolean required = true;
        private String mediaType = "application/json";
        private Object example;

        private Builder() {
        }

        public Builder javaType(Type javaType) {
            this.javaType = javaType;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder mediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder example(Object example) {
            this.example = example;
            return this;
        }

        public RequestBodyMetadata build() {
            return new RequestBodyMetadata(this);
        }
    }
}
