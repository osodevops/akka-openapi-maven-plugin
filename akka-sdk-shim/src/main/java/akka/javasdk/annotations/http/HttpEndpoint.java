package akka.javasdk.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an HTTP endpoint.
 *
 * <p>This annotation is compatible with the Akka SDK {@code @HttpEndpoint} annotation.
 * It provides the base path for all HTTP operations defined in the class.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpEndpoint {
    /**
     * The base path for all operations in this endpoint.
     *
     * @return the base path (e.g., "/api/v1/customers")
     */
    String value();
}
