package com.example.endpoint;

import akka.javasdk.annotations.http.Delete;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Patch;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.annotations.http.Put;
import com.example.dto.*;
import com.github.osodevops.akka.openapi.annotations.OpenAPIResponse;
import com.github.osodevops.akka.openapi.annotations.OpenAPITag;

import java.util.List;

/**
 * Customer management endpoint.
 *
 * <p>Provides CRUD operations for managing customer records in the system.
 * All endpoints follow RESTful conventions and return JSON responses.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Create, read, update, and delete customers</li>
 *   <li>Search customers by various criteria</li>
 *   <li>Manage customer addresses</li>
 *   <li>View customer order history</li>
 * </ul>
 */
@HttpEndpoint("/api/v1/customers")
@OpenAPITag(name = "Customers", description = "Customer management operations")
public class CustomerEndpoint {

    /**
     * Retrieves all customers with pagination.
     *
     * <p>Returns a paginated list of all customers. Use the page and size
     * query parameters to control pagination.</p>
     *
     * @param page the page number (0-indexed, default: 0)
     * @param size the number of items per page (default: 20, max: 100)
     * @param status optional filter by customer status
     * @return a paginated response containing customers
     */
    @Get
    @OpenAPIResponse(status = "200", description = "Successfully retrieved customer list")
    public PagedResponse<Customer> listCustomers(Integer page, Integer size, CustomerStatus status) {
        // Implementation would go here
        return new PagedResponse<>();
    }

    /**
     * Retrieves a specific customer by ID.
     *
     * <p>Returns detailed information about a single customer including
     * their addresses and account status.</p>
     *
     * @param id the unique customer identifier
     * @return the customer details
     */
    @Get("/{id}")
    @OpenAPIResponse(status = "200", description = "Customer found")
    @OpenAPIResponse(status = "404", description = "Customer not found", responseType = ErrorResponse.class)
    public Customer getCustomer(String id) {
        // Implementation would go here
        return new Customer();
    }

    /**
     * Creates a new customer.
     *
     * <p>Creates a new customer record with the provided information.
     * The customer will be assigned a unique ID and set to PENDING_VERIFICATION status.</p>
     *
     * @param request the customer creation request
     * @return the created customer with assigned ID
     */
    @Post
    @OpenAPIResponse(status = "201", description = "Customer created successfully")
    @OpenAPIResponse(status = "400", description = "Invalid request data", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "409", description = "Customer with this email already exists", responseType = ErrorResponse.class)
    public Customer createCustomer(CreateCustomerRequest request) {
        // Implementation would go here
        return new Customer();
    }

    /**
     * Updates an existing customer.
     *
     * <p>Performs a full replacement of the customer data. All fields
     * must be provided, even if they are not changing.</p>
     *
     * @param id the customer ID to update
     * @param request the customer data to set
     * @return the updated customer
     */
    @Put("/{id}")
    @OpenAPIResponse(status = "200", description = "Customer updated successfully")
    @OpenAPIResponse(status = "400", description = "Invalid request data", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "404", description = "Customer not found", responseType = ErrorResponse.class)
    public Customer updateCustomer(String id, CreateCustomerRequest request) {
        // Implementation would go here
        return new Customer();
    }

    /**
     * Partially updates a customer.
     *
     * <p>Updates only the fields provided in the request. Fields not
     * included in the request will remain unchanged.</p>
     *
     * @param id the customer ID to update
     * @param request the fields to update
     * @return the updated customer
     */
    @Patch("/{id}")
    @OpenAPIResponse(status = "200", description = "Customer updated successfully")
    @OpenAPIResponse(status = "400", description = "Invalid request data", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "404", description = "Customer not found", responseType = ErrorResponse.class)
    public Customer patchCustomer(String id, UpdateCustomerRequest request) {
        // Implementation would go here
        return new Customer();
    }

    /**
     * Deletes a customer.
     *
     * <p>Permanently removes a customer from the system. This operation
     * cannot be undone. All associated data will be deleted.</p>
     *
     * @param id the customer ID to delete
     */
    @Delete("/{id}")
    @OpenAPIResponse(status = "204", description = "Customer deleted successfully")
    @OpenAPIResponse(status = "404", description = "Customer not found", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "409", description = "Customer has active orders and cannot be deleted", responseType = ErrorResponse.class)
    public void deleteCustomer(String id) {
        // Implementation would go here
    }

    /**
     * Searches for customers by email.
     *
     * <p>Performs a search for customers matching the provided email address.
     * Supports partial matching.</p>
     *
     * @param email the email address to search for
     * @return list of matching customers
     */
    @Get("/search")
    @OpenAPIResponse(status = "200", description = "Search completed successfully")
    public List<Customer> searchByEmail(String email) {
        // Implementation would go here
        return List.of();
    }

    /**
     * Retrieves the orders for a specific customer.
     *
     * <p>Returns all orders placed by the specified customer, ordered by
     * creation date descending (newest first).</p>
     *
     * @param id the customer ID
     * @return list of customer's orders
     */
    @Get("/{id}/orders")
    @OpenAPIResponse(status = "200", description = "Orders retrieved successfully")
    @OpenAPIResponse(status = "404", description = "Customer not found", responseType = ErrorResponse.class)
    public List<Order> getCustomerOrders(String id) {
        // Implementation would go here
        return List.of();
    }

    /**
     * Adds a shipping address to a customer.
     *
     * <p>Adds a new shipping address to the customer's address list.</p>
     *
     * @param id the customer ID
     * @param address the address to add
     * @return the updated customer
     */
    @Post("/{id}/addresses")
    @OpenAPIResponse(status = "201", description = "Address added successfully")
    @OpenAPIResponse(status = "400", description = "Invalid address data", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "404", description = "Customer not found", responseType = ErrorResponse.class)
    public Customer addAddress(String id, Address address) {
        // Implementation would go here
        return new Customer();
    }
}
