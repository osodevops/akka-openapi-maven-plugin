package com.github.osodevops.akka.openapi.core.fixtures;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for testing enum schema generation with @JsonValue.
 */
public enum CustomerStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    PENDING("pending"),
    SUSPENDED("suspended");

    private final String value;

    CustomerStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
