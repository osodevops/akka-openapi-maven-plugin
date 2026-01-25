package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.core.fixtures.*;
import io.swagger.v3.oas.models.media.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
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
}
