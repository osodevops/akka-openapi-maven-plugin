package com.github.osodevops.akka.openapi.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationMetadataTest {

    @Test
    void shouldBuildWithRequiredFields() {
        OperationMetadata metadata = OperationMetadata.builder()
            .methodName("getCustomer")
            .httpMethod(OperationMetadata.HttpMethod.GET)
            .build();

        assertThat(metadata.getMethodName()).isEqualTo("getCustomer");
        assertThat(metadata.getHttpMethod()).isEqualTo(OperationMetadata.HttpMethod.GET);
        assertThat(metadata.getPath()).isEmpty();
        assertThat(metadata.getOperationId()).isEqualTo("getCustomer"); // defaults to methodName
        assertThat(metadata.getSummary()).isEmpty();
        assertThat(metadata.getDescription()).isEmpty();
        assertThat(metadata.getParameters()).isEmpty();
        assertThat(metadata.getRequestBody()).isNull();
        assertThat(metadata.getResponses()).isEmpty();
        assertThat(metadata.getTags()).isEmpty();
        assertThat(metadata.isDeprecated()).isFalse();
    }

    @Test
    void shouldBuildWithAllFields() {
        ParameterMetadata param = ParameterMetadata.builder()
            .name("id")
            .location(ParameterMetadata.ParameterLocation.PATH)
            .javaType(String.class)
            .required(true)
            .build();

        RequestBodyMetadata requestBody = RequestBodyMetadata.builder()
            .javaType(String.class)
            .build();

        ResponseMetadata response = ResponseMetadata.builder()
            .statusCode("200")
            .description("Success")
            .build();

        OperationMetadata metadata = OperationMetadata.builder()
            .methodName("updateCustomer")
            .httpMethod(OperationMetadata.HttpMethod.PUT)
            .path("/{id}")
            .operationId("updateCustomerById")
            .summary("Update a customer")
            .description("Updates an existing customer by ID")
            .addParameter(param)
            .requestBody(requestBody)
            .addResponse("200", response)
            .addTag("customers")
            .deprecated(true)
            .build();

        assertThat(metadata.getMethodName()).isEqualTo("updateCustomer");
        assertThat(metadata.getHttpMethod()).isEqualTo(OperationMetadata.HttpMethod.PUT);
        assertThat(metadata.getPath()).isEqualTo("/{id}");
        assertThat(metadata.getOperationId()).isEqualTo("updateCustomerById");
        assertThat(metadata.getSummary()).isEqualTo("Update a customer");
        assertThat(metadata.getDescription()).isEqualTo("Updates an existing customer by ID");
        assertThat(metadata.getParameters()).containsExactly(param);
        assertThat(metadata.getRequestBody()).isEqualTo(requestBody);
        assertThat(metadata.hasRequestBody()).isTrue();
        assertThat(metadata.getResponses()).containsEntry("200", response);
        assertThat(metadata.getTags()).containsExactly("customers");
        assertThat(metadata.isDeprecated()).isTrue();
    }

    @Test
    void shouldRejectNullMethodName() {
        assertThatThrownBy(() -> OperationMetadata.builder()
            .httpMethod(OperationMetadata.HttpMethod.GET)
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("methodName");
    }

    @Test
    void shouldRejectNullHttpMethod() {
        assertThatThrownBy(() -> OperationMetadata.builder()
            .methodName("test")
            .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("httpMethod");
    }

    @Test
    void parametersListShouldBeImmutable() {
        OperationMetadata metadata = OperationMetadata.builder()
            .methodName("test")
            .httpMethod(OperationMetadata.HttpMethod.GET)
            .build();

        ParameterMetadata param = ParameterMetadata.builder()
            .name("id")
            .location(ParameterMetadata.ParameterLocation.PATH)
            .javaType(String.class)
            .build();

        assertThatThrownBy(() -> metadata.getParameters().add(param))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void responsesMapShouldBeImmutable() {
        OperationMetadata metadata = OperationMetadata.builder()
            .methodName("test")
            .httpMethod(OperationMetadata.HttpMethod.GET)
            .build();

        ResponseMetadata response = ResponseMetadata.builder()
            .statusCode("200")
            .build();

        assertThatThrownBy(() -> metadata.getResponses().put("200", response))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldSupportAllHttpMethods() {
        for (OperationMetadata.HttpMethod method : OperationMetadata.HttpMethod.values()) {
            OperationMetadata metadata = OperationMetadata.builder()
                .methodName("test")
                .httpMethod(method)
                .build();

            assertThat(metadata.getHttpMethod()).isEqualTo(method);
        }
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        OperationMetadata metadata1 = OperationMetadata.builder()
            .methodName("test")
            .httpMethod(OperationMetadata.HttpMethod.GET)
            .path("/test")
            .build();

        OperationMetadata metadata2 = OperationMetadata.builder()
            .methodName("test")
            .httpMethod(OperationMetadata.HttpMethod.GET)
            .path("/test")
            .build();

        OperationMetadata metadata3 = OperationMetadata.builder()
            .methodName("test")
            .httpMethod(OperationMetadata.HttpMethod.POST)
            .path("/test")
            .build();

        assertThat(metadata1).isEqualTo(metadata2);
        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
        assertThat(metadata1).isNotEqualTo(metadata3);
    }
}
