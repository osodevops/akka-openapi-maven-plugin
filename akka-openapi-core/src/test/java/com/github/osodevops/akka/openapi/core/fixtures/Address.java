package com.github.osodevops.akka.openapi.core.fixtures;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Address DTO for testing nested object schema generation.
 */
public class Address {

    @NotNull
    @Size(min = 1, max = 100)
    private String street;

    @NotNull
    private String city;

    @Size(max = 50)
    private String state;

    @JsonProperty("postal_code")
    @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$")
    private String postalCode;

    private String country;

    public Address() {}

    public Address(String street, String city, String state, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
