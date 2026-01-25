/**
 * HTTP endpoints demonstrating Akka SDK and OpenAPI annotations.
 *
 * <p>This package contains HTTP endpoint classes annotated with Akka SDK
 * {@code @HttpEndpoint} and method annotations ({@code @Get}, {@code @Post}, etc.)
 * combined with OpenAPI annotations for documentation enrichment.</p>
 *
 * <h2>Available Endpoints</h2>
 *
 * <h3>Customer API ({@code /api/v1/customers})</h3>
 * <ul>
 *   <li>GET / - List all customers (paginated)</li>
 *   <li>GET /{id} - Get customer by ID</li>
 *   <li>POST / - Create new customer</li>
 *   <li>PUT /{id} - Replace customer</li>
 *   <li>PATCH /{id} - Partially update customer</li>
 *   <li>DELETE /{id} - Delete customer</li>
 * </ul>
 *
 * <h3>Order API ({@code /api/v1/orders})</h3>
 * <ul>
 *   <li>GET / - List all orders (paginated)</li>
 *   <li>GET /{id} - Get order by ID</li>
 *   <li>POST / - Create new order</li>
 *   <li>PATCH /{id}/status - Update order status</li>
 *   <li>DELETE /{id} - Cancel order</li>
 * </ul>
 *
 * <h3>Health API ({@code /health})</h3>
 * <ul>
 *   <li>GET / - Overall health status</li>
 *   <li>GET /live - Liveness probe</li>
 *   <li>GET /ready - Readiness probe</li>
 * </ul>
 */
package com.example.endpoint;
