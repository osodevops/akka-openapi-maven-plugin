package com.github.osodevops.akka.openapi.core.fixtures;

/**
 * Abstract endpoint that should be excluded from scanning.
 */
@MockHttpEndpoint("/abstract")
public abstract class AbstractEndpoint {

    public abstract String process();
}
