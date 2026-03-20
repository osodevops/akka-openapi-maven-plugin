package com.github.osodevops.akka.openapi.core.model;

import java.util.Objects;

/**
 * Represents server metadata extracted from @OpenAPIServer annotations.
 *
 * @since 1.1.0
 */
public final class ServerMetadata {

    private final String url;
    private final String description;

    public ServerMetadata(String url, String description) {
        this.url = Objects.requireNonNull(url, "url must not be null");
        this.description = description != null ? description : "";
    }

    public String getUrl() { return url; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerMetadata that = (ServerMetadata) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "ServerMetadata{url='" + url + "', description='" + description + "'}";
    }
}
