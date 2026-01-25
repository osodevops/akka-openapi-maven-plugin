package com.github.osodevops.akka.openapi.core.fixtures;

/**
 * Regular class without endpoint annotation - should not be scanned.
 */
public class NotAnEndpoint {

    public String doSomething() {
        return "Not an endpoint";
    }
}
