package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.core.config.PluginConfiguration;
import com.github.osodevops.akka.openapi.core.config.ServerConfig;
import com.github.osodevops.akka.openapi.core.fixtures.CustomerDto;
import com.github.osodevops.akka.openapi.core.fixtures.CreateCustomerRequest;
import com.github.osodevops.akka.openapi.core.model.*;
import com.github.osodevops.akka.openapi.core.model.OperationMetadata.HttpMethod;
import com.github.osodevops.akka.openapi.core.model.ParameterMetadata.ParameterLocation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OpenAPIModelBuilder.
 */
class OpenAPIModelBuilderTest {

    private List<String> logMessages;
    private PluginConfiguration config;
    private OpenAPIModelBuilder builder;

    @BeforeEach
    void setUp() {
        logMessages = new ArrayList<>();
        config = PluginConfiguration.builder()
            .apiTitle("Test API")
            .apiVersion("1.0.0")
            .apiDescription("Test API description")
            .build();
        builder = new OpenAPIModelBuilder(config, logMessages::add);
    }

    @Test
    void shouldRejectNullConfig() {
        assertThatThrownBy(() -> new OpenAPIModelBuilder(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("config must not be null");
    }

    @Test
    void shouldRejectNullEndpoints() {
        assertThatThrownBy(() -> builder.build(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("endpoints must not be null");
    }

    @Test
    void shouldBuildEmptySpecWithNoEndpoints() {
        OpenAPI openAPI = builder.build(List.of());

        assertThat(openAPI.getOpenapi()).isEqualTo("3.1.0");
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Test API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getPaths()).isEmpty();
    }

    @Test
    void shouldBuildInfoSection() {
        config = PluginConfiguration.builder()
            .apiTitle("Customer API")
            .apiVersion("2.0.0")
            .apiDescription("API for customer management")
            .contactName("John Doe")
            .contactEmail("john@example.com")
            .contactUrl("https://example.com")
            .licenseName("Apache 2.0")
            .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
            .termsOfService("https://example.com/terms")
            .build();

        builder = new OpenAPIModelBuilder(config);
        OpenAPI openAPI = builder.build(List.of());

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Customer API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("2.0.0");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("API for customer management");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("John Doe");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("john@example.com");
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("Apache 2.0");
        assertThat(openAPI.getInfo().getTermsOfService()).isEqualTo("https://example.com/terms");
    }

    @Test
    void shouldBuildServersSection() {
        ServerConfig server1 = ServerConfig.builder()
            .url("https://api.example.com")
            .description("Production server")
            .build();
        ServerConfig server2 = ServerConfig.builder()
            .url("https://staging.example.com")
            .description("Staging server")
            .build();

        config = PluginConfiguration.builder()
            .apiTitle("Test API")
            .apiVersion("1.0.0")
            .addServer(server1)
            .addServer(server2)
            .build();

        builder = new OpenAPIModelBuilder(config);
        OpenAPI openAPI = builder.build(List.of());

        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("https://api.example.com");
        assertThat(openAPI.getServers().get(1).getUrl()).isEqualTo("https://staging.example.com");
    }

    @Test
    void shouldBuildPathsFromEndpoints() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addTag("Customer")
            .addOperation(OperationMetadata.builder()
                .methodName("getCustomer")
                .httpMethod(HttpMethod.GET)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Customer found")
                    .responseType(CustomerDto.class)
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint));

        assertThat(openAPI.getPaths()).containsKey("/customers/{id}");
        PathItem pathItem = openAPI.getPaths().get("/customers/{id}");
        assertThat(pathItem.getGet()).isNotNull();
        assertThat(pathItem.getGet().getOperationId()).isEqualTo("getCustomer");
    }

    @Test
    void shouldBuildOperationWithParameters() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addOperation(OperationMetadata.builder()
                .methodName("searchCustomers")
                .httpMethod(HttpMethod.GET)
                .path("")
                .summary("Search customers")
                .description("Search for customers by query")
                .addParameter(ParameterMetadata.builder()
                    .name("query")
                    .location(ParameterLocation.QUERY)
                    .javaType(String.class)
                    .required(false)
                    .description("Search query")
                    .build())
                .addParameter(ParameterMetadata.builder()
                    .name("limit")
                    .location(ParameterLocation.QUERY)
                    .javaType(Integer.class)
                    .required(false)
                    .defaultValue(10)
                    .build())
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint));

        Operation operation = openAPI.getPaths().get("/customers").getGet();
        assertThat(operation.getSummary()).isEqualTo("Search customers");
        assertThat(operation.getDescription()).isEqualTo("Search for customers by query");
        assertThat(operation.getParameters()).hasSize(2);

        assertThat(operation.getParameters().get(0).getName()).isEqualTo("query");
        assertThat(operation.getParameters().get(0).getIn()).isEqualTo("query");
        assertThat(operation.getParameters().get(0).getRequired()).isFalse();

        assertThat(operation.getParameters().get(1).getName()).isEqualTo("limit");
        assertThat(operation.getParameters().get(1).getSchema().getDefault()).isEqualTo(10);
    }

    @Test
    void shouldBuildOperationWithRequestBody() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addOperation(OperationMetadata.builder()
                .methodName("createCustomer")
                .httpMethod(HttpMethod.POST)
                .path("")
                .requestBody(RequestBodyMetadata.builder()
                    .javaType(CreateCustomerRequest.class)
                    .required(true)
                    .description("Customer to create")
                    .build())
                .addResponse(ResponseMetadata.builder()
                    .statusCode("201")
                    .description("Customer created")
                    .responseType(CustomerDto.class)
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint));

        Operation operation = openAPI.getPaths().get("/customers").getPost();
        assertThat(operation.getRequestBody()).isNotNull();
        assertThat(operation.getRequestBody().getRequired()).isTrue();
        assertThat(operation.getRequestBody().getDescription()).isEqualTo("Customer to create");
        assertThat(operation.getRequestBody().getContent()).containsKey("application/json");
    }

    @Test
    void shouldBuildComponentsWithSchemas() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addOperation(OperationMetadata.builder()
                .methodName("getCustomer")
                .httpMethod(HttpMethod.GET)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .responseType(CustomerDto.class)
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint));

        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSchemas()).isNotNull();
        assertThat(openAPI.getComponents().getSchemas()).containsKey("CustomerDto");
    }

    @Test
    void shouldBuildTags() {
        EndpointMetadata endpoint1 = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addTag("Customer")
            .addOperation(OperationMetadata.builder()
                .methodName("getCustomer")
                .httpMethod(HttpMethod.GET)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .build();

        EndpointMetadata endpoint2 = EndpointMetadata.builder()
            .className("OrderEndpoint")
            .basePath("/orders")
            .addTag("Order")
            .addOperation(OperationMetadata.builder()
                .methodName("getOrder")
                .httpMethod(HttpMethod.GET)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint1, endpoint2));

        assertThat(openAPI.getTags()).hasSize(2);
        assertThat(openAPI.getTags().stream().map(t -> t.getName()))
            .containsExactly("Customer", "Order");
    }

    @Test
    void shouldGenerateUniqueOperationIds() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addOperation(OperationMetadata.builder()
                .methodName("get")
                .httpMethod(HttpMethod.GET)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .addOperation(OperationMetadata.builder()
                .methodName("get")
                .httpMethod(HttpMethod.GET)
                .path("")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint));

        Operation op1 = openAPI.getPaths().get("/customers/{id}").getGet();
        Operation op2 = openAPI.getPaths().get("/customers").getGet();

        assertThat(op1.getOperationId()).isNotEqualTo(op2.getOperationId());
        assertThat(op1.getOperationId()).isEqualTo("get");
        assertThat(op2.getOperationId()).isEqualTo("get_1");
    }

    @Test
    void shouldMarkDeprecatedOperations() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addOperation(OperationMetadata.builder()
                .methodName("getOldCustomer")
                .httpMethod(HttpMethod.GET)
                .path("/old/{id}")
                .deprecated(true)
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint));

        Operation operation = openAPI.getPaths().get("/customers/old/{id}").getGet();
        assertThat(operation.getDeprecated()).isTrue();
    }

    @Test
    void shouldNormalizePaths() {
        assertThat(OpenAPIModelBuilder.normalizePath("/customers/", "/{id}"))
            .isEqualTo("/customers/{id}");
        assertThat(OpenAPIModelBuilder.normalizePath("/customers", "{id}"))
            .isEqualTo("/customers/{id}");
        assertThat(OpenAPIModelBuilder.normalizePath("customers", "id"))
            .isEqualTo("/customers/id");
        assertThat(OpenAPIModelBuilder.normalizePath("", "/customers"))
            .isEqualTo("/customers");
        assertThat(OpenAPIModelBuilder.normalizePath("/", "/"))
            .isEqualTo("/");
        assertThat(OpenAPIModelBuilder.normalizePath(null, null))
            .isEqualTo("/");
        assertThat(OpenAPIModelBuilder.normalizePath("/api//v1", "//users"))
            .isEqualTo("/api/v1/users");
    }

    @Test
    void shouldResetBuilder() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addOperation(OperationMetadata.builder()
                .methodName("getCustomer")
                .httpMethod(HttpMethod.GET)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .responseType(CustomerDto.class)
                    .build())
                .build())
            .build();

        builder.build(List.of(endpoint));
        builder.reset();

        // Build again - operation IDs should restart
        OpenAPI openAPI = builder.build(List.of(endpoint));
        Operation operation = openAPI.getPaths().get("/customers/{id}").getGet();
        assertThat(operation.getOperationId()).isEqualTo("getCustomer");
    }

    @Test
    void shouldHandleMultipleHttpMethodsOnSamePath() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addOperation(OperationMetadata.builder()
                .methodName("getCustomer")
                .httpMethod(HttpMethod.GET)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .addOperation(OperationMetadata.builder()
                .methodName("updateCustomer")
                .httpMethod(HttpMethod.PUT)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .addOperation(OperationMetadata.builder()
                .methodName("deleteCustomer")
                .httpMethod(HttpMethod.DELETE)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("204")
                    .description("Deleted")
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint));

        PathItem pathItem = openAPI.getPaths().get("/customers/{id}");
        assertThat(pathItem.getGet()).isNotNull();
        assertThat(pathItem.getPut()).isNotNull();
        assertThat(pathItem.getDelete()).isNotNull();
        assertThat(pathItem.getPost()).isNull();
    }

    @Test
    void shouldUseCustomOperationId() {
        EndpointMetadata endpoint = EndpointMetadata.builder()
            .className("CustomerEndpoint")
            .basePath("/customers")
            .addOperation(OperationMetadata.builder()
                .methodName("findById")
                .operationId("getCustomerById")
                .httpMethod(HttpMethod.GET)
                .path("/{id}")
                .addResponse(ResponseMetadata.builder()
                    .statusCode("200")
                    .description("Success")
                    .build())
                .build())
            .build();

        OpenAPI openAPI = builder.build(List.of(endpoint));

        Operation operation = openAPI.getPaths().get("/customers/{id}").getGet();
        assertThat(operation.getOperationId()).isEqualTo("getCustomerById");
    }

    @Test
    void shouldCreateBuilderWithoutLogger() {
        OpenAPIModelBuilder noLogBuilder = new OpenAPIModelBuilder(config);
        OpenAPI openAPI = noLogBuilder.build(List.of());

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getOpenapi()).isEqualTo("3.1.0");
    }
}
