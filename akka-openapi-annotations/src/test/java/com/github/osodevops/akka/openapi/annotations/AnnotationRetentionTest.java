package com.github.osodevops.akka.openapi.annotations;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that all custom annotations have RUNTIME retention.
 */
class AnnotationRetentionTest {

    @Test
    void openAPIInfoShouldHaveRuntimeRetention() {
        assertThat(OpenAPIInfo.class.getAnnotation(Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void openAPIResponseShouldHaveRuntimeRetention() {
        assertThat(OpenAPIResponse.class.getAnnotation(Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void openAPITagShouldHaveRuntimeRetention() {
        assertThat(OpenAPITag.class.getAnnotation(Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void openAPIExampleShouldHaveRuntimeRetention() {
        assertThat(OpenAPIExample.class.getAnnotation(Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void openAPIServerShouldHaveRuntimeRetention() {
        assertThat(OpenAPIServer.class.getAnnotation(Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
    }
}
