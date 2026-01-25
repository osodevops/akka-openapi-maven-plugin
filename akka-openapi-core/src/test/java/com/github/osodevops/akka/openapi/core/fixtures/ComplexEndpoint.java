package com.github.osodevops.akka.openapi.core.fixtures;

import java.util.List;

/**
 * Complex endpoint with multiple methods for testing.
 */
@MockHttpEndpoint("/complex")
public class ComplexEndpoint {

    public List<String> list() {
        return List.of();
    }

    public String create(String request) {
        return request;
    }

    public String update(String id, String request) {
        return id + ": " + request;
    }

    public void delete(String id) {
        // no-op
    }
}
