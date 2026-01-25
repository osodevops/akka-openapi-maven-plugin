package com.github.osodevops.akka.openapi.core.fixtures;

/**
 * Simple test endpoint for scanning tests.
 * Uses a simulated annotation since we don't have Akka SDK in test scope.
 */
@MockHttpEndpoint("/simple")
public class SimpleEndpoint {

    public String hello() {
        return "Hello";
    }

    public String getById(String id) {
        return "Entity: " + id;
    }
}
