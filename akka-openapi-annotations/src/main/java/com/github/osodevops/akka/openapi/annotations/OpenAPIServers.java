package com.github.osodevops.akka.openapi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link OpenAPIServer} annotations.
 *
 * @since 1.0.0
 * @see OpenAPIServer
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenAPIServers {

    /**
     * The array of server annotations.
     *
     * @return the server annotations
     */
    OpenAPIServer[] value();
}
