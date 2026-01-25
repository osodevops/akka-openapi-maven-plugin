package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for updating an existing customer.
 *
 * <p>All fields are optional. Only provided fields will be updated.
 * To clear a field, explicitly set it to null.</p>
 */
public class UpdateCustomerRequest {

    /**
     * The customer's first name.
     */
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @JsonProperty("firstName")
    private String firstName;

    /**
     * The customer's last name.
     */
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @JsonProperty("lastName")
    private String lastName;

    /**
     * The customer's email address.
     */
    @Email(message = "Invalid email format")
    private String email;

    /**
     * The customer's phone number.
     */
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    /**
     * The new customer status.
     */
    private CustomerStatus status;

    /**
     * The customer's billing address.
     */
    @Valid
    @JsonProperty("billingAddress")
    private Address billingAddress;

    /**
     * The customer's shipping addresses.
     */
    @Valid
    @JsonProperty("shippingAddresses")
    private List<Address> shippingAddresses;

    public UpdateCustomerRequest() {
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

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
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
