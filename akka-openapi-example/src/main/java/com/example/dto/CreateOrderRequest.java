package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for creating a new order.
 *
 * <p>Contains all required information to create a new order for a customer.</p>
 */
public class CreateOrderRequest {

    /**
     * The ID of the customer placing the order.
     */
    @NotBlank(message = "Customer ID is required")
    @JsonProperty("customerId")
    private String customerId;

    /**
     * The items to include in the order.
     */
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItem> items;

    /**
     * The shipping address for this order.
     */
    @Valid
    @NotNull(message = "Shipping address is required")
    @JsonProperty("shippingAddress")
    private Address shippingAddress;

    /**
     * Optional notes or special instructions for the order.
     */
    private String notes;

    public CreateOrderRequest() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
