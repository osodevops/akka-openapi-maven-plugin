/**
 * Core library for generating OpenAPI 3.1 specifications from Akka SDK HTTP endpoint annotations.
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link com.github.osodevops.akka.openapi.core.AkkaEndpointScanner} - Scans classpath for endpoints</li>
 *   <li>{@link com.github.osodevops.akka.openapi.core.AkkaAnnotationExtractor} - Extracts annotation metadata</li>
 *   <li>{@link com.github.osodevops.akka.openapi.core.SchemaGenerator} - Generates JSON schemas from POJOs</li>
 *   <li>{@link com.github.osodevops.akka.openapi.core.OpenAPIModelBuilder} - Builds OpenAPI specification</li>
 *   <li>{@link com.github.osodevops.akka.openapi.core.OpenAPISerializer} - Serializes to YAML/JSON</li>
 *   <li>{@link com.github.osodevops.akka.openapi.core.OpenAPIValidator} - Validates specifications</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.github.osodevops.akka.openapi.core;
