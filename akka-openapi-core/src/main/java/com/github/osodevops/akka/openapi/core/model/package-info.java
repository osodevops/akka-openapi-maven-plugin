/**
 * Model classes representing extracted metadata from Akka SDK endpoints.
 *
 * <p>These are immutable value objects with Builder pattern for construction.</p>
 *
 * <h2>Model Hierarchy</h2>
 * <pre>
 * EndpointMetadata (class level)
 *   └── OperationMetadata (method level)
 *         ├── ParameterMetadata (path/query params)
 *         ├── RequestBodyMetadata (request body)
 *         └── ResponseMetadata (responses)
 * </pre>
 *
 * @since 1.0.0
 */
package com.github.osodevops.akka.openapi.core.model;
