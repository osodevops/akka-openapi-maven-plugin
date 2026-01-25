/**
 * Data Transfer Objects (DTOs) for the example API.
 *
 * <p>This package contains all request/response models used by the API endpoints.
 * Each class is documented and uses Jakarta Validation annotations for constraint
 * specifications.</p>
 *
 * <h2>Customer DTOs</h2>
 * <ul>
 *   <li>{@link com.example.dto.Customer} - Customer entity</li>
 *   <li>{@link com.example.dto.CreateCustomerRequest} - Create customer request</li>
 *   <li>{@link com.example.dto.UpdateCustomerRequest} - Update customer request</li>
 *   <li>{@link com.example.dto.CustomerStatus} - Customer status enum</li>
 * </ul>
 *
 * <h2>Order DTOs</h2>
 * <ul>
 *   <li>{@link com.example.dto.Order} - Order entity</li>
 *   <li>{@link com.example.dto.OrderItem} - Order line item</li>
 *   <li>{@link com.example.dto.CreateOrderRequest} - Create order request</li>
 *   <li>{@link com.example.dto.OrderStatus} - Order status enum</li>
 * </ul>
 *
 * <h2>Common DTOs</h2>
 * <ul>
 *   <li>{@link com.example.dto.Address} - Physical address</li>
 *   <li>{@link com.example.dto.HealthStatus} - Health check response</li>
 *   <li>{@link com.example.dto.ErrorResponse} - Standard error response</li>
 *   <li>{@link com.example.dto.PagedResponse} - Pagination wrapper</li>
 * </ul>
 */
package com.example.dto;
