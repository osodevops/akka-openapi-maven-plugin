package com.github.osodevops.akka.openapi.core.model;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Represents metadata for an operation parameter.
 *
 * <p>Covers path parameters, query parameters, and header parameters.</p>
 *
 * @since 1.0.0
 */
public final class ParameterMetadata {

    private final String name;
    private final ParameterLocation location;
    private final Type javaType;
    private final String description;
    private final boolean required;
    private final Object defaultValue;
    private final Object example;
    private final String format;

    private ParameterMetadata(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name must not be null");
        this.location = Objects.requireNonNull(builder.location, "location must not be null");
        this.javaType = Objects.requireNonNull(builder.javaType, "javaType must not be null");
        this.description = builder.description != null ? builder.description : "";
        this.required = builder.required;
        this.defaultValue = builder.defaultValue;
        this.example = builder.example;
        this.format = builder.format;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the parameter name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the parameter location.
     *
     * @return the location (path, query, header, cookie)
     */
    public ParameterLocation getLocation() {
        return location;
    }

    /**
     * Gets the Java type of this parameter.
     *
     * @return the Java type
     */
    public Type getJavaType() {
        return javaType;
    }

    /**
     * Gets the parameter description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this parameter is required.
     *
     * @return true if required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Gets the default value for this parameter.
     *
     * @return the default value, or null if none
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Gets an example value for this parameter.
     *
     * @return the example, or null if none
     */
    public Object getExample() {
        return example;
    }

    /**
     * Gets the OpenAPI format hint.
     *
     * @return the format (e.g., "int32", "date-time"), or null
     */
    public String getFormat() {
        return format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterMetadata that = (ParameterMetadata) o;
        return required == that.required &&
               Objects.equals(name, that.name) &&
               location == that.location &&
               Objects.equals(javaType, that.javaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, location, javaType, required);
    }

    @Override
    public String toString() {
        return "ParameterMetadata{" +
               "name='" + name + '\'' +
               ", location=" + location +
               ", javaType=" + javaType.getTypeName() +
               ", required=" + required +
               '}';
    }

    /**
     * Parameter location in the HTTP request.
     */
    public enum ParameterLocation {
        PATH("path"),
        QUERY("query"),
        HEADER("header"),
        COOKIE("cookie");

        private final String openApiValue;

        ParameterLocation(String openApiValue) {
            this.openApiValue = openApiValue;
        }

        /**
         * Gets the OpenAPI "in" value.
         *
         * @return the OpenAPI value
         */
        public String getOpenApiValue() {
            return openApiValue;
        }
    }

    public static final class Builder {
        private String name;
        private ParameterLocation location;
        private Type javaType;
        private String description;
        private boolean required;
        private Object defaultValue;
        private Object example;
        private String format;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder location(ParameterLocation location) {
            this.location = location;
            return this;
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

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder example(Object example) {
            this.example = example;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public ParameterMetadata build() {
            return new ParameterMetadata(this);
        }
    }
}
