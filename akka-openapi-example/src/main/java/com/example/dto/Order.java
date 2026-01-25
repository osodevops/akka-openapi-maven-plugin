package com.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Represents a customer order.
 *
 * <p>An order contains one or more items, shipping information, and tracks
 * the order status through its lifecycle from creation to delivery.</p>
 */
public class Order {

    /**
     * Unique identifier for the order.
     */
    @NotBlank
    private String id;

    /**
     * The ID of the customer who placed the order.
     */
    @NotBlank(message = "Customer ID is required")
    @JsonProperty("customerId")
    private String customerId;

    /**
     * The items in this order.
     */
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItem> items;

    /**
     * The current status of the order.
     */
    @NotNull
    private OrderStatus status;

    /**
     * The shipping address for this order.
     */
    @Valid
    @NotNull(message = "Shipping address is required")
    @JsonProperty("shippingAddress")
    private Address shippingAddress;

    /**
     * The total amount for this order including all items.
     */
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    /**
     * Optional notes or special instructions for the order.
     */
    private String notes;

    /**
     * Timestamp when the order was created.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @JsonProperty("createdAt")
    private Instant createdAt;

    /**
     * Timestamp when the order was last updated.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    /**
     * Timestamp when the order was shipped.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @JsonProperty("shippedAt")
    private Instant shippedAt;

    /**
     * Timestamp when the order was delivered.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @JsonProperty("deliveredAt")
    private Instant deliveredAt;

    public Order() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(Instant shippedAt) {
        this.shippedAt = shippedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
