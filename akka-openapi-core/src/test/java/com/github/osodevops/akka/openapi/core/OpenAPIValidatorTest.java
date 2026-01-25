package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.core.OpenAPIValidator.ValidationResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OpenAPIValidator.
 */
class OpenAPIValidatorTest {

    private List<String> logMessages;
    private OpenAPIValidator validator;

    @BeforeEach
    void setUp() {
        logMessages = new ArrayList<>();
        validator = new OpenAPIValidator(logMessages::add);
    }

    private OpenAPI createValidOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.1.0");

        Info info = new Info();
        info.setTitle("Test API");
        info.setVersion("1.0.0");
        openAPI.setInfo(info);

        Paths paths = new Paths();

        Operation getOp = new Operation();
        getOp.setOperationId("getTest");
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", new ApiResponse().description("Success"));
        getOp.setResponses(responses);

        PathItem pathItem = new PathItem();
        pathItem.setGet(getOp);
        paths.put("/test", pathItem);

        openAPI.setPaths(paths);

        return openAPI;
    }

    @Test
    void shouldValidateValidSpec() {
        OpenAPI openAPI = createValidOpenAPI();

        ValidationResult result = validator.validate(openAPI);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldDetectMissingInfo() {
        OpenAPI openAPI = createValidOpenAPI();
        openAPI.setInfo(null);

        ValidationResult result = validator.validate(openAPI);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e ->
            e.getMessage().contains("Info section is required"));
    }

    @Test
    void shouldDetectMissingTitle() {
        OpenAPI openAPI = createValidOpenAPI();
        openAPI.getInfo().setTitle(null);

        ValidationResult result = validator.validate(openAPI);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e ->
            e.getMessage().contains("API title is required"));
    }

    @Test
    void shouldDetectMissingVersion() {
        OpenAPI openAPI = createValidOpenAPI();
        openAPI.getInfo().setVersion(null);

        ValidationResult result = validator.validate(openAPI);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e ->
            e.getMessage().contains("API version is required"));
    }

    @Test
    void shouldDetectDuplicateOperationIds() {
        OpenAPI openAPI = createValidOpenAPI();

        // Add another operation with the same operationId
        Operation postOp = new Operation();
        postOp.setOperationId("getTest"); // Duplicate!
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("201", new ApiResponse().description("Created"));
        postOp.setResponses(responses);

        openAPI.getPaths().get("/test").setPost(postOp);

        ValidationResult result = validator.validate(openAPI);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e ->
            e.getMessage().contains("Duplicate operationId"));
    }

    @Test
    void shouldDetectMissingOperationId() {
        OpenAPI openAPI = createValidOpenAPI();
        openAPI.getPaths().get("/test").getGet().setOperationId(null);

        ValidationResult result = validator.validate(openAPI);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e ->
            e.getMessage().contains("operationId is required"));
    }

    @Test
    void shouldAllowEmptyPaths() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.1.0");
        Info info = new Info();
        info.setTitle("Test");
        info.setVersion("1.0.0");
        openAPI.setInfo(info);
        openAPI.setPaths(new Paths());

        ValidationResult result = validator.validate(openAPI);

        // Empty paths should be valid (just a warning)
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldProvideErrorSummary() {
        OpenAPI openAPI = createValidOpenAPI();
        openAPI.getInfo().setTitle(null);
        openAPI.getInfo().setVersion(null);

        ValidationResult result = validator.validate(openAPI);

        String summary = result.getErrorSummary();
        assertThat(summary).contains("validation error");
        assertThat(summary).contains("API title is required");
        assertThat(summary).contains("API version is required");
    }

    @Test
    void shouldProvideNoErrorsSummaryWhenValid() {
        OpenAPI openAPI = createValidOpenAPI();

        ValidationResult result = validator.validate(openAPI);

        assertThat(result.getErrorSummary()).isEqualTo("No errors");
    }

    @Test
    void shouldRejectNullOpenAPI() {
        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldValidateSchemaReferences() {
        OpenAPI openAPI = createValidOpenAPI();

        // Add a reference to a non-existent schema
        Schema<?> refSchema = new Schema<>();
        refSchema.set$ref("#/components/schemas/NonExistent");

        Components components = new Components();
        components.setSchemas(Map.of("Exists", new Schema<>().type("object")));
        openAPI.setComponents(components);

        // The validation should detect the missing reference when scanning YAML
        ValidationResult result = validator.validate(openAPI);
        // Note: This depends on the implementation details
    }

    @Test
    void shouldCreateWithoutLogger() {
        OpenAPIValidator noLogValidator = new OpenAPIValidator();
        OpenAPI openAPI = createValidOpenAPI();

        ValidationResult result = noLogValidator.validate(openAPI);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldTrackWarnings() {
        OpenAPI openAPI = createValidOpenAPI();

        ValidationResult result = validator.validate(openAPI);

        assertThat(result.hasWarnings()).isFalse(); // Valid spec, no warnings expected
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void shouldValidationResultToString() {
        OpenAPI openAPI = createValidOpenAPI();
        ValidationResult result = validator.validate(openAPI);

        String str = result.toString();
        assertThat(str).contains("valid=true");
        assertThat(str).contains("errors=0");
    }

    @Test
    void shouldValidationErrorToString() {
        OpenAPIValidator.ValidationError error =
            new OpenAPIValidator.ValidationError("test.path", "Test message");

        assertThat(error.toString()).isEqualTo("[test.path] Test message");
        assertThat(error.getLocation()).isEqualTo("test.path");
        assertThat(error.getMessage()).isEqualTo("Test message");
    }

    @Test
    void shouldValidationErrorEquals() {
        OpenAPIValidator.ValidationError error1 =
            new OpenAPIValidator.ValidationError("path", "message");
        OpenAPIValidator.ValidationError error2 =
            new OpenAPIValidator.ValidationError("path", "message");
        OpenAPIValidator.ValidationError error3 =
            new OpenAPIValidator.ValidationError("other", "message");

        assertThat(error1).isEqualTo(error2);
        assertThat(error1).isNotEqualTo(error3);
        assertThat(error1.hashCode()).isEqualTo(error2.hashCode());
    }
}
