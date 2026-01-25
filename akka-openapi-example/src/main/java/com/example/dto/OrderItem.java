package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Represents a single item within an order.
 *
 * <p>Contains product information, quantity, and pricing for one line item.</p>
 */
public class OrderItem {

    /**
     * The unique product identifier.
     */
    @NotBlank(message = "Product ID is required")
    @JsonProperty("productId")
    private String productId;

    /**
     * The product name at the time of order.
     */
    @NotBlank(message = "Product name is required")
    @JsonProperty("productName")
    private String productName;

    /**
     * The quantity ordered.
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * The unit price of the product.
     */
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
    @JsonProperty("unitPrice")
    private BigDecimal unitPrice;

    /**
     * The total price for this line item (quantity * unitPrice).
     */
    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;

    public OrderItem() {
    }

    public OrderItem(String productId, String productName, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
