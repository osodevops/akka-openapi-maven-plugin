package com.github.osodevops.akka.openapi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link OpenAPIExample} annotations.
 *
 * @since 1.0.0
 * @see OpenAPIExample
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenAPIExamples {

    /**
     * The array of example annotations.
     *
     * @return the example annotations
     */
    OpenAPIExample[] value();
}
