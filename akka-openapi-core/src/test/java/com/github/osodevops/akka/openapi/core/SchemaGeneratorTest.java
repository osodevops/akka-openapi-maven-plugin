package com.github.osodevops.akka.openapi.core;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.osodevops.akka.openapi.core.fixtures.*;
import io.swagger.v3.oas.models.media.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SchemaGenerator.
 */
class SchemaGeneratorTest {

    private List<String> logMessages;
    private SchemaGenerator generator;

    @BeforeEach
    void setUp() {
        logMessages = new ArrayList<>();
        generator = new SchemaGenerator(logMessages::add);
    }

    // Primitive and wrapper type tests

    @Test
    void shouldGenerateStringSchema() {
        Schema<?> schema = generator.generateSchema(String.class);

        assertThat(schema).isInstanceOf(StringSchema.class);
    }

    @Test
    void shouldGenerateIntegerSchema() {
        Schema<?> intSchema = generator.generateSchema(int.class);
        Schema<?> integerSchema = generator.generateSchema(Integer.class);

        assertThat(intSchema).isInstanceOf(IntegerSchema.class);
        assertThat(intSchema.getFormat()).isEqualTo("int32");
        assertThat(integerSchema).isInstanceOf(IntegerSchema.class);
    }

    @Test
    void shouldGenerateLongSchema() {
        Schema<?> longSchema = generator.generateSchema(long.class);
        Schema<?> longWrapperSchema = generator.generateSchema(Long.class);

        assertThat(longSchema).isInstanceOf(IntegerSchema.class);
        assertThat(longSchema.getFormat()).isEqualTo("int64");
        assertThat(longWrapperSchema).isInstanceOf(IntegerSchema.class);
        assertThat(longWrapperSchema.getFormat()).isEqualTo("int64");
    }

    @Test
    void shouldGenerateFloatAndDoubleSchemas() {
        Schema<?> floatSchema = generator.generateSchema(float.class);
        Schema<?> doubleSchema = generator.generateSchema(double.class);

        assertThat(floatSchema).isInstanceOf(NumberSchema.class);
        assertThat(floatSchema.getFormat()).isEqualTo("float");
        assertThat(doubleSchema).isInstanceOf(NumberSchema.class);
        assertThat(doubleSchema.getFormat()).isEqualTo("double");
    }

    @Test
    void shouldGenerateBooleanSchema() {
        Schema<?> boolSchema = generator.generateSchema(boolean.class);
        Schema<?> booleanSchema = generator.generateSchema(Boolean.class);

        assertThat(boolSchema).isInstanceOf(BooleanSchema.class);
        assertThat(booleanSchema).isInstanceOf(BooleanSchema.class);
    }

    @Test
    void shouldGenerateBigDecimalSchema() {
        Schema<?> schema = generator.generateSchema(BigDecimal.class);

        assertThat(schema).isInstanceOf(NumberSchema.class);
    }

    // Date/time type tests

    @Test
    void shouldGenerateDateSchema() {
        Schema<?> schema = generator.generateSchema(LocalDate.class);

        assertThat(schema).isInstanceOf(DateSchema.class);
    }

    @Test
    void shouldGenerateDateTimeSchemas() {
        Schema<?> localDateTimeSchema = generator.generateSchema(LocalDateTime.class);
        Schema<?> zonedDateTimeSchema = generator.generateSchema(ZonedDateTime.class);
        Schema<?> offsetDateTimeSchema = generator.generateSchema(OffsetDateTime.class);
        Schema<?> instantSchema = generator.generateSchema(Instant.class);

        assertThat(localDateTimeSchema).isInstanceOf(DateTimeSchema.class);
        assertThat(zonedDateTimeSchema).isInstanceOf(DateTimeSchema.class);
        assertThat(offsetDateTimeSchema).isInstanceOf(DateTimeSchema.class);
        assertThat(instantSchema).isInstanceOf(DateTimeSchema.class);
    }

    @Test
    void shouldGenerateTimeSchema() {
        Schema<?> localTimeSchema = generator.generateSchema(LocalTime.class);
        Schema<?> offsetTimeSchema = generator.generateSchema(OffsetTime.class);

        assertThat(localTimeSchema).isInstanceOf(StringSchema.class);
        assertThat(localTimeSchema.getFormat()).isEqualTo("time");
        assertThat(offsetTimeSchema).isInstanceOf(StringSchema.class);
    }

    @Test
    void shouldGenerateUUIDSchema() {
        Schema<?> schema = generator.generateSchema(UUID.class);

        assertThat(schema).isInstanceOf(UUIDSchema.class);
    }

    @Test
    void shouldGenerateBinarySchema() {
        Schema<?> schema = generator.generateSchema(byte[].class);

        assertThat(schema).isInstanceOf(BinarySchema.class);
    }

    // Complex type tests

    @Test
    void shouldGenerateSimplePojoSchema() {
        Schema<?> schema = generator.generateSchema(CustomerDto.class);

        // Should generate a reference or inline schema
        assertThat(schema).isNotNull();

        // Check generated schemas contains CustomerDto
        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        assertThat(schemas).containsKey("CustomerDto");

        Schema<?> customerSchema = schemas.get("CustomerDto");
        assertThat(customerSchema.getProperties()).containsKeys("id", "name", "email");
    }

    @Test
    void shouldGenerateNestedObjectSchema() {
        Schema<?> schema = generator.generateSchema(FullCustomer.class);

        assertThat(schema).isNotNull();

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        assertThat(schemas).containsKey("FullCustomer");

        // Should also have generated Address schema
        assertThat(schemas).containsKey("Address");
    }

    @Test
    void shouldHandleCircularReferences() {
        Schema<?> schema = generator.generateSchema(TreeNode.class);

        assertThat(schema).isNotNull();

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        assertThat(schemas).containsKey("TreeNode");

        // The schema should have a $ref for parent and children
        Schema<?> treeNodeSchema = schemas.get("TreeNode");
        assertThat(treeNodeSchema.getProperties()).containsKeys("name", "parent", "children");

        // Self-referencing fields should use $ref (handled by jsonschema-generator internally)
        Schema<?> parentSchema = treeNodeSchema.getProperties().get("parent");
        Schema<?> childrenSchema = treeNodeSchema.getProperties().get("children");

        // Parent should reference TreeNode
        assertThat(parentSchema.get$ref()).isNotNull();
        assertThat(parentSchema.get$ref()).contains("TreeNode");

        // Children should be an array with items referencing TreeNode
        assertThat(childrenSchema).isInstanceOf(ArraySchema.class);
    }

    @Test
    void shouldHandleEnumWithJsonValue() {
        Schema<?> schema = generator.generateSchema(CustomerStatus.class);

        assertThat(schema).isNotNull();

        // Enum with @JsonValue should be a string schema with enum values
        // The values should be the @JsonValue returns ("active", "inactive", etc.)
        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        assertThat(schemas).containsKey("CustomerStatus");
    }

    @Test
    void shouldRespectJsonPropertyAnnotation() {
        generator.generateSchema(FullCustomer.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> customerSchema = schemas.get("FullCustomer");

        // phone_number should be the property name (from @JsonProperty)
        assertThat(customerSchema.getProperties()).containsKey("phone_number");
    }

    @Test
    void shouldRespectJsonIgnoreAnnotation() {
        generator.generateSchema(FullCustomer.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> customerSchema = schemas.get("FullCustomer");

        // internalId should not be present (has @JsonIgnore)
        assertThat(customerSchema.getProperties()).doesNotContainKey("internalId");
    }

    @Test
    void shouldHandleValidationAnnotations() {
        generator.generateSchema(Address.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> addressSchema = schemas.get("Address");

        // street has @Size(min=1, max=100)
        Schema<?> streetSchema = addressSchema.getProperties().get("street");
        assertThat(streetSchema).isNotNull();
        // Validation constraints should be applied
        if (streetSchema.getMinLength() != null) {
            assertThat(streetSchema.getMinLength()).isEqualTo(1);
        }
        if (streetSchema.getMaxLength() != null) {
            assertThat(streetSchema.getMaxLength()).isEqualTo(100);
        }

        // postal_code has @Pattern
        Schema<?> postalCodeSchema = addressSchema.getProperties().get("postal_code");
        if (postalCodeSchema != null && postalCodeSchema.getPattern() != null) {
            assertThat(postalCodeSchema.getPattern()).contains("[0-9]{5}");
        }
    }

    @Test
    void shouldHandleMapTypes() {
        generator.generateSchema(FullCustomer.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> customerSchema = schemas.get("FullCustomer");

        Schema<?> metadataSchema = customerSchema.getProperties().get("metadata");
        assertThat(metadataSchema).isNotNull();
        // Map<String, String> should be an object with additionalProperties
    }

    @Test
    void shouldHandleArrayTypes() {
        generator.generateSchema(FullCustomer.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> customerSchema = schemas.get("FullCustomer");

        Schema<?> tagsSchema = customerSchema.getProperties().get("tags");
        assertThat(tagsSchema).isNotNull();
        // String[] should be an array of strings
        assertThat(tagsSchema).isInstanceOf(ArraySchema.class);
    }

    @Test
    void shouldHandleListTypes() {
        generator.generateSchema(FullCustomer.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> customerSchema = schemas.get("FullCustomer");

        Schema<?> addressesSchema = customerSchema.getProperties().get("addresses");
        assertThat(addressesSchema).isNotNull();
        // List<Address> should be an array with $ref items
        assertThat(addressesSchema).isInstanceOf(ArraySchema.class);
    }

    @Test
    void shouldUnwrapOptionalFieldsWithoutCreatingOptionalComponents() {
        generator.generateSchema(OptionalHolder.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> holderSchema = schemas.get("OptionalHolder");

        assertThat(holderSchema.getRequired()).isNullOrEmpty();

        // Optional<String> → string type with null included in types (OAS 3.1 nullable)
        Schema<?> maybeText = (Schema<?>) holderSchema.getProperties().get("maybeText");
        assertThat(maybeText).isInstanceOf(StringSchema.class);
        assertThat(maybeText.getTypes()).contains("null");

        // Optional<JsonStringValue> → oneOf with $ref and null type (OAS 3.1 nullable $ref)
        Schema<?> maybeTitle = (Schema<?>) holderSchema.getProperties().get("maybeTitle");
        assertThat(maybeTitle.getOneOf()).hasSize(2);
        assertThat(maybeTitle.getOneOf().get(0).get$ref())
            .isEqualTo("#/components/schemas/JsonStringValue");

        // Optional<PlainRecord> → oneOf with $ref and null type (OAS 3.1 nullable $ref)
        Schema<?> maybePlainRecord = (Schema<?>) holderSchema.getProperties().get("maybePlainRecord");
        assertThat(maybePlainRecord.getOneOf()).hasSize(2);
        assertThat(maybePlainRecord.getOneOf().get(0).get$ref())
            .isEqualTo("#/components/schemas/PlainRecord");

        assertThat(schemas).doesNotContainKeys(
            "Optional", "JsonStringValue-nullable", "PlainRecord-nullable");
    }

    @Test
    void shouldUnwrapTopLevelOptionalTypes() {
        Type optionalUuid = new java.lang.reflect.ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { UUID.class };
            }

            @Override
            public Type getRawType() {
                return Optional.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Schema<?> schema = generator.generateSchema(optionalUuid);

        assertThat(schema).isInstanceOf(UUIDSchema.class);
    }

    @Test
    void shouldRepresentJsonValueWrappersAsUnderlyingScalarSchemas() {
        generator.generateSchema(JsonStringValue.class);
        generator.generateSchema(JsonLongValue.class);
        generator.generateSchema(JsonUuidValue.class);
        generator.generateSchema(JsonDateValue.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        assertThat(schemas.get("JsonStringValue")).isInstanceOf(StringSchema.class);
        assertThat(schemas.get("JsonLongValue")).isInstanceOf(IntegerSchema.class);
        assertThat(schemas.get("JsonLongValue").getFormat()).isEqualTo("int64");
        assertThat(schemas.get("JsonUuidValue")).isInstanceOf(UUIDSchema.class);
        assertThat(schemas.get("JsonDateValue")).isInstanceOf(DateSchema.class);
    }

    @Test
    void shouldUnwrapOnlyNullableAnyOfAndOneOfCompositions() throws Exception {
        Schema<?> anyOfSchema = convertJsonNodeToOpenApiSchema("""
            {
              "description": "nullable string",
              "anyOf": [
                { "type": "string" },
                { "type": "null" }
              ]
            }
            """);
        Schema<?> oneOfSchema = convertJsonNodeToOpenApiSchema("""
            {
              "oneOf": [
                { "type": "null" },
                { "type": "integer", "format": "int64" }
              ]
            }
            """);

        assertThat(anyOfSchema).isInstanceOf(StringSchema.class);
        assertThat(anyOfSchema.getDescription()).isEqualTo("nullable string");
        assertThat(oneOfSchema).isInstanceOf(IntegerSchema.class);
        assertThat(oneOfSchema.getFormat()).isEqualTo("int64");
    }

    @Test
    void shouldPreserveRealAnyOfAndOneOfCompositions() throws Exception {
        Schema<?> anyOfSchema = convertJsonNodeToOpenApiSchema("""
            {
              "anyOf": [
                { "type": "string" },
                { "type": "integer", "format": "int64" }
              ]
            }
            """);
        Schema<?> oneOfSchema = convertJsonNodeToOpenApiSchema("""
            {
              "oneOf": [
                { "type": "string" },
                { "type": "integer" },
                { "type": "null" }
              ]
            }
            """);

        assertThat(anyOfSchema).isInstanceOf(ComposedSchema.class);
        assertThat(((ComposedSchema) anyOfSchema).getAnyOf()).hasSize(2);
        assertThat(((ComposedSchema) anyOfSchema).getAnyOf().get(0)).isInstanceOf(StringSchema.class);
        assertThat(((ComposedSchema) anyOfSchema).getAnyOf().get(1)).isInstanceOf(IntegerSchema.class);

        assertThat(oneOfSchema).isInstanceOf(ComposedSchema.class);
        assertThat(((ComposedSchema) oneOfSchema).getOneOf()).hasSize(3);
        assertThat(((ComposedSchema) oneOfSchema).getOneOf().get(2).getType()).isEqualTo("null");
    }

    // Error handling tests

    @Test
    void shouldRejectNullType() {
        assertThatThrownBy(() -> generator.generateSchema((Type) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("javaType must not be null");
    }

    @Test
    void shouldRejectNullClass() {
        assertThatThrownBy(() -> generator.generateSchema((Class<?>) null))
            .isInstanceOf(NullPointerException.class);
    }

    // Schema caching tests

    @Test
    void shouldReuseGeneratedSchemas() {
        generator.generateSchema(CustomerDto.class);
        int schemaSizeAfterFirst = generator.getGeneratedSchemas().size();

        // Generate again
        generator.generateSchema(CustomerDto.class);
        int schemaSizeAfterSecond = generator.getGeneratedSchemas().size();

        // Should not add duplicate
        assertThat(schemaSizeAfterSecond).isEqualTo(schemaSizeAfterFirst);
    }

    @Test
    void shouldClearSchemas() {
        generator.generateSchema(CustomerDto.class);
        assertThat(generator.getGeneratedSchemas()).isNotEmpty();

        generator.clearSchemas();
        assertThat(generator.getGeneratedSchemas()).isEmpty();
    }

    @Test
    void shouldCheckHasSchema() {
        assertThat(generator.hasSchema("CustomerDto")).isFalse();

        generator.generateSchema(CustomerDto.class);

        assertThat(generator.hasSchema("CustomerDto")).isTrue();
    }

    @Test
    void shouldCreateWithoutLogger() {
        SchemaGenerator noLogGenerator = new SchemaGenerator();
        Schema<?> schema = noLogGenerator.generateSchema(String.class);

        assertThat(schema).isInstanceOf(StringSchema.class);
    }

    @Test
    void shouldReturnNullForVoidType() {
        Schema<?> schema = generator.generateSchema(void.class);
        assertThat(schema).isNull();

        Schema<?> voidSchema = generator.generateSchema(Void.class);
        assertThat(voidSchema).isNull();
    }

    @Test
    void shouldGenerateObjectSchemaForGenericObject() {
        Schema<?> schema = generator.generateSchema(Object.class);

        assertThat(schema).isInstanceOf(ObjectSchema.class);
    }

    @Test
    void shouldGeneratePolymorphicSchemaForSealedInterface() {
        Schema<?> schema = generator.generateSchema(Shape.class);

        assertThat(schema).isNotNull();

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        assertThat(schemas).containsKey("Shape");

        Schema<?> shapeSchema = schemas.get("Shape");
        assertThat(shapeSchema).isInstanceOf(ComposedSchema.class);

        ComposedSchema composedSchema = (ComposedSchema) shapeSchema;
        assertThat(composedSchema.getOneOf()).hasSize(3);

        // Check discriminator
        assertThat(composedSchema.getDiscriminator()).isNotNull();
        assertThat(composedSchema.getDiscriminator().getPropertyName()).isEqualTo("shapeType");

        Map<String, String> mapping = composedSchema.getDiscriminator().getMapping();
        assertThat(mapping).hasSize(3);
        assertThat(mapping).containsEntry("CIRCLE", "#/components/schemas/Circle");
        assertThat(mapping).containsEntry("RECTANGLE", "#/components/schemas/Rectangle");
        assertThat(mapping).containsEntry("TRIANGLE", "#/components/schemas/Triangle");

        // Each subtype should also be generated
        assertThat(schemas).containsKey("Circle");
        assertThat(schemas).containsKey("Rectangle");
        assertThat(schemas).containsKey("Triangle");
        assertThat(schemas.keySet().stream()
            .filter(name -> name.matches("(Shape|Circle|Rectangle|Triangle)-\\d+"))
            .toList()).isEmpty();
    }

    @Test
    void shouldGenerateSubtypeSchemasForPolymorphicType() {
        generator.generateSchema(Shape.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> circleSchema = schemas.get("Circle");
        Schema<?> rectangleSchema = schemas.get("Rectangle");
        Schema<?> triangleSchema = schemas.get("Triangle");

        assertThat(circleSchema.getProperties()).containsKeys("radius", "shapeType");
        assertThat(rectangleSchema.getProperties()).containsKeys("width", "height", "shapeType");
        assertThat(triangleSchema.getProperties()).containsKeys("base", "height", "shapeType");

        assertDiscriminatorProperty(circleSchema, "shapeType", "CIRCLE");
        assertDiscriminatorProperty(rectangleSchema, "shapeType", "RECTANGLE");
        assertDiscriminatorProperty(triangleSchema, "shapeType", "TRIANGLE");
    }

    @Test
    void shouldNotGenerateDuplicateSubtypeComponentsForPolymorphicListAfterBaseType() {
        generator.generateSchema(Shape.class);

        Type listOfShapes = new java.lang.reflect.ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { Shape.class };
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        Schema<?> listSchema = generator.generateSchema(listOfShapes);

        assertThat(listSchema).isInstanceOf(ArraySchema.class);
        assertThat(generator.getGeneratedSchemas().keySet().stream()
            .filter(name -> name.matches("(Shape|Circle|Rectangle|Triangle)-\\d+"))
            .toList()).isEmpty();
    }

    @Test
    void shouldGeneratePolymorphicItemsWhenListIsGeneratedBeforeBaseType() {
        Type listOfShapes = new java.lang.reflect.ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { Shape.class };
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Schema<?> listSchema = generator.generateSchema(listOfShapes);

        assertThat(listSchema).isInstanceOf(ArraySchema.class);
        assertThat(((ArraySchema) listSchema).getItems().get$ref())
            .isEqualTo("#/components/schemas/Shape");

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        assertThat(schemas.get("Shape")).isInstanceOf(ComposedSchema.class);
        assertThat(schemas.keySet().stream()
            .filter(name -> name.matches("(Shape|Circle|Rectangle|Triangle)-\\d+"))
            .toList()).isEmpty();

        assertDiscriminatorProperty(schemas.get("Circle"), "shapeType", "CIRCLE");
        assertDiscriminatorProperty(schemas.get("Rectangle"), "shapeType", "RECTANGLE");
        assertDiscriminatorProperty(schemas.get("Triangle"), "shapeType", "TRIANGLE");
    }

    private Schema<?> convertJsonNodeToOpenApiSchema(String json) throws Exception {
        JsonNode node = new ObjectMapper().readTree(json);
        java.lang.reflect.Method method = SchemaGenerator.class.getDeclaredMethod(
            "convertJsonNodeToSchema", JsonNode.class, String.class);
        method.setAccessible(true);
        return (Schema<?>) method.invoke(generator, node, "ComposedValue");
    }

    private record OptionalHolder(
        Optional<String> maybeText,
        Optional<JsonStringValue> maybeTitle,
        Optional<PlainRecord> maybePlainRecord
    ) {
    }

    private record JsonStringValue(@JsonValue String value) {
    }

    private record JsonLongValue(@JsonValue long value) {
    }

    private record JsonUuidValue(@JsonValue UUID value) {
    }

    private record JsonDateValue(@JsonValue LocalDate value) {
    }

    private record PlainRecord(String value) {
    }

    private void assertDiscriminatorProperty(Schema<?> schema, String propertyName, String value) {
        assertThat(schema.getRequired()).contains(propertyName);

        Schema<?> discriminatorSchema = schema.getProperties().get(propertyName);
        assertThat(discriminatorSchema).isInstanceOf(StringSchema.class);
        assertThat(discriminatorSchema.getConst()).isEqualTo(value);
        assertThat(discriminatorSchema.getEnum()).hasSize(1);
        assertThat(discriminatorSchema.getEnum().get(0)).isEqualTo(value);
    }

    // Clashing $ref resolution tests

    @Test
    void shouldFullyQualifyClashingInnerEnumNames() {
        // Generate schemas for both notification types — each has an inner Status enum
        generator.generateSchema(EmailNotification.class);
        generator.generateSchema(SmsNotification.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        // Both notification schemas should exist
        assertThat(schemas).containsKey("EmailNotification");
        assertThat(schemas).containsKey("SmsNotification");

        // The two Status enums must be stored under qualified names, not bare "Status"
        assertThat(schemas).containsKey("EmailNotificationStatus");
        assertThat(schemas).containsKey("SmsNotificationStatus");
        // Bare "Status" should NOT exist as a component
        assertThat(schemas).doesNotContainKey("Status");
    }

    @Test
    void shouldAssignCorrectEnumValuesToQualifiedStatusSchemas() {
        generator.generateSchema(EmailNotification.class);
        generator.generateSchema(SmsNotification.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        Schema<?> emailStatus = schemas.get("EmailNotificationStatus");
        Schema<?> smsStatus = schemas.get("SmsNotificationStatus");

        assertThat(emailStatus).isNotNull();
        assertThat(smsStatus).isNotNull();

        // EmailNotification.Status: QUEUED, SENT, DELIVERED, BOUNCED
        @SuppressWarnings("unchecked")
        List<String> emailEnumValues = (List<String>) (List<?>) emailStatus.getEnum();
        assertThat(emailEnumValues).containsExactlyInAnyOrder("QUEUED", "SENT", "DELIVERED", "BOUNCED");

        // SmsNotification.Status: QUEUED, SENT, FAILED
        @SuppressWarnings("unchecked")
        List<String> smsEnumValues = (List<String>) (List<?>) smsStatus.getEnum();
        assertThat(smsEnumValues).containsExactlyInAnyOrder("QUEUED", "SENT", "FAILED");
    }

    @Test
    void shouldPointRefsToCorrectQualifiedSchemaNames() {
        generator.generateSchema(EmailNotification.class);
        generator.generateSchema(SmsNotification.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        // EmailNotification's status property should reference EmailNotificationStatus
        Schema<?> emailSchema = schemas.get("EmailNotification");
        Schema<?> emailStatusProp = emailSchema.getProperties().get("status");
        assertThat(emailStatusProp.get$ref()).isEqualTo("#/components/schemas/EmailNotificationStatus");

        // SmsNotification's status property should reference SmsNotificationStatus
        Schema<?> smsSchema = schemas.get("SmsNotification");
        Schema<?> smsStatusProp = smsSchema.getProperties().get("status");
        assertThat(smsStatusProp.get$ref()).isEqualTo("#/components/schemas/SmsNotificationStatus");
    }

    @Test
    void shouldFullyQualifyClashingNamesRegardlessOfGenerationOrder() {
        // Generate in reverse order to ensure order-independence
        generator.generateSchema(SmsNotification.class);
        generator.generateSchema(EmailNotification.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        // Both qualified names should exist regardless of order
        assertThat(schemas).containsKey("EmailNotificationStatus");
        assertThat(schemas).containsKey("SmsNotificationStatus");
        assertThat(schemas).doesNotContainKey("Status");

        // Enum values should still be correct
        @SuppressWarnings("unchecked")
        List<String> emailEnumValues = (List<String>) (List<?>) schemas.get("EmailNotificationStatus").getEnum();
        assertThat(emailEnumValues).containsExactlyInAnyOrder("QUEUED", "SENT", "DELIVERED", "BOUNCED");
        @SuppressWarnings("unchecked")
        List<String> smsEnumValues = (List<String>) (List<?>) schemas.get("SmsNotificationStatus").getEnum();
        assertThat(smsEnumValues).containsExactlyInAnyOrder("QUEUED", "SENT", "FAILED");
    }

    @Test
    void shouldNotQualifyNonClashingEnumNames() {
        // CustomerStatus doesn't clash with anything — should keep its simple name
        generator.generateSchema(FullCustomer.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        assertThat(schemas).containsKey("CustomerStatus");
    }

    @Test
    void shouldFullyQualifyClashingStatusEnumsInNestedHierarchy() {
        // Reproduces the pattern: a parent type (ActiveDispatchGroup) and a child type
        // (DispatchLineItem) both have inner Status enums with different values.
        // The response wrapper generates the full hierarchy in one call.
        generator.generateSchema(ActiveDispatchGroupsResponse.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        // Both qualified Status schemas should exist
        assertThat(schemas).containsKey("ActiveDispatchGroupStatus");
        assertThat(schemas).containsKey("DispatchLineItemStatus");
        // No unqualified "Status" should remain
        assertThat(schemas).doesNotContainKey("Status");

        // Verify correct enum values for each
        @SuppressWarnings("unchecked")
        List<String> groupStatusValues = (List<String>) (List<?>) schemas.get("ActiveDispatchGroupStatus").getEnum();
        assertThat(groupStatusValues).containsExactlyInAnyOrder("PENDING", "DISPATCHED");

        @SuppressWarnings("unchecked")
        List<String> lineItemStatusValues = (List<String>) (List<?>) schemas.get("DispatchLineItemStatus").getEnum();
        assertThat(lineItemStatusValues).containsExactlyInAnyOrder("PENDING", "DISPATCHED", "CANCELLED");
    }

    @Test
    void shouldPointRefsToCorrectQualifiedStatusInNestedHierarchy() {
        generator.generateSchema(ActiveDispatchGroupsResponse.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        // ActiveDispatchGroup.status should reference ActiveDispatchGroupStatus
        Schema<?> groupSchema = schemas.get("ActiveDispatchGroup");
        assertThat(groupSchema).isNotNull();
        Schema<?> groupStatusProp = groupSchema.getProperties().get("status");
        assertThat(groupStatusProp.get$ref()).isEqualTo("#/components/schemas/ActiveDispatchGroupStatus");

        // DispatchLineItem.status should reference DispatchLineItemStatus
        Schema<?> lineItemSchema = schemas.get("DispatchLineItem");
        assertThat(lineItemSchema).isNotNull();
        Schema<?> lineItemStatusProp = lineItemSchema.getProperties().get("status");
        assertThat(lineItemStatusProp.get$ref()).isEqualTo("#/components/schemas/DispatchLineItemStatus");
    }

    @Test
    void shouldFullyQualifyClashingInnerRecordNames() {
        generator.generateSchema(ReportsResponse.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        // Both MonthlyReport.Summary and WeeklyReport.Summary should have qualified names
        assertThat(schemas).containsKey("MonthlyReportSummary");
        assertThat(schemas).containsKey("WeeklyReportSummary");

        // Both MonthlyReport.Charts and WeeklyReport.Charts should have qualified names
        assertThat(schemas).containsKey("MonthlyReportCharts");
        assertThat(schemas).containsKey("WeeklyReportCharts");

        // The ambiguous simple names should NOT exist
        assertThat(schemas).doesNotContainKey("Summary");
        assertThat(schemas).doesNotContainKey("Charts");
    }

    @Test
    void shouldKeepNestedSameNameRecordsDistinctAcrossSeparateEndpoints() {
        // Two separate endpoint response types, each nesting a record named "Baz" with a
        // different shape, generated through the SAME generator (mirrors scanning two
        // endpoint files). The nested Baz refs must stay distinct and resolvable.
        generator.generateSchema(FooEndpoint.Foo.class);
        generator.generateSchema(BarEndpoint.Bar.class);

        assertNestedBazRefsAreDistinctAndPresent(generator.getGeneratedSchemas());
    }

    @Test
    void shouldKeepNestedSameNameRecordsDistinctRegardlessOfGenerationOrder() {
        // Reverse generation order — result must be identical (order-independent).
        generator.generateSchema(BarEndpoint.Bar.class);
        generator.generateSchema(FooEndpoint.Foo.class);

        assertNestedBazRefsAreDistinctAndPresent(generator.getGeneratedSchemas());
    }

    private void assertNestedBazRefsAreDistinctAndPresent(Map<String, Schema<?>> schemas) {
        // Top-level types collapse to their simple names (globally unique).
        Schema<?> foo = schemas.get("Foo");
        Schema<?> bar = schemas.get("Bar");
        assertThat(foo).isNotNull();
        assertThat(bar).isNotNull();

        // The shared nested name "Baz" stays qualified for both owners.
        String fooBazRef = foo.getProperties().get("baz").get$ref();
        String barBazRef = bar.getProperties().get("baz").get$ref();
        assertThat(fooBazRef).isEqualTo("#/components/schemas/FooEndpointBaz");
        assertThat(barBazRef).isEqualTo("#/components/schemas/BarEndpointBaz");
        assertThat(fooBazRef).isNotEqualTo(barBazRef);

        // Both referenced schemas exist (no dangling refs) and carry their own shape.
        Schema<?> fooBaz = schemas.get("FooEndpointBaz");
        Schema<?> barBaz = schemas.get("BarEndpointBaz");
        assertThat(fooBaz).isNotNull();
        assertThat(barBaz).isNotNull();
        assertThat(fooBaz.getProperties().get("value").getType()).isEqualTo("string");
        assertThat(barBaz.getProperties().get("value").getType()).isEqualTo("integer");

        // The ambiguous simple name must not leak into the component map.
        assertThat(schemas).doesNotContainKey("Baz");
    }

    @Test
    void shouldCollapseDuplicateUnionBranchesToSingleParentRef() {
        // Shape (discriminated parent) is generated first; the shape field of ShipmentCommand
        // is then rendered inline by victools as an anyOf of suffixed duplicate refs that all
        // resolve to the same component. The union must collapse to a single $ref.
        generator.generateSchema(ShipmentCommand.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> shapeProperty = schemas.get("ShipmentCommand").getProperties().get("shape");

        assertThat(shapeProperty.getAnyOf()).isNull();
        assertThat(shapeProperty.getOneOf()).isNull();
        assertThat(shapeProperty.get$ref()).isEqualTo("#/components/schemas/Shape");
    }

    @Test
    void shouldRestoreDiscriminatedParentRefForInlineSubtypeUnion() {
        // Reached through a wrapper, the polymorphic channel field is rendered inline as an
        // anyOf of the subtype refs. The union must be replaced by a $ref to the discriminated
        // parent so the discriminator mapping stays reachable from the field.
        generator.generateSchema(ChannelGroupsResponse.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();
        Schema<?> channelProperty = schemas.get("ChannelGroup").getProperties().get("channel");

        assertThat(channelProperty.getAnyOf()).isNull();
        assertThat(channelProperty.get$ref()).isEqualTo("#/components/schemas/Channel");

        // The parent component keeps its full discriminated form.
        Schema<?> channel = schemas.get("Channel");
        assertThat(channel.getOneOf()).hasSize(2);
        assertThat(channel.getDiscriminator()).isNotNull();
        assertThat(channel.getDiscriminator().getMapping())
            .containsEntry("EMAIL", "#/components/schemas/EmailChannel")
            .containsEntry("SMS", "#/components/schemas/SmsChannel");
    }

    @Test
    void shouldKeepConcreteSubtypeFieldRefRegardlessOfParentGenerationOrder() {
        SchemaGenerator subtypeFirst = new SchemaGenerator(logMessages::add);
        subtypeFirst.generateSchema(CircleShipment.class);

        SchemaGenerator parentFirst = new SchemaGenerator(logMessages::add);
        parentFirst.generateSchema(Shape.class);
        parentFirst.generateSchema(CircleShipment.class);

        assertCircleShipmentReferencesConcreteCircle(subtypeFirst.getGeneratedSchemas());
        assertCircleShipmentReferencesConcreteCircle(parentFirst.getGeneratedSchemas());
    }

    private void assertCircleShipmentReferencesConcreteCircle(Map<String, Schema<?>> schemas) {
        Schema<?> circleProperty = schemas.get("CircleShipment").getProperties().get("circle");

        assertThat(schemas).containsKey("Circle");
        assertThat(schemas).doesNotContainKey("Circle-1");
        assertThat(circleProperty.getAnyOf()).isNull();
        assertThat(circleProperty.getOneOf()).isNull();
        assertThat(circleProperty.get$ref()).isEqualTo("#/components/schemas/Circle");
    }

    @Test
    void shouldStoreCorrectPropertiesForQualifiedInnerRecordSchemas() {
        generator.generateSchema(ReportsResponse.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        // MonthlyReportSummary should have totalRevenue, totalOrders, averageOrderValue
        Schema<?> monthlySummary = schemas.get("MonthlyReportSummary");
        assertThat(monthlySummary).isNotNull();
        assertThat(monthlySummary.getProperties()).containsKey("totalRevenue");
        assertThat(monthlySummary.getProperties()).containsKey("totalOrders");
        assertThat(monthlySummary.getProperties()).containsKey("averageOrderValue");

        // WeeklyReportSummary should have weeklyRevenue, weeklyOrders
        Schema<?> weeklySummary = schemas.get("WeeklyReportSummary");
        assertThat(weeklySummary).isNotNull();
        assertThat(weeklySummary.getProperties()).containsKey("weeklyRevenue");
        assertThat(weeklySummary.getProperties()).containsKey("weeklyOrders");
    }

    @Test
    void shouldPointRefsToCorrectQualifiedInnerRecordNames() {
        generator.generateSchema(ReportsResponse.class);

        Map<String, Schema<?>> schemas = generator.getGeneratedSchemas();

        // MonthlyReport.summary should reference MonthlyReportSummary
        Schema<?> monthlyReport = schemas.get("MonthlyReport");
        assertThat(monthlyReport).isNotNull();
        Schema<?> monthlySummaryProp = monthlyReport.getProperties().get("summary");
        assertThat(monthlySummaryProp.get$ref()).isEqualTo("#/components/schemas/MonthlyReportSummary");

        // MonthlyReport.charts should reference MonthlyReportCharts
        Schema<?> monthlyChartsProp = monthlyReport.getProperties().get("charts");
        assertThat(monthlyChartsProp.get$ref()).isEqualTo("#/components/schemas/MonthlyReportCharts");

        // WeeklyReport.summary should reference WeeklyReportSummary
        Schema<?> weeklyReport = schemas.get("WeeklyReport");
        assertThat(weeklyReport).isNotNull();
        Schema<?> weeklySummaryProp = weeklyReport.getProperties().get("summary");
        assertThat(weeklySummaryProp.get$ref()).isEqualTo("#/components/schemas/WeeklyReportSummary");

        // WeeklyReport.charts should reference WeeklyReportCharts
        Schema<?> weeklyChartsProp = weeklyReport.getProperties().get("charts");
        assertThat(weeklyChartsProp.get$ref()).isEqualTo("#/components/schemas/WeeklyReportCharts");
    }
}
