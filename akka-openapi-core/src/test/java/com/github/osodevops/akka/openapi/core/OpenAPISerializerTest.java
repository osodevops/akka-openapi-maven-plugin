package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.core.OpenAPISerializer.Format;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OpenAPISerializer.
 */
class OpenAPISerializerTest {

    private List<String> logMessages;
    private OpenAPISerializer serializer;

    @BeforeEach
    void setUp() {
        logMessages = new ArrayList<>();
        serializer = new OpenAPISerializer(logMessages::add);
    }

    private OpenAPI createSimpleOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.1.0");

        Info info = new Info();
        info.setTitle("Test API");
        info.setVersion("1.0.0");
        info.setDescription("A test API");
        openAPI.setInfo(info);

        Paths paths = new Paths();
        paths.put("/test", new PathItem());
        openAPI.setPaths(paths);

        return openAPI;
    }

    @Test
    void shouldSerializeToYaml() {
        OpenAPI openAPI = createSimpleOpenAPI();

        String yaml = serializer.toYaml(openAPI);

        assertThat(yaml).isNotEmpty();
        assertThat(yaml).contains("openapi: 3.1.0");
        assertThat(yaml).contains("title: Test API");
        assertThat(yaml).contains("version: 1.0.0");
        assertThat(yaml).contains("/test:");
    }

    @Test
    void shouldSerializeToJson() {
        OpenAPI openAPI = createSimpleOpenAPI();

        String json = serializer.toJson(openAPI);

        assertThat(json).isNotEmpty();
        assertThat(json).contains("\"openapi\"");
        assertThat(json).contains("\"3.1.0\"");
        assertThat(json).contains("\"title\"");
        assertThat(json).contains("\"Test API\"");
    }

    @Test
    void shouldSerializeWithFormatEnum() {
        OpenAPI openAPI = createSimpleOpenAPI();

        String yaml = serializer.serialize(openAPI, Format.YAML);
        String json = serializer.serialize(openAPI, Format.JSON);

        assertThat(yaml).contains("openapi: 3.1.0");
        assertThat(json).contains("\"openapi\"");
    }

    @Test
    void shouldWriteToYamlFile(@TempDir Path tempDir) throws IOException {
        OpenAPI openAPI = createSimpleOpenAPI();
        Path outputPath = tempDir.resolve("openapi.yaml");

        serializer.writeToFile(openAPI, outputPath, Format.YAML);

        assertThat(Files.exists(outputPath)).isTrue();
        String content = Files.readString(outputPath);
        assertThat(content).contains("openapi: 3.1.0");
        assertThat(logMessages).anyMatch(msg -> msg.contains("Wrote OpenAPI"));
    }

    @Test
    void shouldWriteToJsonFile(@TempDir Path tempDir) throws IOException {
        OpenAPI openAPI = createSimpleOpenAPI();
        Path outputPath = tempDir.resolve("openapi.json");

        serializer.writeToFile(openAPI, outputPath, Format.JSON);

        assertThat(Files.exists(outputPath)).isTrue();
        String content = Files.readString(outputPath);
        assertThat(content).contains("\"openapi\"");
    }

    @Test
    void shouldCreateParentDirectories(@TempDir Path tempDir) throws IOException {
        OpenAPI openAPI = createSimpleOpenAPI();
        Path outputPath = tempDir.resolve("nested/dir/openapi.yaml");

        serializer.writeToFile(openAPI, outputPath);

        assertThat(Files.exists(outputPath)).isTrue();
        assertThat(logMessages).anyMatch(msg -> msg.contains("Created directory"));
    }

    @Test
    void shouldInferFormatFromExtension(@TempDir Path tempDir) throws IOException {
        OpenAPI openAPI = createSimpleOpenAPI();

        Path yamlPath = tempDir.resolve("test.yaml");
        Path jsonPath = tempDir.resolve("test.json");

        serializer.writeToFile(openAPI, yamlPath);
        serializer.writeToFile(openAPI, jsonPath);

        String yamlContent = Files.readString(yamlPath);
        String jsonContent = Files.readString(jsonPath);

        assertThat(yamlContent).contains("openapi: 3.1.0");
        assertThat(jsonContent).contains("\"openapi\"");
    }

    @Test
    void shouldInferYamlForUnknownExtension() {
        assertThat(OpenAPISerializer.inferFormat(Path.of("test.yml"))).isEqualTo(Format.YAML);
        assertThat(OpenAPISerializer.inferFormat(Path.of("test.txt"))).isEqualTo(Format.YAML);
        assertThat(OpenAPISerializer.inferFormat(Path.of("test"))).isEqualTo(Format.YAML);
    }

    @Test
    void shouldInferJsonForJsonExtension() {
        assertThat(OpenAPISerializer.inferFormat(Path.of("test.json"))).isEqualTo(Format.JSON);
        assertThat(OpenAPISerializer.inferFormat(Path.of("test.JSON"))).isEqualTo(Format.JSON);
    }

    @Test
    void shouldRejectNullOpenAPI() {
        assertThatThrownBy(() -> serializer.toYaml(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> serializer.toJson(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullPath() {
        OpenAPI openAPI = createSimpleOpenAPI();
        assertThatThrownBy(() -> serializer.writeToFile(openAPI, null, Format.YAML))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullFormat() {
        OpenAPI openAPI = createSimpleOpenAPI();
        assertThatThrownBy(() -> serializer.serialize(openAPI, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldGetFormatExtensions() {
        assertThat(Format.YAML.getExtension()).isEqualTo(".yaml");
        assertThat(Format.JSON.getExtension()).isEqualTo(".json");
    }

    @Test
    void shouldCreateWithoutLogger() {
        OpenAPISerializer noLogSerializer = new OpenAPISerializer();
        String yaml = noLogSerializer.toYaml(createSimpleOpenAPI());
        assertThat(yaml).contains("openapi: 3.1.0");
    }
}
