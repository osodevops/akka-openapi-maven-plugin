package com.github.osodevops.akka.openapi.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndpointMetadataTest {

    @Test
    void shouldBuildWithRequiredFields() {
        EndpointMetadata metadata = EndpointMetadata.builder()
            .className("com.example.CustomerEndpoint")
            .basePath("/customers")
            .build();

        assertThat(metadata.getClassName()).isEqualTo("com.example.CustomerEndpoint");
        assertThat(metadata.getBasePath()).isEqualTo("/customers");
        assertThat(metadata.getDescription()).isEmpty();
        assertThat(metadata.getTags()).isEmpty();
        assertThat(metadata.getOperations()).isEmpty();
    }

    @Test
    void shouldBuildWithAllFields() {
        OperationMetadata operation = OperationMetadata.builder()
            .methodName("getCustomer")
            .httpMethod(OperationMetadata.HttpMethod.GET)
            .path("/{id}")
            .build();

        SecurityMetadata security = SecurityMetadata.builder()
            .type(SecurityMetadata.SecurityType.BEARER)
            .build();

        EndpointMetadata metadata = EndpointMetadata.builder()
            .className("com.example.CustomerEndpoint")
            .basePath("/customers")
            .description("Customer management endpoint")
            .addTag("customers")
            .addTag("api")
            .addOperation(operation)
            .security(security)
            .build();

        assertThat(metadata.getClassName()).isEqualTo("com.example.CustomerEndpoint");
        assertThat(metadata.getSimpleClassName()).isEqualTo("CustomerEndpoint");
        assertThat(metadata.getBasePath()).isEqualTo("/customers");
        assertThat(metadata.getDescription()).isEqualTo("Customer management endpoint");
        assertThat(metadata.getTags()).containsExactly("customers", "api");
        assertThat(metadata.getOperations()).containsExactly(operation);
        assertThat(metadata.getSecurity()).isEqualTo(security);
        assertThat(metadata.hasOperations()).isTrue();
    }

    @Test
    void shouldRejectNullClassName() {
        assertThatThrownBy(() -> EndpointMetadata.builder()
            .basePath("/test")
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("className");
    }

    @Test
    void shouldRejectNullBasePath() {
        assertThatThrownBy(() -> EndpointMetadata.builder()
            .className("com.example.Test")
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("basePath");
    }

    @Test
    void tagsListShouldBeImmutable() {
        EndpointMetadata metadata = EndpointMetadata.builder()
            .className("com.example.Test")
            .basePath("/test")
            .addTag("tag1")
            .build();

        assertThatThrownBy(() -> metadata.getTags().add("tag2"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void operationsListShouldBeImmutable() {
        EndpointMetadata metadata = EndpointMetadata.builder()
            .className("com.example.Test")
            .basePath("/test")
            .build();

        OperationMetadata op = OperationMetadata.builder()
            .methodName("test")
            .httpMethod(OperationMetadata.HttpMethod.GET)
            .build();

        assertThatThrownBy(() -> metadata.getOperations().add(op))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldExtractSimpleClassName() {
        EndpointMetadata metadata = EndpointMetadata.builder()
            .className("com.example.deep.package.CustomerEndpoint")
            .basePath("/customers")
            .build();

        assertThat(metadata.getSimpleClassName()).isEqualTo("CustomerEndpoint");
    }

    @Test
    void shouldHandleSimpleClassNameWithoutPackage() {
        EndpointMetadata metadata = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .build();

        assertThat(metadata.getSimpleClassName()).isEqualTo("CustomerEndpoint");
    }

    @Test
    void shouldIgnoreBlankTags() {
        EndpointMetadata metadata = EndpointMetadata.builder()
            .className("com.example.Test")
            .basePath("/test")
            .addTag("valid")
            .addTag("")
            .addTag("   ")
            .addTag(null)
            .build();

        assertThat(metadata.getTags()).containsExactly("valid");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        EndpointMetadata metadata1 = EndpointMetadata.builder()
            .className("com.example.Test")
            .basePath("/test")
            .build();

        EndpointMetadata metadata2 = EndpointMetadata.builder()
            .className("com.example.Test")
            .basePath("/test")
            .build();

        EndpointMetadata metadata3 = EndpointMetadata.builder()
            .className("com.example.Other")
            .basePath("/test")
            .build();

        assertThat(metadata1).isEqualTo(metadata2);
        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
        assertThat(metadata1).isNotEqualTo(metadata3);
    }

    @Test
    void shouldImplementToString() {
        EndpointMetadata metadata = EndpointMetadata.builder()
            .className("com.example.CustomerEndpoint")
            .basePath("/customers")
            .addTag("customers")
            .build();

        assertThat(metadata.toString())
            .contains("CustomerEndpoint")
            .contains("/customers")
            .contains("customers");
    }
}
