package com.github.osodevops.akka.openapi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides additional API metadata for OpenAPI specification generation.
 *
 * <p>This annotation can be placed on a package-info.java or on any endpoint class
 * to provide global API metadata. When placed on multiple classes, the first one found
 * will be used.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @OpenAPIInfo(
 *     title = "Customer API",
 *     version = "1.0.0",
 *     description = "API for managing customer records",
 *     termsOfService = "https://example.com/terms",
 *     contactName = "API Support",
 *     contactEmail = "support@example.com"
 * )
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
public @interface OpenAPIInfo {

    /**
     * The title of the API.
     *
     * @return the API title
     */
    String title() default "";

    /**
     * The version of the API.
     *
     * @return the API version
     */
    String version() default "";

    /**
     * A description of the API.
     *
     * @return the API description
     */
    String description() default "";

    /**
     * URL to the Terms of Service for the API.
     *
     * @return the terms of service URL
     */
    String termsOfService() default "";

    /**
     * The name of the contact person/organization for the API.
     *
     * @return the contact name
     */
    String contactName() default "";

    /**
     * The email address of the contact person/organization.
     *
     * @return the contact email
     */
    String contactEmail() default "";

    /**
     * The URL pointing to the contact information.
     *
     * @return the contact URL
     */
    String contactUrl() default "";

    /**
     * The license name used for the API.
     *
     * @return the license name
     */
    String licenseName() default "";

    /**
     * URL to the license used for the API.
     *
     * @return the license URL
     */
    String licenseUrl() default "";
}
