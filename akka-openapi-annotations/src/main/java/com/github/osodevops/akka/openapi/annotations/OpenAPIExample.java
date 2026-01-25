package com.github.osodevops.akka.openapi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides an example value for a request body, response, or parameter.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Post
 * @OpenAPIExample(
 *     name = "newCustomer",
 *     summary = "Create a new customer",
 *     value = "{\"name\": \"John Doe\", \"email\": \"john@example.com\"}"
 * )
 * public Customer createCustomer(CreateCustomerRequest request) {
 *     // ...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(OpenAPIExamples.class)
public @interface OpenAPIExample {

    /**
     * A unique name for this example.
     *
     * @return the example name
     */
    String name();

    /**
     * A short summary of the example.
     *
     * @return the summary
     */
    String summary() default "";

    /**
     * A description of the example.
     *
     * @return the description
     */
    String description() default "";

    /**
     * The example value as a JSON string.
     *
     * @return the example value
     */
    String value();

    /**
     * URL pointing to an external example.
     *
     * @return the external value URL
     */
    String externalValue() default "";
}
