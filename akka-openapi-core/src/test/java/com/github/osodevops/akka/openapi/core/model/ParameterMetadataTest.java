package com.github.osodevops.akka.openapi.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterMetadataTest {

    @Test
    void shouldBuildWithRequiredFields() {
        ParameterMetadata metadata = ParameterMetadata.builder()
            .name("id")
            .location(ParameterMetadata.ParameterLocation.PATH)
            .javaType(String.class)
            .build();

        assertThat(metadata.getName()).isEqualTo("id");
        assertThat(metadata.getLocation()).isEqualTo(ParameterMetadata.ParameterLocation.PATH);
        assertThat(metadata.getJavaType()).isEqualTo(String.class);
        assertThat(metadata.getDescription()).isEmpty();
        assertThat(metadata.isRequired()).isFalse();
        assertThat(metadata.getDefaultValue()).isNull();
        assertThat(metadata.getExample()).isNull();
        assertThat(metadata.getFormat()).isNull();
    }

    @Test
    void shouldBuildWithAllFields() {
        ParameterMetadata metadata = ParameterMetadata.builder()
            .name("page")
            .location(ParameterMetadata.ParameterLocation.QUERY)
            .javaType(Integer.class)
            .description("Page number for pagination")
            .required(false)
            .defaultValue(1)
            .example(5)
            .format("int32")
            .build();

        assertThat(metadata.getName()).isEqualTo("page");
        assertThat(metadata.getLocation()).isEqualTo(ParameterMetadata.ParameterLocation.QUERY);
        assertThat(metadata.getJavaType()).isEqualTo(Integer.class);
        assertThat(metadata.getDescription()).isEqualTo("Page number for pagination");
        assertThat(metadata.isRequired()).isFalse();
        assertThat(metadata.getDefaultValue()).isEqualTo(1);
        assertThat(metadata.getExample()).isEqualTo(5);
        assertThat(metadata.getFormat()).isEqualTo("int32");
    }

    @Test
    void shouldRejectNullName() {
        assertThatThrownBy(() -> ParameterMetadata.builder()
            .location(ParameterMetadata.ParameterLocation.PATH)
            .javaType(String.class)
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("name");
    }

    @Test
    void shouldRejectNullLocation() {
        assertThatThrownBy(() -> ParameterMetadata.builder()
            .name("id")
            .javaType(String.class)
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("location");
    }

    @Test
    void shouldRejectNullJavaType() {
        assertThatThrownBy(() -> ParameterMetadata.builder()
            .name("id")
            .location(ParameterMetadata.ParameterLocation.PATH)
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("javaType");
    }

    @Test
    void shouldSupportAllParameterLocations() {
        for (ParameterMetadata.ParameterLocation location : ParameterMetadata.ParameterLocation.values()) {
            ParameterMetadata metadata = ParameterMetadata.builder()
                .name("test")
                .location(location)
                .javaType(String.class)
                .build();

            assertThat(metadata.getLocation()).isEqualTo(location);
        }
    }

    @Test
    void parameterLocationShouldHaveCorrectOpenApiValue() {
        assertThat(ParameterMetadata.ParameterLocation.PATH.getOpenApiValue()).isEqualTo("path");
        assertThat(ParameterMetadata.ParameterLocation.QUERY.getOpenApiValue()).isEqualTo("query");
        assertThat(ParameterMetadata.ParameterLocation.HEADER.getOpenApiValue()).isEqualTo("header");
        assertThat(ParameterMetadata.ParameterLocation.COOKIE.getOpenApiValue()).isEqualTo("cookie");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        ParameterMetadata metadata1 = ParameterMetadata.builder()
            .name("id")
            .location(ParameterMetadata.ParameterLocation.PATH)
            .javaType(String.class)
            .required(true)
            .build();

        ParameterMetadata metadata2 = ParameterMetadata.builder()
            .name("id")
            .location(ParameterMetadata.ParameterLocation.PATH)
            .javaType(String.class)
            .required(true)
            .build();

        ParameterMetadata metadata3 = ParameterMetadata.builder()
            .name("id")
            .location(ParameterMetadata.ParameterLocation.QUERY)
            .javaType(String.class)
            .required(true)
            .build();

        assertThat(metadata1).isEqualTo(metadata2);
        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
        assertThat(metadata1).isNotEqualTo(metadata3);
    }

    @Test
    void shouldImplementToString() {
        ParameterMetadata metadata = ParameterMetadata.builder()
            .name("customerId")
            .location(ParameterMetadata.ParameterLocation.PATH)
            .javaType(String.class)
            .required(true)
            .build();

        assertThat(metadata.toString())
            .contains("customerId")
            .contains("PATH")
            .contains("String")
            .contains("required=true");
    }
}
