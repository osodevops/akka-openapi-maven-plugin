package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.annotations.*;
import com.github.osodevops.akka.openapi.core.fixtures.AnnotatedEndpoint;
import com.github.osodevops.akka.openapi.core.fixtures.CustomerDto;
import com.github.osodevops.akka.openapi.core.fixtures.SimpleEndpoint;
import com.github.osodevops.akka.openapi.core.model.InfoMetadata;
import com.github.osodevops.akka.openapi.core.model.ServerMetadata;
import com.github.osodevops.akka.openapi.core.model.TagMetadata;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that OpenAPI annotations (@OpenAPITag, @OpenAPIResponse, @OpenAPIExample)
 * are correctly read from classes and methods.
 *
 * These tests verify the annotation extraction at the reflection level,
 * since the full extractor pipeline requires real Akka SDK annotations.
 */
class OpenAPIAnnotationExtractorTest {

    @Test
    void shouldReadOpenAPITagFromClass() {
        OpenAPITag tag = AnnotatedEndpoint.class.getAnnotation(OpenAPITag.class);

        assertThat(tag).isNotNull();
        assertThat(tag.name()).isEqualTo("Customers");
        assertThat(tag.description()).isEqualTo("Customer management operations");
        assertThat(tag.externalDocsUrl()).isEqualTo("https://docs.example.com/customers");
        assertThat(tag.externalDocsDescription()).isEqualTo("Customer API Guide");
    }

    @Test
    void shouldReturnNullWhenNoOpenAPITag() {
        OpenAPITag tag = SimpleEndpoint.class.getAnnotation(OpenAPITag.class);

        assertThat(tag).isNull();
    }

    @Test
    void shouldReadSingleOpenAPIResponse() throws Exception {
        Method method = AnnotatedEndpoint.class.getMethod("listCustomers");
        OpenAPIResponse[] responses = method.getAnnotationsByType(OpenAPIResponse.class);

        assertThat(responses).hasSize(1);
        assertThat(responses[0].status()).isEqualTo("200");
        assertThat(responses[0].description()).isEqualTo("Successfully retrieved customer list");
        assertThat(responses[0].responseType()).isEqualTo(Void.class); // default
        assertThat(responses[0].mediaType()).isEqualTo("application/json"); // default
    }

    @Test
    void shouldReadMultipleOpenAPIResponses() throws Exception {
        Method method = AnnotatedEndpoint.class.getMethod("getCustomer", String.class);
        OpenAPIResponse[] responses = method.getAnnotationsByType(OpenAPIResponse.class);

        assertThat(responses).hasSize(2);

        assertThat(responses[0].status()).isEqualTo("200");
        assertThat(responses[0].description()).isEqualTo("Customer found");

        assertThat(responses[1].status()).isEqualTo("404");
        assertThat(responses[1].description()).isEqualTo("Customer not found");
    }

    @Test
    void shouldReadThreeOpenAPIResponsesIncluding409() throws Exception {
        Method method = AnnotatedEndpoint.class.getMethod("createCustomer",
            com.github.osodevops.akka.openapi.core.fixtures.CreateCustomerRequest.class);
        OpenAPIResponse[] responses = method.getAnnotationsByType(OpenAPIResponse.class);

        assertThat(responses).hasSize(3);
        assertThat(responses[0].status()).isEqualTo("201");
        assertThat(responses[1].status()).isEqualTo("400");
        assertThat(responses[2].status()).isEqualTo("409");
        assertThat(responses[2].description()).isEqualTo("Customer with this email already exists");
    }

    @Test
    void shouldReadOpenAPIExample() throws Exception {
        Method method = AnnotatedEndpoint.class.getMethod("createCustomer",
            com.github.osodevops.akka.openapi.core.fixtures.CreateCustomerRequest.class);
        OpenAPIExample[] examples = method.getAnnotationsByType(OpenAPIExample.class);

        assertThat(examples).hasSize(1);
        assertThat(examples[0].name()).isEqualTo("newCustomer");
        assertThat(examples[0].summary()).isEqualTo("Create a new customer");
        assertThat(examples[0].value()).isEqualTo("{\"name\": \"John Doe\", \"email\": \"john@example.com\"}");
    }

    @Test
    void shouldReturnEmptyArrayWhenNoOpenAPIResponseAnnotations() throws Exception {
        Method method = SimpleEndpoint.class.getMethod("hello");
        OpenAPIResponse[] responses = method.getAnnotationsByType(OpenAPIResponse.class);

        assertThat(responses).isEmpty();
    }

    @Test
    void shouldReturnEmptyArrayWhenNoOpenAPIExampleAnnotations() throws Exception {
        Method method = AnnotatedEndpoint.class.getMethod("listCustomers");
        OpenAPIExample[] examples = method.getAnnotationsByType(OpenAPIExample.class);

        assertThat(examples).isEmpty();
    }

    @Test
    void shouldCreateTagMetadataFromAnnotation() {
        OpenAPITag tag = AnnotatedEndpoint.class.getAnnotation(OpenAPITag.class);
        TagMetadata metadata = new TagMetadata(
            tag.name(), tag.description(),
            tag.externalDocsUrl(), tag.externalDocsDescription()
        );

        assertThat(metadata.getName()).isEqualTo("Customers");
        assertThat(metadata.getDescription()).isEqualTo("Customer management operations");
        assertThat(metadata.getExternalDocsUrl()).isEqualTo("https://docs.example.com/customers");
        assertThat(metadata.getExternalDocsDescription()).isEqualTo("Customer API Guide");
    }

    @Test
    void shouldReadOpenAPIInfoFromClass() {
        OpenAPIInfo info = AnnotatedEndpoint.class.getAnnotation(OpenAPIInfo.class);

        assertThat(info).isNotNull();
        assertThat(info.title()).isEqualTo("Customer API");
        assertThat(info.version()).isEqualTo("2.0.0");
        assertThat(info.description()).isEqualTo("API for managing customer records");
        assertThat(info.termsOfService()).isEqualTo("https://example.com/terms");
        assertThat(info.contactName()).isEqualTo("API Support");
        assertThat(info.contactEmail()).isEqualTo("support@example.com");
        assertThat(info.contactUrl()).isEqualTo("https://example.com/support");
        assertThat(info.licenseName()).isEqualTo("Apache 2.0");
        assertThat(info.licenseUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
    }

    @Test
    void shouldReturnNullWhenNoOpenAPIInfo() {
        OpenAPIInfo info = SimpleEndpoint.class.getAnnotation(OpenAPIInfo.class);
        assertThat(info).isNull();
    }

    @Test
    void shouldCreateInfoMetadataFromAnnotation() {
        OpenAPIInfo info = AnnotatedEndpoint.class.getAnnotation(OpenAPIInfo.class);
        InfoMetadata metadata = InfoMetadata.builder()
            .title(info.title())
            .version(info.version())
            .description(info.description())
            .termsOfService(info.termsOfService())
            .contactName(info.contactName())
            .contactEmail(info.contactEmail())
            .contactUrl(info.contactUrl())
            .licenseName(info.licenseName())
            .licenseUrl(info.licenseUrl())
            .build();

        assertThat(metadata.getTitle()).isEqualTo("Customer API");
        assertThat(metadata.getVersion()).isEqualTo("2.0.0");
        assertThat(metadata.getContactEmail()).isEqualTo("support@example.com");
        assertThat(metadata.getLicenseName()).isEqualTo("Apache 2.0");
        assertThat(metadata.hasContent()).isTrue();
    }

    @Test
    void shouldReadOpenAPIServersFromClass() {
        OpenAPIServer[] servers = AnnotatedEndpoint.class.getAnnotationsByType(OpenAPIServer.class);

        assertThat(servers).hasSize(2);
        assertThat(servers[0].url()).isEqualTo("https://api.example.com");
        assertThat(servers[0].description()).isEqualTo("Production");
        assertThat(servers[1].url()).isEqualTo("https://api-staging.example.com");
        assertThat(servers[1].description()).isEqualTo("Staging");
    }

    @Test
    void shouldReturnEmptyArrayWhenNoOpenAPIServers() {
        OpenAPIServer[] servers = SimpleEndpoint.class.getAnnotationsByType(OpenAPIServer.class);
        assertThat(servers).isEmpty();
    }

    @Test
    void shouldCreateServerMetadataFromAnnotation() {
        OpenAPIServer[] servers = AnnotatedEndpoint.class.getAnnotationsByType(OpenAPIServer.class);
        ServerMetadata metadata = new ServerMetadata(servers[0].url(), servers[0].description());

        assertThat(metadata.getUrl()).isEqualTo("https://api.example.com");
        assertThat(metadata.getDescription()).isEqualTo("Production");
    }
}
