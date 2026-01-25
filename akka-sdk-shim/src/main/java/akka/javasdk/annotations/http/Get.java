package akka.javasdk.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as handling HTTP GET requests.
 *
 * <p>This annotation is compatible with the Akka SDK {@code @Get} annotation.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Get {
    /**
     * The path for this operation, relative to the endpoint's base path.
     * May contain path parameters (e.g., "/{id}").
     *
     * @return the relative path
     */
    String value() default "";
}
