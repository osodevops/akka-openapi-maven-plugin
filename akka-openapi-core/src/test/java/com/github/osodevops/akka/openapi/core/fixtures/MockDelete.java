package com.github.osodevops.akka.openapi.core.fixtures;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MockDelete {
    String value() default "";
}
