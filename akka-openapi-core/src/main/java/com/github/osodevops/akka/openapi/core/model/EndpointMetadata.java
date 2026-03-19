package com.github.osodevops.akka.openapi.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents metadata extracted from an Akka SDK HTTP endpoint class.
 *
 * <p>This is an immutable value object containing all information needed
 * to generate OpenAPI paths for a single endpoint class.</p>
 *
 * @since 1.0.0
 */
public final class EndpointMetadata {

    private final String className;
    private final String basePath;
    private final String description;
    private final List<String> tags;
    private final List<TagMetadata> tagMetadata;
    private final List<OperationMetadata> operations;
    private final SecurityMetadata security;
    private final InfoMetadata infoMetadata;
    private final List<ServerMetadata> serverMetadata;

    private EndpointMetadata(Builder builder) {
        this.className = Objects.requireNonNull(builder.className, "className must not be null");
        this.basePath = Objects.requireNonNull(builder.basePath, "basePath must not be null");
        this.description = builder.description != null ? builder.description : "";
        this.tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        this.tagMetadata = Collections.unmodifiableList(new ArrayList<>(builder.tagMetadata));
        this.operations = Collections.unmodifiableList(new ArrayList<>(builder.operations));
        this.security = builder.security;
        this.infoMetadata = builder.infoMetadata;
        this.serverMetadata = Collections.unmodifiableList(new ArrayList<>(builder.serverMetadata));
    }

    /**
     * Creates a new builder for EndpointMetadata.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the fully qualified class name of the endpoint.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the simple class name (without package).
     *
     * @return the simple class name
     */
    public String getSimpleClassName() {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    /**
     * Gets the base path for all operations in this endpoint.
     *
     * @return the base path (e.g., "/customers")
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * Gets the description of this endpoint.
     *
     * @return the description, or empty string if not provided
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the tags for grouping operations in this endpoint.
     *
     * @return an unmodifiable list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Gets detailed tag metadata including descriptions and external docs.
     *
     * @return an unmodifiable list of tag metadata
     */
    public List<TagMetadata> getTagMetadata() {
        return tagMetadata;
    }

    /**
     * Gets all operations defined in this endpoint.
     *
     * @return an unmodifiable list of operations
     */
    public List<OperationMetadata> getOperations() {
        return operations;
    }

    /**
     * Gets the security metadata for this endpoint.
     *
     * @return the security metadata, or null if not defined
     */
    public SecurityMetadata getSecurity() {
        return security;
    }

    /**
     * Gets the API info metadata from @OpenAPIInfo annotation.
     *
     * @return the info metadata, or null if not annotated
     */
    public InfoMetadata getInfoMetadata() {
        return infoMetadata;
    }

    /**
     * Gets the server metadata from @OpenAPIServer annotations.
     *
     * @return an unmodifiable list of server metadata
     */
    public List<ServerMetadata> getServerMetadata() {
        return serverMetadata;
    }

    /**
     * Checks if this endpoint has any operations.
     *
     * @return true if there are operations
     */
    public boolean hasOperations() {
        return !operations.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointMetadata that = (EndpointMetadata) o;
        return Objects.equals(className, that.className) &&
               Objects.equals(basePath, that.basePath) &&
               Objects.equals(description, that.description) &&
               Objects.equals(tags, that.tags) &&
               Objects.equals(tagMetadata, that.tagMetadata) &&
               Objects.equals(operations, that.operations) &&
               Objects.equals(security, that.security);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, basePath, description, tags, tagMetadata, operations, security);
    }

    @Override
    public String toString() {
        return "EndpointMetadata{" +
               "className='" + className + '\'' +
               ", basePath='" + basePath + '\'' +
               ", operations=" + operations.size() +
               ", tags=" + tags +
               '}';
    }

    /**
     * Builder for creating EndpointMetadata instances.
     */
    public static final class Builder {
        private String className;
        private String basePath;
        private String description;
        private List<String> tags = new ArrayList<>();
        private List<TagMetadata> tagMetadata = new ArrayList<>();
        private List<OperationMetadata> operations = new ArrayList<>();
        private SecurityMetadata security;
        private InfoMetadata infoMetadata;
        private List<ServerMetadata> serverMetadata = new ArrayList<>();

        private Builder() {
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public Builder tagMetadata(List<TagMetadata> tagMetadata) {
            this.tagMetadata = tagMetadata != null ? new ArrayList<>(tagMetadata) : new ArrayList<>();
            return this;
        }

        public Builder addTagMetadata(TagMetadata metadata) {
            if (metadata != null) {
                this.tagMetadata.add(metadata);
            }
            return this;
        }

        public Builder operations(List<OperationMetadata> operations) {
            this.operations = operations != null ? new ArrayList<>(operations) : new ArrayList<>();
            return this;
        }

        public Builder addOperation(OperationMetadata operation) {
            if (operation != null) {
                this.operations.add(operation);
            }
            return this;
        }

        public Builder security(SecurityMetadata security) {
            this.security = security;
            return this;
        }

        public Builder infoMetadata(InfoMetadata infoMetadata) {
            this.infoMetadata = infoMetadata;
            return this;
        }

        public Builder serverMetadata(List<ServerMetadata> serverMetadata) {
            this.serverMetadata = serverMetadata != null ? new ArrayList<>(serverMetadata) : new ArrayList<>();
            return this;
        }

        public Builder addServerMetadata(ServerMetadata server) {
            if (server != null) {
                this.serverMetadata.add(server);
            }
            return this;
        }

        public EndpointMetadata build() {
            return new EndpointMetadata(this);
        }
    }
}
