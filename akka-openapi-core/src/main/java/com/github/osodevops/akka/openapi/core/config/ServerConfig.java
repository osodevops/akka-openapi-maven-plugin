package com.github.osodevops.akka.openapi.core.config;

import java.util.Objects;

/**
 * Configuration for an OpenAPI server entry.
 *
 * @since 1.0.0
 */
public class ServerConfig {

    private String url;
    private String description;

    /**
     * Default constructor for Maven configuration injection.
     */
    public ServerConfig() {
    }

    /**
     * Creates a new server configuration.
     *
     * @param url the server URL
     * @param description the server description
     */
    public ServerConfig(String url, String description) {
        this.url = Objects.requireNonNull(url, "url must not be null");
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerConfig that = (ServerConfig) o;
        return Objects.equals(url, that.url) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, description);
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "url='" + url + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private String description;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(url, description);
        }
    }
}
