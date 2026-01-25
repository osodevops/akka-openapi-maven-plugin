/**
 * Akka SDK HTTP annotation compatibility shim.
 *
 * <p>This package provides annotation interfaces compatible with the Akka SDK HTTP annotations.
 * These annotations can be used for compile-time compatibility when the actual Akka SDK
 * is not available. At runtime, the Akka SDK's actual annotations will be used when present.</p>
 *
 * <p>Annotations provided:</p>
 * <ul>
 *   <li>{@link HttpEndpoint} - Marks a class as an HTTP endpoint</li>
 *   <li>{@link Get} - HTTP GET method handler</li>
 *   <li>{@link Post} - HTTP POST method handler</li>
 *   <li>{@link Put} - HTTP PUT method handler</li>
 *   <li>{@link Delete} - HTTP DELETE method handler</li>
 *   <li>{@link Patch} - HTTP PATCH method handler</li>
 *   <li>{@link Head} - HTTP HEAD method handler</li>
 *   <li>{@link Options} - HTTP OPTIONS method handler</li>
 * </ul>
 */
package akka.javasdk.annotations.http;
