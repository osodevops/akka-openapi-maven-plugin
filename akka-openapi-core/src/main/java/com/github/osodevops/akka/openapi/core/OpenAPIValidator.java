package com.github.osodevops.akka.openapi.core;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates OpenAPI specifications for correctness and completeness.
 *
 * <p>Performs the following validations:</p>
 * <ul>
 *   <li>Schema validation using swagger-parser</li>
 *   <li>All $ref references resolve correctly</li>
 *   <li>No duplicate operationIds</li>
 *   <li>Required fields are present</li>
 * </ul>
 */
public class OpenAPIValidator {

    private static final Pattern REF_PATTERN = Pattern.compile("\\$ref.*?#/components/(\\w+)/([\\w-]+)");

    private final Consumer<String> logger;
    private final OpenAPISerializer serializer;

    /**
     * Creates a new OpenAPIValidator with no logging.
     */
    public OpenAPIValidator() {
        this(msg -> {});
    }

    /**
     * Creates a new OpenAPIValidator with custom logging.
     *
     * @param logger consumer for log messages
     */
    public OpenAPIValidator(Consumer<String> logger) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
        this.serializer = new OpenAPISerializer(logger);
    }

    /**
     * Validates an OpenAPI specification.
     *
     * @param openAPI the specification to validate
     * @return the validation result
     */
    public ValidationResult validate(OpenAPI openAPI) {
        Objects.requireNonNull(openAPI, "openAPI must not be null");

        List<ValidationError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Basic structure validation
        validateBasicStructure(openAPI, errors);

        // Validate using swagger-parser
        validateWithParser(openAPI, errors, warnings);

        // Check for duplicate operationIds
        validateOperationIds(openAPI, errors);

        // Validate $ref references
        validateReferences(openAPI, errors);

        return new ValidationResult(errors, warnings);
    }

    private void validateBasicStructure(OpenAPI openAPI, List<ValidationError> errors) {
        if (openAPI.getInfo() == null) {
            errors.add(new ValidationError("info", "Info section is required"));
        } else {
            if (openAPI.getInfo().getTitle() == null || openAPI.getInfo().getTitle().isBlank()) {
                errors.add(new ValidationError("info.title", "API title is required"));
            }
            if (openAPI.getInfo().getVersion() == null || openAPI.getInfo().getVersion().isBlank()) {
                errors.add(new ValidationError("info.version", "API version is required"));
            }
        }

        if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            // This is a warning, not an error - empty API is valid
            logger.accept("Warning: No paths defined in the specification");
        }
    }

    private void validateWithParser(OpenAPI openAPI, List<ValidationError> errors, List<String> warnings) {
        try {
            // Serialize to YAML and re-parse for validation
            String yaml = serializer.toYaml(openAPI);

            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setResolveFully(true);

            SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);

            if (result.getMessages() != null) {
                for (String message : result.getMessages()) {
                    if (message.toLowerCase().contains("error")) {
                        errors.add(new ValidationError("parser", message));
                    } else {
                        warnings.add(message);
                    }
                }
            }

            // Check if parsing failed completely
            if (result.getOpenAPI() == null && !errors.isEmpty()) {
                errors.add(new ValidationError("parser", "Failed to parse specification"));
            }

        } catch (Exception e) {
            errors.add(new ValidationError("parser", "Parser validation failed: " + e.getMessage()));
        }
    }

    private void validateOperationIds(OpenAPI openAPI, List<ValidationError> errors) {
        if (openAPI.getPaths() == null) {
            return;
        }

        Set<String> seenOperationIds = new HashSet<>();

        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();

            checkOperationId(path, "GET", pathItem.getGet(), seenOperationIds, errors);
            checkOperationId(path, "POST", pathItem.getPost(), seenOperationIds, errors);
            checkOperationId(path, "PUT", pathItem.getPut(), seenOperationIds, errors);
            checkOperationId(path, "DELETE", pathItem.getDelete(), seenOperationIds, errors);
            checkOperationId(path, "PATCH", pathItem.getPatch(), seenOperationIds, errors);
            checkOperationId(path, "HEAD", pathItem.getHead(), seenOperationIds, errors);
            checkOperationId(path, "OPTIONS", pathItem.getOptions(), seenOperationIds, errors);
            checkOperationId(path, "TRACE", pathItem.getTrace(), seenOperationIds, errors);
        }
    }

    private void checkOperationId(String path, String method, Operation operation,
                                  Set<String> seenOperationIds, List<ValidationError> errors) {
        if (operation == null) {
            return;
        }

        String operationId = operation.getOperationId();
        if (operationId == null || operationId.isBlank()) {
            errors.add(new ValidationError(
                path + " " + method,
                "operationId is required for all operations"
            ));
            return;
        }

        if (seenOperationIds.contains(operationId)) {
            errors.add(new ValidationError(
                path + " " + method,
                "Duplicate operationId: " + operationId
            ));
        } else {
            seenOperationIds.add(operationId);
        }
    }

    private void validateReferences(OpenAPI openAPI, List<ValidationError> errors) {
        if (openAPI.getComponents() == null) {
            return;
        }

        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        if (schemas == null) {
            schemas = Collections.emptyMap();
        }

        // Collect all schema $refs and verify they exist
        Set<String> referencedSchemas = collectSchemaReferences(openAPI);
        for (String ref : referencedSchemas) {
            if (!schemas.containsKey(ref)) {
                errors.add(new ValidationError(
                    "$ref",
                    "Unresolved schema reference: #/components/schemas/" + ref
                ));
            }
        }
    }

    private Set<String> collectSchemaReferences(OpenAPI openAPI) {
        Set<String> refs = new HashSet<>();

        // Serialize to YAML and scan for $refs
        try {
            String yaml = serializer.toYaml(openAPI);
            Matcher matcher = REF_PATTERN.matcher(yaml);
            while (matcher.find()) {
                if ("schemas".equals(matcher.group(1))) {
                    refs.add(matcher.group(2));
                }
            }
        } catch (Exception e) {
            logger.accept("Warning: Could not scan for references: " + e.getMessage());
        }

        return refs;
    }

    /**
     * Represents the result of validation.
     */
    public static class ValidationResult {
        private final List<ValidationError> errors;
        private final List<String> warnings;

        public ValidationResult(List<ValidationError> errors, List<String> warnings) {
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<ValidationError> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String getErrorSummary() {
            if (errors.isEmpty()) {
                return "No errors";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(errors.size()).append(" validation error(s):\n");
            for (ValidationError error : errors) {
                sb.append("  - [").append(error.getLocation()).append("] ")
                  .append(error.getMessage()).append("\n");
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "ValidationResult{valid=" + isValid() +
                   ", errors=" + errors.size() +
                   ", warnings=" + warnings.size() + "}";
        }
    }

    /**
     * Represents a single validation error.
     */
    public static class ValidationError {
        private final String location;
        private final String message;

        public ValidationError(String location, String message) {
            this.location = location;
            this.message = message;
        }

        public String getLocation() {
            return location;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "[" + location + "] " + message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidationError that = (ValidationError) o;
            return Objects.equals(location, that.location) &&
                   Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, message);
        }
    }
}
