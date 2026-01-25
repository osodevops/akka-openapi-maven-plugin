package com.example.dto;

/**
 * Represents the status of a customer account.
 */
public enum CustomerStatus {

    /**
     * The customer account is active and can place orders.
     */
    ACTIVE,

    /**
     * The customer account is temporarily suspended.
     */
    SUSPENDED,

    /**
     * The customer account has been closed.
     */
    CLOSED,

    /**
     * The customer account is pending verification.
     */
    PENDING_VERIFICATION
}
