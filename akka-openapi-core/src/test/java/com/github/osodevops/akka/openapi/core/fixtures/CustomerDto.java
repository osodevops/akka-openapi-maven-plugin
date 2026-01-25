package com.github.osodevops.akka.openapi.core.fixtures;

/**
 * Sample DTO for testing request/response body extraction.
 */
public class CustomerDto {
    private String id;
    private String name;
    private String email;

    public CustomerDto() {}

    public CustomerDto(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
