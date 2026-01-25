package com.github.osodevops.akka.openapi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assigns tags to an endpoint for grouping in OpenAPI documentation.
 *
 * <p>Tags are used to group operations in API documentation tools like SwaggerUI.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @HttpEndpoint("/customers")
 * @OpenAPITag(name = "Customers", description = "Customer management operations")
 * public class CustomerEndpoint {
 *     // ...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenAPITag {

    /**
     * The tag name.
     *
     * @return the tag name
     */
    String name();

    /**
     * A description of the tag.
     *
     * @return the tag description
     */
    String description() default "";

    /**
     * URL for external documentation about this tag.
     *
     * @return the external documentation URL
     */
    String externalDocsUrl() default "";

    /**
     * Description of the external documentation.
     *
     * @return the external documentation description
     */
    String externalDocsDescription() default "";
}
