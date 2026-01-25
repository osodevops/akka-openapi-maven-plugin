package com.github.osodevops.akka.openapi.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents metadata extracted from an HTTP operation method.
 *
 * <p>Contains all information needed to generate an OpenAPI operation
 * for a single HTTP method (GET, POST, PUT, DELETE, etc.).</p>
 *
 * @since 1.0.0
 */
public final class OperationMetadata {

    private final String methodName;
    private final HttpMethod httpMethod;
    private final String path;
    private final String operationId;
    private final String summary;
    private final String description;
    private final List<ParameterMetadata> parameters;
    private final RequestBodyMetadata requestBody;
    private final Map<String, ResponseMetadata> responses;
    private final List<String> tags;
    private final boolean deprecated;

    private OperationMetadata(Builder builder) {
        this.methodName = Objects.requireNonNull(builder.methodName, "methodName must not be null");
        this.httpMethod = Objects.requireNonNull(builder.httpMethod, "httpMethod must not be null");
        this.path = builder.path != null ? builder.path : "";
        this.operationId = builder.operationId != null ? builder.operationId : builder.methodName;
        this.summary = builder.summary != null ? builder.summary : "";
        this.description = builder.description != null ? builder.description : "";
        this.parameters = Collections.unmodifiableList(new ArrayList<>(builder.parameters));
        this.requestBody = builder.requestBody;
        this.responses = Collections.unmodifiableMap(new LinkedHashMap<>(builder.responses));
        this.tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        this.deprecated = builder.deprecated;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the Java method name.
     *
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the HTTP method type.
     *
     * @return the HTTP method
     */
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Gets the path for this operation (relative to endpoint base path).
     *
     * @return the path (e.g., "/{id}")
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the unique operation ID for OpenAPI.
     *
     * @return the operation ID
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * Gets the operation summary (typically first line of JavaDoc).
     *
     * @return the summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Gets the full operation description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the operation parameters (path, query, header).
     *
     * @return an unmodifiable list of parameters
     */
    public List<ParameterMetadata> getParameters() {
        return parameters;
    }

    /**
     * Gets the request body metadata.
     *
     * @return the request body, or null if none
     */
    public RequestBodyMetadata getRequestBody() {
        return requestBody;
    }

    /**
     * Checks if this operation has a request body.
     *
     * @return true if request body is present
     */
    public boolean hasRequestBody() {
        return requestBody != null;
    }

    /**
     * Gets the response definitions keyed by status code.
     *
     * @return an unmodifiable map of responses
     */
    public Map<String, ResponseMetadata> getResponses() {
        return responses;
    }

    /**
     * Gets the tags for this operation.
     *
     * @return an unmodifiable list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Checks if this operation is deprecated.
     *
     * @return true if deprecated
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationMetadata that = (OperationMetadata) o;
        return deprecated == that.deprecated &&
               Objects.equals(methodName, that.methodName) &&
               httpMethod == that.httpMethod &&
               Objects.equals(path, that.path) &&
               Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, httpMethod, path, operationId, deprecated);
    }

    @Override
    public String toString() {
        return "OperationMetadata{" +
               "httpMethod=" + httpMethod +
               ", path='" + path + '\'' +
               ", operationId='" + operationId + '\'' +
               ", deprecated=" + deprecated +
               '}';
    }

    /**
     * HTTP method types supported by Akka SDK.
     */
    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }

    public static final class Builder {
        private String methodName;
        private HttpMethod httpMethod;
        private String path = "";
        private String operationId;
        private String summary;
        private String description;
        private List<ParameterMetadata> parameters = new ArrayList<>();
        private RequestBodyMetadata requestBody;
        private Map<String, ResponseMetadata> responses = new LinkedHashMap<>();
        private List<String> tags = new ArrayList<>();
        private boolean deprecated;

        private Builder() {
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder httpMethod(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder operationId(String operationId) {
            this.operationId = operationId;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parameters(List<ParameterMetadata> parameters) {
            this.parameters = parameters != null ? new ArrayList<>(parameters) : new ArrayList<>();
            return this;
        }

        public Builder addParameter(ParameterMetadata parameter) {
            if (parameter != null) {
                this.parameters.add(parameter);
            }
            return this;
        }

        public Builder requestBody(RequestBodyMetadata requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder responses(Map<String, ResponseMetadata> responses) {
            this.responses = responses != null ? new LinkedHashMap<>(responses) : new LinkedHashMap<>();
            return this;
        }

        public Builder addResponse(String statusCode, ResponseMetadata response) {
            if (statusCode != null && response != null) {
                this.responses.put(statusCode, response);
            }
            return this;
        }

        public Builder addResponse(ResponseMetadata response) {
            if (response != null && response.getStatusCode() != null) {
                this.responses.put(response.getStatusCode(), response);
            }
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
            return this;
        }

        public Builder addTag(String tag) {
            if (tag != null && !tag.isBlank()) {
                this.tags.add(tag);
            }
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public OperationMetadata build() {
            return new OperationMetadata(this);
        }
    }
}
