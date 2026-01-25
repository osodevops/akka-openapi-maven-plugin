package com.github.osodevops.akka.openapi.core;

import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Serializes OpenAPI specifications to YAML or JSON format.
 *
 * <p>Supports both OpenAPI 3.0 and 3.1 formats, using the appropriate
 * serializers from Swagger Core library.</p>
 */
public class OpenAPISerializer {

    /**
     * Output format for the OpenAPI specification.
     */
    public enum Format {
        YAML(".yaml"),
        JSON(".json");

        private final String extension;

        Format(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }

    private final Consumer<String> logger;

    /**
     * Creates a new OpenAPISerializer with no logging.
     */
    public OpenAPISerializer() {
        this(msg -> {});
    }

    /**
     * Creates a new OpenAPISerializer with custom logging.
     *
     * @param logger consumer for log messages
     */
    public OpenAPISerializer(Consumer<String> logger) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    /**
     * Serializes an OpenAPI specification to YAML format.
     *
     * @param openAPI the specification to serialize
     * @return the YAML string representation
     * @throws SerializationException if serialization fails
     */
    public String toYaml(OpenAPI openAPI) {
        Objects.requireNonNull(openAPI, "openAPI must not be null");
        try {
            return Yaml31.pretty(openAPI);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize OpenAPI to YAML", e);
        }
    }

    /**
     * Serializes an OpenAPI specification to JSON format.
     *
     * @param openAPI the specification to serialize
     * @return the JSON string representation
     * @throws SerializationException if serialization fails
     */
    public String toJson(OpenAPI openAPI) {
        Objects.requireNonNull(openAPI, "openAPI must not be null");
        try {
            return Json31.pretty(openAPI);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize OpenAPI to JSON", e);
        }
    }

    /**
     * Serializes an OpenAPI specification to the specified format.
     *
     * @param openAPI the specification to serialize
     * @param format the output format
     * @return the serialized string
     * @throws SerializationException if serialization fails
     */
    public String serialize(OpenAPI openAPI, Format format) {
        Objects.requireNonNull(format, "format must not be null");
        return switch (format) {
            case YAML -> toYaml(openAPI);
            case JSON -> toJson(openAPI);
        };
    }

    /**
     * Writes an OpenAPI specification to a file.
     *
     * <p>The parent directory will be created if it doesn't exist.</p>
     *
     * @param openAPI the specification to write
     * @param path the output file path
     * @param format the output format
     * @throws SerializationException if serialization or writing fails
     */
    public void writeToFile(OpenAPI openAPI, Path path, Format format) {
        Objects.requireNonNull(openAPI, "openAPI must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(format, "format must not be null");

        try {
            // Create parent directories if they don't exist
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                logger.accept("Created directory: " + parent);
            }

            // Serialize and write
            String content = serialize(openAPI, format);
            Files.writeString(path, content, StandardCharsets.UTF_8);
            logger.accept("Wrote OpenAPI specification to: " + path);

        } catch (IOException e) {
            throw new SerializationException("Failed to write OpenAPI to file: " + path, e);
        }
    }

    /**
     * Writes an OpenAPI specification to a file, inferring format from extension.
     *
     * <p>If the file extension is ".json", JSON format is used. Otherwise, YAML is used.</p>
     *
     * @param openAPI the specification to write
     * @param path the output file path
     * @throws SerializationException if serialization or writing fails
     */
    public void writeToFile(OpenAPI openAPI, Path path) {
        Format format = inferFormat(path);
        writeToFile(openAPI, path, format);
    }

    /**
     * Infers the output format from a file path's extension.
     *
     * @param path the file path
     * @return the inferred format (YAML if not .json)
     */
    public static Format inferFormat(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) {
            return Format.JSON;
        }
        return Format.YAML;
    }

    /**
     * Exception thrown when serialization fails.
     */
    public static class SerializationException extends RuntimeException {
        public SerializationException(String message) {
            super(message);
        }

        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
