package com.github.osodevops.akka.openapi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a server URL for the OpenAPI specification.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @OpenAPIServer(url = "https://api.example.com", description = "Production")
 * @OpenAPIServer(url = "https://api-staging.example.com", description = "Staging")
 * @HttpEndpoint("/customers")
 * public class CustomerEndpoint {
 *     // ...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(OpenAPIServers.class)
public @interface OpenAPIServer {

    /**
     * The server URL.
     *
     * @return the server URL
     */
    String url();

    /**
     * A description of the server.
     *
     * @return the server description
     */
    String description() default "";
}
