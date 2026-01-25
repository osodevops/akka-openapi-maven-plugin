# Examples

This document provides comprehensive examples for common use cases with the Akka OpenAPI Maven Plugin.

## Basic CRUD Endpoint

A complete example of a RESTful CRUD endpoint:

```java
package com.example.api;

import akka.javasdk.annotations.http.*;
import sh.oso.akka.openapi.annotations.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Product management endpoint.
 *
 * Provides CRUD operations for managing products in the catalog.
 */
@HttpEndpoint("/api/v1/products")
@OpenAPITag(name = "Products", description = "Product catalog management")
public class ProductEndpoint {

    /**
     * List all products with pagination.
     *
     * @param page page number (0-indexed)
     * @param size number of items per page
     * @param category optional category filter
     * @return paginated list of products
     */
    @Get
    @OpenAPIResponse(status = "200", description = "Products retrieved successfully")
    public PagedResponse<Product> listProducts(
            Integer page,
            Integer size,
            String category) {
        // Implementation
        return new PagedResponse<>();
    }

    /**
     * Get a product by ID.
     *
     * @param id the product identifier
     * @return the product details
     */
    @Get("/{id}")
    @OpenAPIResponse(status = "200", description = "Product found")
    @OpenAPIResponse(status = "404", description = "Product not found")
    public Product getProduct(String id) {
        // Implementation
        return new Product();
    }

    /**
     * Create a new product.
     *
     * @param request the product creation request
     * @return the created product with assigned ID
     */
    @Post
    @OpenAPIResponse(status = "201", description = "Product created successfully")
    @OpenAPIResponse(status = "400", description = "Invalid request data")
    public Product createProduct(@Valid CreateProductRequest request) {
        // Implementation
        return new Product();
    }

    /**
     * Update an existing product.
     *
     * @param id the product ID to update
     * @param request the updated product data
     * @return the updated product
     */
    @Put("/{id}")
    @OpenAPIResponse(status = "200", description = "Product updated successfully")
    @OpenAPIResponse(status = "404", description = "Product not found")
    @OpenAPIResponse(status = "400", description = "Invalid request data")
    public Product updateProduct(String id, @Valid CreateProductRequest request) {
        // Implementation
        return new Product();
    }

    /**
     * Delete a product.
     *
     * @param id the product ID to delete
     */
    @Delete("/{id}")
    @OpenAPIResponse(status = "204", description = "Product deleted successfully")
    @OpenAPIResponse(status = "404", description = "Product not found")
    public void deleteProduct(String id) {
        // Implementation
    }
}
```

## DTOs with Validation

```java
package com.example.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a product in the catalog.
 */
public class Product {

    /**
     * Unique product identifier.
     */
    private String id;

    /**
     * Product name.
     */
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 200, message = "Name must be 1-200 characters")
    private String name;

    /**
     * Product description.
     */
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    /**
     * Product price in USD.
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least $0.01")
    @DecimalMax(value = "999999.99", message = "Price must not exceed $999,999.99")
    private BigDecimal price;

    /**
     * Product category.
     */
    @NotBlank
    private String category;

    /**
     * Stock quantity.
     */
    @Min(value = 0, message = "Stock cannot be negative")
    private int stockQuantity;

    /**
     * Product SKU.
     */
    @Pattern(regexp = "^[A-Z]{3}-\\d{6}$", message = "SKU must be in format XXX-000000")
    private String sku;

    /**
     * When the product was created.
     */
    @JsonProperty("createdAt")
    private Instant createdAt;

    // Getters and setters...
}

/**
 * Request to create a new product.
 */
public class CreateProductRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @NotBlank
    private String category;

    @Min(0)
    private int stockQuantity;

    @Pattern(regexp = "^[A-Z]{3}-\\d{6}$")
    private String sku;

    // Getters and setters...
}
```

## Nested Objects

Example with complex nested types:

```java
package com.example.api;

import java.util.List;

/**
 * Customer order with nested items and addresses.
 */
public class Order {

    private String id;
    private String customerId;
    private List<OrderItem> items;
    private Address shippingAddress;
    private Address billingAddress;
    private PaymentInfo payment;
    private OrderStatus status;

    // Getters and setters...
}

public class OrderItem {
    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}

public class Address {
    @NotBlank
    private String street;

    @NotBlank
    private String city;

    private String state;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$")
    private String country;

    @NotBlank
    private String postalCode;
}

public class PaymentInfo {
    private PaymentMethod method;
    private String transactionId;
    private Instant paidAt;
}

public enum PaymentMethod {
    CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER
}

public enum OrderStatus {
    PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}
```

## Enumerations

Enums are automatically converted to OpenAPI string schemas with enum values:

```java
/**
 * Customer account status.
 */
public enum CustomerStatus {
    /**
     * Account is active and in good standing.
     */
    ACTIVE,

    /**
     * Account is temporarily suspended.
     */
    SUSPENDED,

    /**
     * Account has been permanently closed.
     */
    CLOSED,

    /**
     * Account is pending email verification.
     */
    PENDING_VERIFICATION
}
```

Generated OpenAPI:
```yaml
CustomerStatus:
  type: string
  enum:
    - ACTIVE
    - SUSPENDED
    - CLOSED
    - PENDING_VERIFICATION
```

## Pagination

Standard pagination pattern:

```java
/**
 * Paginated response wrapper.
 *
 * @param <T> the type of items in the page
 */
public class PagedResponse<T> {

    /**
     * Items in this page.
     */
    private List<T> content;

    /**
     * Current page number (0-indexed).
     */
    private int page;

    /**
     * Number of items per page.
     */
    private int size;

    /**
     * Total number of items across all pages.
     */
    private long totalElements;

    /**
     * Total number of pages.
     */
    private int totalPages;

    /**
     * Whether this is the first page.
     */
    private boolean first;

    /**
     * Whether this is the last page.
     */
    private boolean last;

    // Getters and setters...
}
```

## Health Check Endpoint

Standard health check pattern:

```java
package com.example.health;

import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import sh.oso.akka.openapi.annotations.*;

/**
 * Health check endpoints for monitoring.
 */
@HttpEndpoint("/health")
@OpenAPITag(name = "Health", description = "Application health monitoring")
public class HealthEndpoint {

    /**
     * Overall application health status.
     */
    @Get
    @OpenAPIResponse(status = "200", description = "Application is healthy")
    @OpenAPIResponse(status = "503", description = "Application is unhealthy")
    public HealthStatus getHealth() {
        return new HealthStatus(HealthStatus.Status.UP, "1.0.0");
    }

    /**
     * Liveness probe for Kubernetes.
     */
    @Get("/live")
    @OpenAPIResponse(status = "200", description = "Application is alive")
    public HealthStatus getLiveness() {
        return new HealthStatus(HealthStatus.Status.UP, "1.0.0");
    }

    /**
     * Readiness probe for Kubernetes.
     */
    @Get("/ready")
    @OpenAPIResponse(status = "200", description = "Application is ready")
    @OpenAPIResponse(status = "503", description = "Application is not ready")
    public HealthStatus getReadiness() {
        return new HealthStatus(HealthStatus.Status.UP, "1.0.0");
    }
}

public class HealthStatus {
    public enum Status { UP, DOWN, UNKNOWN }

    private Status status;
    private String version;
    private Instant timestamp;
    private Map<String, ComponentHealth> components;

    // Constructor, getters, setters...
}

public class ComponentHealth {
    private HealthStatus.Status status;
    private String details;
}
```

## API Versioning

Multiple API versions in the same project:

```java
// Version 1
@HttpEndpoint("/api/v1/customers")
@OpenAPITag(name = "Customers (v1)", description = "Customer API version 1")
public class CustomerEndpointV1 {
    @Get("/{id}")
    public CustomerV1 getCustomer(String id) { ... }
}

// Version 2 with enhanced response
@HttpEndpoint("/api/v2/customers")
@OpenAPITag(name = "Customers (v2)", description = "Customer API version 2 with enhanced features")
public class CustomerEndpointV2 {
    @Get("/{id}")
    public CustomerV2 getCustomer(String id) { ... }
}
```

## Error Response Pattern

Consistent error responses:

```java
/**
 * Standard error response format.
 */
public class ErrorResponse {

    /**
     * HTTP status code.
     */
    private int status;

    /**
     * Error type/code.
     */
    private String error;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Request path that caused the error.
     */
    private String path;

    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * Field-level validation errors.
     */
    private List<FieldError> validationErrors;

    public static class FieldError {
        private String field;
        private Object rejectedValue;
        private String message;
    }
}
```

Use in endpoints:

```java
@Get("/{id}")
@OpenAPIResponse(status = "200", description = "Customer found")
@OpenAPIResponse(status = "404", description = "Customer not found", responseType = ErrorResponse.class)
@OpenAPIResponse(status = "500", description = "Internal server error", responseType = ErrorResponse.class)
public Customer getCustomer(String id) { ... }
```

## Complete Example Project

See the `akka-openapi-example` module for a complete working example:

```bash
# Build the example
mvn compile -pl akka-openapi-example

# View the generated specification
cat akka-openapi-example/target/openapi.yaml
```

The example includes:
- CustomerEndpoint - Full CRUD with validation
- OrderEndpoint - Nested types and enums
- HealthEndpoint - Health checks

## See Also

- [Getting Started](GETTING_STARTED.md)
- [Configuration Reference](CONFIGURATION.md)
- [Troubleshooting](TROUBLESHOOTING.md)
