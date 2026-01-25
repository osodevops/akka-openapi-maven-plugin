package com.github.osodevops.akka.openapi.core.fixtures;

/**
 * Sample request DTO for testing request body extraction.
 */
public class CreateCustomerRequest {
    private String name;
    private String email;

    public CreateCustomerRequest() {}

    public CreateCustomerRequest(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
