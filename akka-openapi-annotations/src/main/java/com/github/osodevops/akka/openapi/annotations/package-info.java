/**
 * Custom annotations for enhancing OpenAPI specification generation from Akka SDK endpoints.
 *
 * <p>These annotations are optional and can be used to provide additional metadata
 * that cannot be inferred from Akka SDK annotations or JavaDoc comments.</p>
 *
 * <h2>Available Annotations</h2>
 * <ul>
 *   <li>{@link com.github.osodevops.akka.openapi.annotations.OpenAPIInfo} - API metadata</li>
 *   <li>{@link com.github.osodevops.akka.openapi.annotations.OpenAPIServer} - Server definitions</li>
 *   <li>{@link com.github.osodevops.akka.openapi.annotations.OpenAPITag} - Endpoint grouping</li>
 *   <li>{@link com.github.osodevops.akka.openapi.annotations.OpenAPIResponse} - Response documentation</li>
 *   <li>{@link com.github.osodevops.akka.openapi.annotations.OpenAPIExample} - Example values</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.github.osodevops.akka.openapi.annotations;
