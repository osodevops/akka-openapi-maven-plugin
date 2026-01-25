package com.github.osodevops.akka.openapi.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import io.swagger.v3.oas.models.media.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Generates OpenAPI Schema objects from Java types.
 *
 * <p>This class uses the jsonschema-generator library to convert Java POJOs
 * to JSON Schema, then transforms them into OpenAPI 3.1 compatible Schema objects.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Primitive and wrapper type support</li>
 *   <li>Collection (List, Set) and array support</li>
 *   <li>Map type support</li>
 *   <li>Optional type unwrapping</li>
 *   <li>Enum support with @JsonValue</li>
 *   <li>Circular reference detection via $ref</li>
 *   <li>Jackson annotation support (@JsonProperty, @JsonIgnore)</li>
 *   <li>Validation annotation support (@NotNull, @Size, @Pattern)</li>
 * </ul>
 */
public class SchemaGenerator {

    private final com.github.victools.jsonschema.generator.SchemaGenerator jsonSchemaGenerator;
    private final ObjectMapper objectMapper;
    private final Map<String, Schema<?>> generatedSchemas;
    private final Consumer<String> logger;
    private final Set<String> processingTypes;

    /**
     * Creates a new SchemaGenerator with default settings.
     */
    public SchemaGenerator() {
        this(msg -> {});
    }

    /**
     * Creates a new SchemaGenerator with a custom logger.
     *
     * @param logger consumer for log messages
     */
    public SchemaGenerator(Consumer<String> logger) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
        this.objectMapper = new ObjectMapper();
        this.generatedSchemas = new ConcurrentHashMap<>();
        this.processingTypes = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.jsonSchemaGenerator = createJsonSchemaGenerator();
    }

    private com.github.victools.jsonschema.generator.SchemaGenerator createJsonSchemaGenerator() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
            SchemaVersion.DRAFT_2020_12,
            OptionPreset.PLAIN_JSON
        );

        // Add Jackson module for @JsonProperty, @JsonIgnore, etc.
        JacksonModule jacksonModule = new JacksonModule(
            JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
            JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE
        );
        configBuilder.with(jacksonModule);

        // Add Jakarta Validation module for @NotNull, @Size, @Pattern, etc.
        JakartaValidationModule validationModule = new JakartaValidationModule(
            JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
            JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS
        );
        configBuilder.with(validationModule);

        // Configure options
        configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
        configBuilder.with(Option.SCHEMA_VERSION_INDICATOR);

        // Handle date/time types
        configBuilder.forTypesInGeneral()
            .withStringFormatResolver(target -> {
                Class<?> erasedType = target.getType().getErasedType();
                if (LocalDate.class.isAssignableFrom(erasedType)) {
                    return "date";
                } else if (LocalDateTime.class.isAssignableFrom(erasedType) ||
                           ZonedDateTime.class.isAssignableFrom(erasedType) ||
                           OffsetDateTime.class.isAssignableFrom(erasedType) ||
                           Instant.class.isAssignableFrom(erasedType)) {
                    return "date-time";
                } else if (LocalTime.class.isAssignableFrom(erasedType) ||
                           OffsetTime.class.isAssignableFrom(erasedType)) {
                    return "time";
                } else if (Duration.class.isAssignableFrom(erasedType) ||
                           Period.class.isAssignableFrom(erasedType)) {
                    return "duration";
                } else if (UUID.class.isAssignableFrom(erasedType)) {
                    return "uuid";
                }
                return null;
            });

        return new com.github.victools.jsonschema.generator.SchemaGenerator(configBuilder.build());
    }

    /**
     * Generates an OpenAPI Schema for the given Java type.
     *
     * @param javaType the Java type to generate a schema for
     * @return the generated OpenAPI Schema
     * @throws SchemaGenerationException if schema generation fails
     */
    public Schema<?> generateSchema(Type javaType) {
        Objects.requireNonNull(javaType, "javaType must not be null");

        Class<?> rawClass = getRawClass(javaType);
        if (rawClass == null) {
            throw new SchemaGenerationException("Cannot determine raw class for type: " + javaType);
        }

        // Handle void types explicitly
        if (rawClass == Void.class || rawClass == void.class) {
            return null;
        }

        // Check for simple types first
        Schema<?> simpleSchema = trySimpleTypeSchema(rawClass);
        if (simpleSchema != null) {
            return simpleSchema;
        }

        // For complex types, check if already generated
        String typeName = getTypeName(rawClass);
        if (generatedSchemas.containsKey(typeName)) {
            return createReference(typeName);
        }

        // Check for circular reference
        if (processingTypes.contains(typeName)) {
            logger.accept("Circular reference detected for: " + typeName);
            return createReference(typeName);
        }

        try {
            processingTypes.add(typeName);
            return generateComplexSchema(javaType, rawClass, typeName);
        } finally {
            processingTypes.remove(typeName);
        }
    }

    /**
     * Generates an OpenAPI Schema for the given Java class.
     *
     * @param javaClass the Java class to generate a schema for
     * @return the generated OpenAPI Schema
     * @throws SchemaGenerationException if schema generation fails
     */
    public Schema<?> generateSchema(Class<?> javaClass) {
        return generateSchema((Type) javaClass);
    }

    private Schema<?> generateComplexSchema(Type javaType, Class<?> rawClass, String typeName) {
        try {
            logger.accept("Generating schema for: " + typeName);

            // Generate JSON Schema using victools
            ObjectNode jsonSchema = jsonSchemaGenerator.generateSchema(javaType);

            // Convert to OpenAPI Schema
            Schema<?> openApiSchema = convertJsonSchemaToOpenApi(jsonSchema, typeName);

            // Store in generated schemas if it's a named type
            if (openApiSchema != null && !(openApiSchema instanceof ArraySchema)) {
                generatedSchemas.put(typeName, openApiSchema);
            }

            return openApiSchema;

        } catch (Exception e) {
            throw new SchemaGenerationException("Failed to generate schema for: " + typeName, e);
        }
    }

    private Schema<?> convertJsonSchemaToOpenApi(ObjectNode jsonSchema, String rootTypeName) {
        // Extract definitions first (they may be under $defs in draft 2020-12)
        JsonNode defs = jsonSchema.get("$defs");
        if (defs == null) {
            defs = jsonSchema.get("definitions");
        }

        if (defs != null && defs.isObject()) {
            defs.fields().forEachRemaining(entry -> {
                String defName = sanitizeSchemaName(entry.getKey());
                if (!generatedSchemas.containsKey(defName)) {
                    Schema<?> defSchema = convertJsonNodeToSchema(entry.getValue(), defName);
                    if (defSchema != null) {
                        generatedSchemas.put(defName, defSchema);
                    }
                }
            });
        }

        // Convert the main schema
        return convertJsonNodeToSchema(jsonSchema, rootTypeName);
    }

    @SuppressWarnings("unchecked")
    private Schema<?> convertJsonNodeToSchema(JsonNode node, String currentTypeName) {
        if (node == null || node.isNull()) {
            return new ObjectSchema();
        }

        // Handle $ref
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            // Extract the type name from the reference
            String refName = extractRefName(ref, currentTypeName);
            if (refName != null && !refName.isEmpty()) {
                return createReference(refName);
            }
            // If we can't extract a proper ref name, return an object schema
            return new ObjectSchema();
        }

        String type = node.has("type") ? node.get("type").asText() : null;

        // Handle arrays with multiple types (JSON Schema 2020-12 style)
        if (node.has("type") && node.get("type").isArray()) {
            ArrayNode typeArray = (ArrayNode) node.get("type");
            // Find the non-null type
            for (JsonNode typeNode : typeArray) {
                String t = typeNode.asText();
                if (!"null".equals(t)) {
                    type = t;
                    break;
                }
            }
        }

        Schema<?> schema;

        if ("array".equals(type)) {
            ArraySchema arraySchema = new ArraySchema();
            if (node.has("items")) {
                arraySchema.setItems(convertJsonNodeToSchema(node.get("items"), currentTypeName));
            }
            schema = arraySchema;
        } else if ("object".equals(type) || (type == null && node.has("properties"))) {
            ObjectSchema objectSchema = new ObjectSchema();

            if (node.has("properties")) {
                JsonNode properties = node.get("properties");
                final String typeName = currentTypeName;
                properties.fields().forEachRemaining(entry -> {
                    Schema<?> propSchema = convertJsonNodeToSchema(entry.getValue(), typeName);
                    objectSchema.addProperty(entry.getKey(), propSchema);
                });
            }

            if (node.has("required") && node.get("required").isArray()) {
                List<String> required = new ArrayList<>();
                node.get("required").forEach(n -> required.add(n.asText()));
                objectSchema.setRequired(required);
            }

            if (node.has("additionalProperties")) {
                JsonNode addProps = node.get("additionalProperties");
                if (addProps.isBoolean()) {
                    // For Map types, allow additional properties
                    if (addProps.asBoolean()) {
                        objectSchema.setAdditionalProperties(new ObjectSchema());
                    }
                } else {
                    objectSchema.setAdditionalProperties(convertJsonNodeToSchema(addProps, currentTypeName));
                }
            }

            schema = objectSchema;
        } else if ("string".equals(type)) {
            StringSchema stringSchema = new StringSchema();
            if (node.has("format")) {
                stringSchema.setFormat(node.get("format").asText());
            }
            if (node.has("pattern")) {
                stringSchema.setPattern(node.get("pattern").asText());
            }
            if (node.has("minLength")) {
                stringSchema.setMinLength(node.get("minLength").asInt());
            }
            if (node.has("maxLength")) {
                stringSchema.setMaxLength(node.get("maxLength").asInt());
            }
            if (node.has("enum")) {
                List<String> enumValues = new ArrayList<>();
                node.get("enum").forEach(n -> enumValues.add(n.asText()));
                stringSchema.setEnum(enumValues);
            }
            schema = stringSchema;
        } else if ("integer".equals(type)) {
            IntegerSchema integerSchema = new IntegerSchema();
            if (node.has("format")) {
                integerSchema.setFormat(node.get("format").asText());
            }
            if (node.has("minimum")) {
                integerSchema.setMinimum(BigDecimal.valueOf(node.get("minimum").asLong()));
            }
            if (node.has("maximum")) {
                integerSchema.setMaximum(BigDecimal.valueOf(node.get("maximum").asLong()));
            }
            schema = integerSchema;
        } else if ("number".equals(type)) {
            NumberSchema numberSchema = new NumberSchema();
            if (node.has("format")) {
                numberSchema.setFormat(node.get("format").asText());
            }
            if (node.has("minimum")) {
                numberSchema.setMinimum(BigDecimal.valueOf(node.get("minimum").asDouble()));
            }
            if (node.has("maximum")) {
                numberSchema.setMaximum(BigDecimal.valueOf(node.get("maximum").asDouble()));
            }
            schema = numberSchema;
        } else if ("boolean".equals(type)) {
            schema = new BooleanSchema();
        } else {
            // Default to object schema
            schema = new ObjectSchema();
        }

        // Set common properties
        if (node.has("description")) {
            schema.setDescription(node.get("description").asText());
        }
        if (node.has("title")) {
            schema.setTitle(node.get("title").asText());
        }
        if (node.has("default")) {
            schema.setDefault(convertJsonNodeToValue(node.get("default")));
        }
        if (node.has("example")) {
            schema.setExample(convertJsonNodeToValue(node.get("example")));
        }
        if (node.has("nullable") && node.get("nullable").asBoolean()) {
            schema.setNullable(true);
        }

        return schema;
    }

    private Object convertJsonNodeToValue(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isLong()) {
            return node.asLong();
        } else if (node.isDouble()) {
            return node.asDouble();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(n -> list.add(convertJsonNodeToValue(n)));
            return list;
        } else if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry ->
                map.put(entry.getKey(), convertJsonNodeToValue(entry.getValue())));
            return map;
        }
        return node.toString();
    }

    private String extractRefName(String ref, String currentTypeName) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }

        String refName = null;

        // Handle JSON Schema 2020-12 $defs
        if (ref.startsWith("#/$defs/")) {
            refName = ref.substring("#/$defs/".length());
        }
        // Handle older JSON Schema definitions
        else if (ref.startsWith("#/definitions/")) {
            refName = ref.substring("#/definitions/".length());
        }
        // Handle existing OpenAPI component refs
        else if (ref.startsWith("#/components/schemas/")) {
            refName = ref.substring("#/components/schemas/".length());
        }
        // If it's just a pointer to root (#), use the current type name (circular reference)
        else if ("#".equals(ref)) {
            logger.accept("Circular reference detected for: " + currentTypeName);
            return sanitizeSchemaName(currentTypeName);
        }
        // Otherwise, try to extract the last segment
        else {
            int lastSlash = ref.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < ref.length() - 1) {
                refName = ref.substring(lastSlash + 1);
            }
        }

        return refName != null ? sanitizeSchemaName(refName) : null;
    }

    /**
     * Sanitizes a schema name to ensure it's valid for OpenAPI.
     * Replaces invalid characters with underscores.
     *
     * @param name the raw schema name
     * @return the sanitized schema name
     */
    private String sanitizeSchemaName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        // Replace characters that are not valid in OpenAPI schema names
        // Valid characters: letters, digits, underscores, hyphens
        // Common problematic characters: parentheses, commas, spaces, angle brackets
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private Schema<?> trySimpleTypeSchema(Class<?> type) {
        // Primitives and wrappers
        if (type == String.class || type == CharSequence.class) {
            return new StringSchema();
        } else if (type == int.class || type == Integer.class) {
            return new IntegerSchema().format("int32");
        } else if (type == long.class || type == Long.class) {
            return new IntegerSchema().format("int64");
        } else if (type == short.class || type == Short.class) {
            return new IntegerSchema().format("int32");
        } else if (type == byte.class || type == Byte.class) {
            return new IntegerSchema().format("int32");
        } else if (type == float.class || type == Float.class) {
            return new NumberSchema().format("float");
        } else if (type == double.class || type == Double.class) {
            return new NumberSchema().format("double");
        } else if (type == boolean.class || type == Boolean.class) {
            return new BooleanSchema();
        } else if (type == char.class || type == Character.class) {
            return new StringSchema().minLength(1).maxLength(1);
        } else if (type == BigDecimal.class) {
            return new NumberSchema();
        } else if (type == BigInteger.class) {
            return new IntegerSchema();
        } else if (type == UUID.class) {
            return new UUIDSchema();
        } else if (type == LocalDate.class) {
            return new DateSchema();
        } else if (type == LocalDateTime.class || type == ZonedDateTime.class ||
                   type == OffsetDateTime.class || type == Instant.class) {
            return new DateTimeSchema();
        } else if (type == LocalTime.class || type == OffsetTime.class) {
            return new StringSchema().format("time");
        } else if (type == Duration.class || type == Period.class) {
            return new StringSchema().format("duration");
        } else if (type == byte[].class) {
            return new BinarySchema();
        } else if (type == Object.class) {
            return new ObjectSchema();
        } else if (type == Void.class || type == void.class) {
            return null;
        }

        return null;
    }

    private Schema<?> createReference(String typeName) {
        Schema<?> schema = new Schema<>();
        schema.set$ref("#/components/schemas/" + typeName);
        return schema;
    }

    private Class<?> getRawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof java.lang.reflect.ParameterizedType) {
            Type rawType = ((java.lang.reflect.ParameterizedType) type).getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
        }
        return null;
    }

    private String getTypeName(Class<?> type) {
        return type.getSimpleName();
    }

    /**
     * Returns all schemas generated so far.
     *
     * <p>These schemas should be added to the OpenAPI components/schemas section.</p>
     *
     * @return map of schema names to schemas
     */
    public Map<String, Schema<?>> getGeneratedSchemas() {
        return Collections.unmodifiableMap(generatedSchemas);
    }

    /**
     * Clears all generated schemas.
     */
    public void clearSchemas() {
        generatedSchemas.clear();
    }

    /**
     * Checks if a schema has already been generated for the given type.
     *
     * @param typeName the type name to check
     * @return true if a schema exists for the type
     */
    public boolean hasSchema(String typeName) {
        return generatedSchemas.containsKey(typeName);
    }

    /**
     * Exception thrown when schema generation fails.
     */
    public static class SchemaGenerationException extends RuntimeException {
        public SchemaGenerationException(String message) {
            super(message);
        }

        public SchemaGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
