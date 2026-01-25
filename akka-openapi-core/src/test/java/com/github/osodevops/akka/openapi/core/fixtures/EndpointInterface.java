package com.github.osodevops.akka.openapi.core.fixtures;

/**
 * Interface that should be excluded from scanning.
 */
@MockHttpEndpoint("/interface")
public interface EndpointInterface {
    String process();
}
