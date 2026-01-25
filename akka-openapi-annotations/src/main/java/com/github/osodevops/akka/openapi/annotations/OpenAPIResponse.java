package com.github.osodevops.akka.openapi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents an HTTP response for an endpoint operation.
 *
 * <p>Use this annotation to document additional response codes beyond
 * what can be inferred from the method signature.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Get("/{id}")
 * @OpenAPIResponse(status = "200", description = "Customer found")
 * @OpenAPIResponse(status = "404", description = "Customer not found")
 * @OpenAPIResponse(status = "500", description = "Internal server error")
 * public Customer getCustomer(String id) {
 *     // ...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(OpenAPIResponses.class)
public @interface OpenAPIResponse {

    /**
     * The HTTP status code for this response.
     *
     * @return the HTTP status code (e.g., "200", "404", "500")
     */
    String status();

    /**
     * A description of the response.
     *
     * @return the response description
     */
    String description();

    /**
     * The response body type. If not specified, the method return type is used
     * for successful responses (2xx).
     *
     * @return the response type class
     */
    Class<?> responseType() default Void.class;

    /**
     * The media type of the response content.
     *
     * @return the media type (default: application/json)
     */
    String mediaType() default "application/json";
}
