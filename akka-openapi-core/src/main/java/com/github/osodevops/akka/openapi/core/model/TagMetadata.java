package com.github.osodevops.akka.openapi.core.model;

import java.util.Objects;

/**
 * Represents metadata for an OpenAPI tag with description and external docs.
 *
 * @since 1.1.0
 */
public final class TagMetadata {

    private final String name;
    private final String description;
    private final String externalDocsUrl;
    private final String externalDocsDescription;

    public TagMetadata(String name, String description, String externalDocsUrl, String externalDocsDescription) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description != null ? description : "";
        this.externalDocsUrl = externalDocsUrl != null ? externalDocsUrl : "";
        this.externalDocsDescription = externalDocsDescription != null ? externalDocsDescription : "";
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getExternalDocsUrl() {
        return externalDocsUrl;
    }

    public String getExternalDocsDescription() {
        return externalDocsDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagMetadata that = (TagMetadata) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "TagMetadata{name='" + name + "', description='" + description + "'}";
    }
}
