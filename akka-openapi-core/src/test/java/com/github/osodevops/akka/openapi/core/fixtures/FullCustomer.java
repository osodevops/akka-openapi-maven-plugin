package com.github.osodevops.akka.openapi.core.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Complex customer DTO for testing comprehensive schema generation.
 */
public class FullCustomer {

    @NotNull
    private UUID id;

    @NotNull
    @Size(min = 1, max = 100)
    private String name;

    @Email
    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    private CustomerStatus status;

    private LocalDate createdAt;

    // Nested object
    private Address primaryAddress;

    // Collection of nested objects
    private List<Address> addresses;

    // Optional field
    private Optional<String> nickname;

    // Map type
    private Map<String, String> metadata;

    // Array of primitives
    private String[] tags;

    // Field to ignore
    @JsonIgnore
    private String internalId;

    public FullCustomer() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public Address getPrimaryAddress() { return primaryAddress; }
    public void setPrimaryAddress(Address primaryAddress) { this.primaryAddress = primaryAddress; }

    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }

    public Optional<String> getNickname() { return nickname; }
    public void setNickname(Optional<String> nickname) { this.nickname = nickname; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public String getInternalId() { return internalId; }
    public void setInternalId(String internalId) { this.internalId = internalId; }
}
