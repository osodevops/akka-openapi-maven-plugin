package com.example.endpoint;

import akka.javasdk.annotations.http.Delete;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Patch;
import akka.javasdk.annotations.http.Post;
import com.example.dto.*;
import com.github.osodevops.akka.openapi.annotations.OpenAPIResponse;
import com.github.osodevops.akka.openapi.annotations.OpenAPITag;

import java.util.List;

/**
 * Order management endpoint.
 *
 * <p>Provides operations for creating, viewing, and managing orders.
 * Orders contain nested types including items and shipping addresses.</p>
 *
 * <h2>Order Lifecycle</h2>
 * <ol>
 *   <li>PENDING - Order created, awaiting confirmation</li>
 *   <li>CONFIRMED - Payment received, order confirmed</li>
 *   <li>PROCESSING - Order being prepared</li>
 *   <li>SHIPPED - Order dispatched to carrier</li>
 *   <li>DELIVERED - Order received by customer</li>
 * </ol>
 */
@HttpEndpoint("/api/v1/orders")
@OpenAPITag(name = "Orders", description = "Order management operations")
public class OrderEndpoint {

    /**
     * Lists all orders with pagination.
     *
     * <p>Returns a paginated list of all orders. Results can be filtered
     * by status and sorted by creation date.</p>
     *
     * @param page the page number (0-indexed)
     * @param size the page size (default: 20)
     * @param status optional filter by order status
     * @return paginated order list
     */
    @Get
    @OpenAPIResponse(status = "200", description = "Orders retrieved successfully")
    public PagedResponse<Order> listOrders(Integer page, Integer size, OrderStatus status) {
        // Implementation would go here
        return new PagedResponse<>();
    }

    /**
     * Retrieves a specific order by ID.
     *
     * <p>Returns detailed order information including all items,
     * shipping address, and status history.</p>
     *
     * @param id the order ID
     * @return the order details
     */
    @Get("/{id}")
    @OpenAPIResponse(status = "200", description = "Order found")
    @OpenAPIResponse(status = "404", description = "Order not found", responseType = ErrorResponse.class)
    public Order getOrder(String id) {
        // Implementation would go here
        return new Order();
    }

    /**
     * Creates a new order.
     *
     * <p>Creates a new order with the specified items and shipping address.
     * The order will be created in PENDING status.</p>
     *
     * <h3>Validation</h3>
     * <ul>
     *   <li>Customer must exist and be in ACTIVE status</li>
     *   <li>At least one item is required</li>
     *   <li>All product IDs must be valid</li>
     *   <li>Quantities must be positive</li>
     * </ul>
     *
     * @param request the order creation request
     * @return the created order
     */
    @Post
    @OpenAPIResponse(status = "201", description = "Order created successfully")
    @OpenAPIResponse(status = "400", description = "Invalid order data", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "404", description = "Customer or product not found", responseType = ErrorResponse.class)
    public Order createOrder(CreateOrderRequest request) {
        // Implementation would go here
        return new Order();
    }

    /**
     * Updates the order status.
     *
     * <p>Advances the order through its lifecycle. Only valid status
     * transitions are allowed.</p>
     *
     * <h3>Valid Transitions</h3>
     * <ul>
     *   <li>PENDING → CONFIRMED, CANCELLED</li>
     *   <li>CONFIRMED → PROCESSING, CANCELLED</li>
     *   <li>PROCESSING → SHIPPED</li>
     *   <li>SHIPPED → DELIVERED</li>
     * </ul>
     *
     * @param id the order ID
     * @param status the new status
     * @return the updated order
     */
    @Patch("/{id}/status")
    @OpenAPIResponse(status = "200", description = "Order status updated")
    @OpenAPIResponse(status = "400", description = "Invalid status transition", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "404", description = "Order not found", responseType = ErrorResponse.class)
    public Order updateOrderStatus(String id, OrderStatus status) {
        // Implementation would go here
        return new Order();
    }

    /**
     * Cancels an order.
     *
     * <p>Cancels the order if it is in a cancellable state (PENDING or CONFIRMED).
     * Orders that have been shipped cannot be cancelled.</p>
     *
     * @param id the order ID to cancel
     * @return the cancelled order
     */
    @Delete("/{id}")
    @OpenAPIResponse(status = "200", description = "Order cancelled successfully")
    @OpenAPIResponse(status = "400", description = "Order cannot be cancelled", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "404", description = "Order not found", responseType = ErrorResponse.class)
    public Order cancelOrder(String id) {
        // Implementation would go here
        return new Order();
    }

    /**
     * Retrieves items for a specific order.
     *
     * <p>Returns the list of items in the order with full details.</p>
     *
     * @param id the order ID
     * @return list of order items
     */
    @Get("/{id}/items")
    @OpenAPIResponse(status = "200", description = "Order items retrieved")
    @OpenAPIResponse(status = "404", description = "Order not found", responseType = ErrorResponse.class)
    public List<OrderItem> getOrderItems(String id) {
        // Implementation would go here
        return List.of();
    }

    /**
     * Adds an item to an existing order.
     *
     * <p>Adds a new item to the order. Only works for orders in PENDING status.</p>
     *
     * @param id the order ID
     * @param item the item to add
     * @return the updated order
     */
    @Post("/{id}/items")
    @OpenAPIResponse(status = "201", description = "Item added to order")
    @OpenAPIResponse(status = "400", description = "Cannot modify order or invalid item", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "404", description = "Order or product not found", responseType = ErrorResponse.class)
    public Order addOrderItem(String id, OrderItem item) {
        // Implementation would go here
        return new Order();
    }

    /**
     * Updates the shipping address for an order.
     *
     * <p>Changes the delivery address. Only works for orders that have
     * not yet been shipped.</p>
     *
     * @param id the order ID
     * @param address the new shipping address
     * @return the updated order
     */
    @Patch("/{id}/address")
    @OpenAPIResponse(status = "200", description = "Shipping address updated")
    @OpenAPIResponse(status = "400", description = "Cannot modify shipped order", responseType = ErrorResponse.class)
    @OpenAPIResponse(status = "404", description = "Order not found", responseType = ErrorResponse.class)
    public Order updateShippingAddress(String id, Address address) {
        // Implementation would go here
        return new Order();
    }
}
