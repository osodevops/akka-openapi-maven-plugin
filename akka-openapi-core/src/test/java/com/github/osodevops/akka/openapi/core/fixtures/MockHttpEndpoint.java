package com.github.osodevops.akka.openapi.core.fixtures;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mock annotation simulating Akka SDK's @HttpEndpoint for testing purposes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MockHttpEndpoint {
    String value() default "/";
}
