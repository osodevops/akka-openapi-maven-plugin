package com.example.dto;

/**
 * Represents the status of an order.
 */
public enum OrderStatus {

    /**
     * Order has been created but not yet processed.
     */
    PENDING,

    /**
     * Order has been confirmed and payment accepted.
     */
    CONFIRMED,

    /**
     * Order is being processed and prepared for shipment.
     */
    PROCESSING,

    /**
     * Order has been shipped.
     */
    SHIPPED,

    /**
     * Order has been delivered to the customer.
     */
    DELIVERED,

    /**
     * Order has been cancelled.
     */
    CANCELLED,

    /**
     * Order has been refunded.
     */
    REFUNDED
}
