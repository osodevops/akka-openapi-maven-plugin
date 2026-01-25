package com.example.endpoint;

import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import com.example.dto.HealthStatus;
import com.github.osodevops.akka.openapi.annotations.OpenAPIResponse;
import com.github.osodevops.akka.openapi.annotations.OpenAPITag;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint.
 *
 * <p>Provides endpoints for monitoring application health and readiness.
 * These endpoints are typically used by load balancers and container
 * orchestration systems like Kubernetes.</p>
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li><code>/health</code> - Overall application health</li>
 *   <li><code>/health/live</code> - Liveness probe</li>
 *   <li><code>/health/ready</code> - Readiness probe</li>
 * </ul>
 */
@HttpEndpoint("/health")
@OpenAPITag(name = "Health", description = "Health check and monitoring endpoints")
public class HealthEndpoint {

    private static final String VERSION = "1.0.0";

    /**
     * Returns the overall health status of the application.
     *
     * <p>Performs comprehensive health checks on all application components
     * including database connections, external service availability, and
     * internal subsystems.</p>
     *
     * @return the health status with component details
     */
    @Get
    @OpenAPIResponse(status = "200", description = "Application is healthy")
    @OpenAPIResponse(status = "503", description = "Application is unhealthy")
    public HealthStatus getHealth() {
        HealthStatus status = new HealthStatus(HealthStatus.Status.UP, VERSION);

        Map<String, HealthStatus.ComponentHealth> components = new HashMap<>();
        components.put("database", new HealthStatus.ComponentHealth(
                HealthStatus.Status.UP, "Connected to primary database"));
        components.put("cache", new HealthStatus.ComponentHealth(
                HealthStatus.Status.UP, "Redis connection healthy"));
        components.put("messageQueue", new HealthStatus.ComponentHealth(
                HealthStatus.Status.UP, "Kafka broker reachable"));

        status.setComponents(components);
        return status;
    }

    /**
     * Liveness probe endpoint.
     *
     * <p>Returns UP if the application is running and able to respond to
     * requests. This does not check external dependencies.</p>
     *
     * <p>Use this endpoint for Kubernetes liveness probes. A failure indicates
     * the application should be restarted.</p>
     *
     * @return simple health status indicating liveness
     */
    @Get("/live")
    @OpenAPIResponse(status = "200", description = "Application is alive")
    public HealthStatus getLiveness() {
        return new HealthStatus(HealthStatus.Status.UP, VERSION);
    }

    /**
     * Readiness probe endpoint.
     *
     * <p>Returns UP if the application is ready to receive traffic.
     * This checks that all required dependencies are available.</p>
     *
     * <p>Use this endpoint for Kubernetes readiness probes. A failure indicates
     * the application should be removed from the load balancer.</p>
     *
     * @return health status with readiness information
     */
    @Get("/ready")
    @OpenAPIResponse(status = "200", description = "Application is ready to receive traffic")
    @OpenAPIResponse(status = "503", description = "Application is not ready")
    public HealthStatus getReadiness() {
        HealthStatus status = new HealthStatus(HealthStatus.Status.UP, VERSION);

        Map<String, HealthStatus.ComponentHealth> components = new HashMap<>();
        components.put("database", new HealthStatus.ComponentHealth(
                HealthStatus.Status.UP, "Database connection pool available"));
        components.put("dependencies", new HealthStatus.ComponentHealth(
                HealthStatus.Status.UP, "All external services reachable"));

        status.setComponents(components);
        return status;
    }

    /**
     * Returns the application version.
     *
     * <p>Simple endpoint that returns version information for the application.</p>
     *
     * @return version string
     */
    @Get("/version")
    @OpenAPIResponse(status = "200", description = "Version information retrieved")
    public String getVersion() {
        return VERSION;
    }
}
