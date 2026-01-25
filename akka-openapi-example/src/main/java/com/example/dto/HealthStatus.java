package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the health status of the application.
 *
 * <p>Provides information about the overall application health and the status
 * of individual components or dependencies.</p>
 */
public class HealthStatus {

    /**
     * Possible health states.
     */
    public enum Status {
        /**
         * The service is healthy and operating normally.
         */
        UP,

        /**
         * The service is unhealthy or experiencing issues.
         */
        DOWN,

        /**
         * The service health is unknown or could not be determined.
         */
        UNKNOWN
    }

    /**
     * The overall health status.
     */
    private Status status;

    /**
     * The timestamp when this health check was performed.
     */
    private Instant timestamp;

    /**
     * The application version.
     */
    private String version;

    /**
     * Individual component health statuses.
     */
    @JsonProperty("components")
    private Map<String, ComponentHealth> components;

    public HealthStatus() {
    }

    public HealthStatus(Status status, String version) {
        this.status = status;
        this.version = version;
        this.timestamp = Instant.now();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, ComponentHealth> getComponents() {
        return components;
    }

    public void setComponents(Map<String, ComponentHealth> components) {
        this.components = components;
    }

    /**
     * Represents the health of an individual component.
     */
    public static class ComponentHealth {

        /**
         * The component's health status.
         */
        private Status status;

        /**
         * Additional details about the component's health.
         */
        private String details;

        public ComponentHealth() {
        }

        public ComponentHealth(Status status, String details) {
            this.status = status;
            this.details = details;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }
}
