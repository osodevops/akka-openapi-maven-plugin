package com.github.osodevops.akka.openapi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link OpenAPIResponse} annotations.
 *
 * <p>This annotation is automatically used when multiple {@code @OpenAPIResponse}
 * annotations are placed on the same method.</p>
 *
 * @since 1.0.0
 * @see OpenAPIResponse
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenAPIResponses {

    /**
     * The array of response annotations.
     *
     * @return the response annotations
     */
    OpenAPIResponse[] value();
}
