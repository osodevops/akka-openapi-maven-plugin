package com.github.osodevops.akka.openapi.core.fixtures;

/**
 * Test fixture: field declared as a concrete subtype of the polymorphic Shape hierarchy.
 */
public record CircleShipment(String orderId, Circle circle) {}
