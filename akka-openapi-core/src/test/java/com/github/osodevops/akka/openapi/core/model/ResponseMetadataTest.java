package com.github.osodevops.akka.openapi.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseMetadataTest {

    @Test
    void shouldBuildWithRequiredFields() {
        ResponseMetadata metadata = ResponseMetadata.builder()
            .statusCode("200")
            .build();

        assertThat(metadata.getStatusCode()).isEqualTo("200");
        assertThat(metadata.getDescription()).isEmpty();
        assertThat(metadata.getResponseType()).isNull();
        assertThat(metadata.getMediaType()).isEqualTo("application/json");
        assertThat(metadata.getExamples()).isEmpty();
        assertThat(metadata.hasBody()).isFalse();
    }

    @Test
    void shouldBuildWithAllFields() {
        ResponseMetadata metadata = ResponseMetadata.builder()
            .statusCode("200")
            .description("Customer found")
            .responseType(String.class)
            .mediaType("application/xml")
            .addExample("default", "{\"id\": \"123\"}")
            .build();

        assertThat(metadata.getStatusCode()).isEqualTo("200");
        assertThat(metadata.getDescription()).isEqualTo("Customer found");
        assertThat(metadata.getResponseType()).isEqualTo(String.class);
        assertThat(metadata.getMediaType()).isEqualTo("application/xml");
        assertThat(metadata.getExamples()).containsEntry("default", "{\"id\": \"123\"}");
        assertThat(metadata.hasBody()).isTrue();
    }

    @Test
    void shouldRejectNullStatusCode() {
        assertThatThrownBy(() -> ResponseMetadata.builder().build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("statusCode");
    }

    @Test
    void shouldDetectBodyWithVoidType() {
        ResponseMetadata metadata = ResponseMetadata.builder()
            .statusCode("204")
            .responseType(Void.class)
            .build();

        assertThat(metadata.hasBody()).isFalse();
    }

    @Test
    void shouldDetectBodyWithVoidPrimitive() {
        ResponseMetadata metadata = ResponseMetadata.builder()
            .statusCode("204")
            .responseType(void.class)
            .build();

        assertThat(metadata.hasBody()).isFalse();
    }

    @Test
    void examplesMapShouldBeImmutable() {
        ResponseMetadata metadata = ResponseMetadata.builder()
            .statusCode("200")
            .build();

        assertThatThrownBy(() -> metadata.getExamples().put("new", "value"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        ResponseMetadata metadata1 = ResponseMetadata.builder()
            .statusCode("200")
            .responseType(String.class)
            .build();

        ResponseMetadata metadata2 = ResponseMetadata.builder()
            .statusCode("200")
            .responseType(String.class)
            .build();

        ResponseMetadata metadata3 = ResponseMetadata.builder()
            .statusCode("404")
            .build();

        assertThat(metadata1).isEqualTo(metadata2);
        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
        assertThat(metadata1).isNotEqualTo(metadata3);
    }

    @Test
    void shouldImplementToString() {
        ResponseMetadata metadata = ResponseMetadata.builder()
            .statusCode("200")
            .description("Success")
            .responseType(List.class)
            .build();

        assertThat(metadata.toString())
            .contains("200")
            .contains("Success")
            .contains("List");
    }
}
