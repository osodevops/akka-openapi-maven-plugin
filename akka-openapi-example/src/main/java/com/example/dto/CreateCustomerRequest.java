package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for creating a new customer.
 *
 * <p>Contains all the required information to create a new customer account.
 * The customer ID is auto-generated and not included in this request.</p>
 */
public class CreateCustomerRequest {

    /**
     * The customer's first name.
     */
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @JsonProperty("firstName")
    private String firstName;

    /**
     * The customer's last name.
     */
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @JsonProperty("lastName")
    private String lastName;

    /**
     * The customer's email address. Must be unique in the system.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * The customer's phone number.
     */
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    /**
     * The customer's billing address.
     */
    @Valid
    @NotNull(message = "Billing address is required")
    @JsonProperty("billingAddress")
    private Address billingAddress;

    /**
     * Optional list of shipping addresses.
     */
    @Valid
    @JsonProperty("shippingAddresses")
    private List<Address> shippingAddresses;

    public CreateCustomerRequest() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(Address billingAddress) {
        this.billingAddress = billingAddress;
    }

    public List<Address> getShippingAddresses() {
        return shippingAddresses;
    }

    public void setShippingAddresses(List<Address> shippingAddresses) {
        this.shippingAddresses = shippingAddresses;
    }
}
