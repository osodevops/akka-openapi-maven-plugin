package akka.javasdk.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as handling HTTP DELETE requests.
 *
 * <p>This annotation is compatible with the Akka SDK {@code @Delete} annotation.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Delete {
    /**
     * The path for this operation, relative to the endpoint's base path.
     *
     * @return the relative path
     */
    String value() default "";
}
