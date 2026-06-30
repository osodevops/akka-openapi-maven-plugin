package com.github.osodevops.akka.openapi.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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

import java.lang.reflect.AnnotatedElement;
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
    private final Map<String, String> schemaNameAliases;
    private final Consumer<String> logger;
    private final Set<String> processingTypes;
    /** Registry mapping internal schema name → Class<?> for enrichRequiredFields post-processing on defs. */
    private final Map<String, Class<?>> classRegistry;
    /** Canonical internal schema name resolved for each class (enclosing-chain qualified). */
    private final Map<Class<?>, String> resolvedSchemaNames;
    /**
     * Cumulative internal-name → final-display-name replacements applied by the last
     * {@link #getGeneratedSchemas()} call. Lets callers normalise {@code $ref}s they built
     * from internal names (e.g. path-level response refs) to the collapsed component names.
     */
    private final Map<String, String> lastSchemaNameReplacements;

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
        this.schemaNameAliases = new ConcurrentHashMap<>();
        this.processingTypes = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.classRegistry = new ConcurrentHashMap<>();
        this.resolvedSchemaNames = new ConcurrentHashMap<>();
        this.lastSchemaNameReplacements = new ConcurrentHashMap<>();
        this.jsonSchemaGenerator = createJsonSchemaGenerator();
    }

    /**
     * Recursively registers a class and all types reachable from it into the classRegistry:
     * - Declared nested/inner classes
     * - Types of record components
     * - Types of declared fields
     * - Generic type arguments (e.g. List&lt;Foo&gt; → Foo)
     *
     * This ensures that any type victools may emit as a $def can be found later
     * for {@link #enrichRequiredFields} post-processing, regardless of the nesting depth.
     */
    private void registerClassHierarchy(Class<?> clazz) {
        registerClassHierarchy(clazz, new LinkedHashSet<>());
    }

    private void registerClassHierarchy(Class<?> clazz, Set<Class<?>> visited) {
        if (clazz == null || visited.contains(clazz)) return;
        if (clazz.isPrimitive() || clazz.isArray()
                || clazz.getPackageName().startsWith("java.")
                || clazz.getPackageName().startsWith("javax.")
                || clazz.getPackageName().startsWith("scala.")
                || clazz.getPackageName().startsWith("akka.")) {
            return;
        }
        visited.add(clazz);
        classRegistry.putIfAbsent(internalNameForClass(clazz), clazz);

        // For enums, register but don't recurse into fields/nested classes
        if (clazz.isEnum()) {
            return;
        }

        for (Class<?> nested : clazz.getDeclaredClasses()) {
            registerClassHierarchy(nested, visited);
        }

        JsonSubTypes jsonSubTypes = clazz.getAnnotation(JsonSubTypes.class);
        if (jsonSubTypes != null) {
            for (JsonSubTypes.Type subType : jsonSubTypes.value()) {
                registerClassHierarchy(subType.value(), visited);
            }
        }

        if (clazz.isSealed()) {
            for (Class<?> permitted : clazz.getPermittedSubclasses()) {
                registerClassHierarchy(permitted, visited);
            }
        }

        java.lang.reflect.RecordComponent[] components = clazz.getRecordComponents();
        if (components != null) {
            for (java.lang.reflect.RecordComponent component : components) {
                registerTypeAndArguments(component.getGenericType(), visited);
            }
        }

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                registerTypeAndArguments(field.getGenericType(), visited);
            }
        }

        for (Class<?> iface : clazz.getInterfaces()) {
            registerClassHierarchy(iface, visited);
        }
    }

    /**
     * Resolves a generic type to its raw class and any type arguments, registering each.
     */
    private void registerTypeAndArguments(java.lang.reflect.Type type, Set<Class<?>> visited) {
        if (type instanceof Class<?> raw) {
            registerClassHierarchy(raw, visited);
        } else if (type instanceof java.lang.reflect.ParameterizedType pt) {
            registerTypeAndArguments(pt.getRawType(), visited);
            for (java.lang.reflect.Type arg : pt.getActualTypeArguments()) {
                registerTypeAndArguments(arg, visited);
            }
        } else if (type instanceof java.lang.reflect.WildcardType wt) {
            for (java.lang.reflect.Type bound : wt.getUpperBounds()) {
                registerTypeAndArguments(bound, visited);
            }
        }
    }

    /**
     * Computes the canonical internal schema name for a class: the simple names of its
     * enclosing-class chain concatenated with its own simple name (e.g. {@code FooEndpoint.Baz}
     * -&gt; {@code FooEndpointBaz}, top-level {@code Address} -&gt; {@code Address}).
     *
     * <p>Pure and deterministic — identical to the name emitted by the victools definition
     * naming strategy, so every {@code $ref} we build matches the corresponding {@code $def}.
     * Disambiguation back to minimal display names happens once, in {@link #getGeneratedSchemas}.</p>
     */
    private String internalNameForClass(Class<?> clazz) {
        java.util.Deque<String> chain = new java.util.ArrayDeque<>();
        for (Class<?> cur = clazz; cur != null; cur = cur.getEnclosingClass()) {
            chain.addFirst(cur.getSimpleName());
        }
        return sanitizeSchemaName(String.join("", chain));
    }

    /**
     * Computes the internal name for a resolved (possibly generic) type, mirroring the
     * specialized/Map naming used elsewhere so def keys and our own refs stay in sync.
     */
    private String internalNameForResolvedType(com.fasterxml.classmate.ResolvedType type) {
        Class<?> erased = type.getErasedType();
        List<com.fasterxml.classmate.ResolvedType> params = type.getTypeParameters();
        if (Map.class.isAssignableFrom(erased) && params.size() == 2) {
            return "Map_" + internalNamePart(params.get(0))
                + "_" + internalNamePart(params.get(1)) + "_";
        }
        if (!params.isEmpty() && !Collection.class.isAssignableFrom(erased)) {
            StringBuilder name = new StringBuilder(internalNameForClass(erased));
            for (com.fasterxml.classmate.ResolvedType param : params) {
                name.append(internalNamePart(param));
            }
            return name.toString();
        }
        return internalNameForClass(erased);
    }

    private String internalNamePart(com.fasterxml.classmate.ResolvedType type) {
        Class<?> erased = type.getErasedType();
        if (erased.isPrimitive() || trySimpleTypeSchema(erased) != null) {
            return sanitizeSchemaName(erased.getSimpleName());
        }
        return internalNameForResolvedType(type);
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

        // Emit globally-unambiguous $def names (enclosing-chain qualified) so that no two
        // distinct classes ever share a def key — within or across generation calls. This
        // removes any need for content-based collision disambiguation downstream. Names are
        // collapsed back to minimal display names in getGeneratedSchemas().
        configBuilder.forTypesInGeneral()
            .withDefinitionNamingStrategy(new com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy() {
                @Override
                public String getDefinitionNameForKey(
                        com.github.victools.jsonschema.generator.impl.DefinitionKey key,
                        SchemaGenerationContext context) {
                    return internalNameForResolvedType(key.getType());
                }
            });

        // Unwrap Optional<T> fields and method return types to their inner type T.
        // Without this, victools treats Optional as an opaque object.
        configBuilder.forFields()
            .withTargetTypeOverridesResolver(field -> {
                com.github.victools.jsonschema.generator.TypeContext ctx =
                    field.getContext();
                com.fasterxml.classmate.ResolvedType resolvedType = field.getType();
                if (Optional.class.isAssignableFrom(resolvedType.getErasedType())
                    && resolvedType.getTypeParameters().size() == 1) {
                    return List.of(ctx.resolve(resolvedType.getTypeParameters().get(0)));
                }
                return null;
            });
        configBuilder.forMethods()
            .withTargetTypeOverridesResolver(method -> {
                com.github.victools.jsonschema.generator.TypeContext ctx =
                    method.getContext();
                com.fasterxml.classmate.ResolvedType resolvedType = method.getType();
                if (Optional.class.isAssignableFrom(resolvedType.getErasedType())
                    && resolvedType.getTypeParameters().size() == 1) {
                    return List.of(ctx.resolve(resolvedType.getTypeParameters().get(0)));
                }
                return null;
            });

        // Handle @JsonValue: types with a single @JsonValue-annotated member should
        // be represented as the type of that member (e.g., record SKU(@JsonValue String value) -> string)
        configBuilder.forTypesInGeneral()
            .withCustomDefinitionProvider((javaType, context) -> {
                Class<?> erasedType = javaType.getErasedType();
                if (erasedType.isEnum()) {
                    return null;
                }
                JsonValueSource source = findJsonValueSource(erasedType);
                if (source != null) {
                    ObjectNode schema = createJsonValueDefinition(source, context);
                    return new CustomDefinition(schema);
                }
                return null;
            });

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
     * Resolves the canonical internal component name under which {@link #generateSchema}
     * registers the given type in the component map, or {@code null} for types that are
     * never registered as named components (simple types, containers, void, internal
     * framework types).
     *
     * <p>Callers should use this name for both {@link #hasSchema(String)} checks and any
     * {@code $ref} they construct, so that references resolve consistently regardless of the
     * order in which types are encountered. The returned (enclosing-chain qualified) name is
     * collapsed to the final display name via {@link #getSchemaNameReplacements()}.</p>
     *
     * @param javaType the Java type to resolve a component name for
     * @return the canonical internal component name, or {@code null} if the type is not a named component
     */
    public String getComponentName(Type javaType) {
        Class<?> rawClass = getRawClass(javaType);
        if (rawClass == null || rawClass == Void.class || rawClass == void.class) {
            return null;
        }
        if (trySimpleTypeSchema(rawClass) != null) {
            return null;
        }
        if (Optional.class.isAssignableFrom(rawClass)
                || rawClass.isArray()
                || Collection.class.isAssignableFrom(rawClass)
                || Map.class.isAssignableFrom(rawClass)) {
            return null;
        }

        String typeName;
        if (javaType instanceof java.lang.reflect.ParameterizedType parameterizedType
                && !Collection.class.isAssignableFrom(rawClass)
                && !Map.class.isAssignableFrom(rawClass)) {
            typeName = getSpecializedTypeName(rawClass, parameterizedType);
        } else {
            typeName = getTypeName(rawClass);
        }

        if (isInternalSchemaName(typeName)) {
            return null;
        }
        return typeName;
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

        // Suppress framework-internal Akka/Scala types — treat as opaque object
        String earlyTypeName = getTypeName(rawClass);
        if (isInternalSchemaName(earlyTypeName)) {
            logger.accept("Suppressing internal framework type: " + earlyTypeName);
            return new ObjectSchema();
        }

        // Check for simple types first
        Schema<?> simpleSchema = trySimpleTypeSchema(rawClass);
        if (simpleSchema != null) {
            return simpleSchema;
        }

        Schema<?> containerSchema = tryContainerSchema(javaType, rawClass);
        if (containerSchema != null) {
            return containerSchema;
        }

        // For parameterized non-container types (e.g. PagedResponse<Customer>),
        // generate a specialized schema with a unique name incorporating the type argument.
        String typeName;
        if (javaType instanceof java.lang.reflect.ParameterizedType parameterizedType
                && !Collection.class.isAssignableFrom(rawClass)
                && !Map.class.isAssignableFrom(rawClass)) {
            typeName = getSpecializedTypeName(rawClass, parameterizedType);
        } else {
            typeName = getTypeName(rawClass);
        }

        // Also check the full specialized type name (e.g. Source_Object_NotUsed_)
        if (isInternalSchemaName(typeName)) {
            logger.accept("Suppressing internal framework type: " + typeName);
            return new ObjectSchema();
        }

        // Check for circular reference
        if (processingTypes.contains(typeName)) {
            logger.accept("Circular reference detected for: " + typeName);
            return createReference(typeName);
        }

        // Prefer explicit Jackson polymorphism over cached schemas that may have
        // been discovered as generic $defs from another type.
        Schema<?> polymorphicSchema = tryPolymorphicSchema(rawClass, typeName);
        if (polymorphicSchema != null) {
            generatedSchemas.put(typeName, polymorphicSchema);
            return polymorphicSchema;
        }

        // For complex types, check if already generated
        if (generatedSchemas.containsKey(typeName)) {
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

    private Schema<?> tryContainerSchema(Type javaType, Class<?> rawClass) {
        if (Optional.class.isAssignableFrom(rawClass)) {
            Type innerType = Object.class;
            if (javaType instanceof java.lang.reflect.ParameterizedType parameterizedType
                    && parameterizedType.getActualTypeArguments().length > 0) {
                innerType = parameterizedType.getActualTypeArguments()[0];
            }
            return resolveNestedSchema(innerType);
        }

        if (rawClass.isArray()) {
            ArraySchema arraySchema = new ArraySchema();
            arraySchema.setItems(resolveNestedSchema(rawClass.getComponentType()));
            return arraySchema;
        }

        if (Collection.class.isAssignableFrom(rawClass)) {
            ArraySchema arraySchema = new ArraySchema();
            Type itemType = Object.class;
            if (javaType instanceof java.lang.reflect.ParameterizedType parameterizedType
                    && parameterizedType.getActualTypeArguments().length > 0) {
                itemType = parameterizedType.getActualTypeArguments()[0];
            }
            arraySchema.setItems(resolveNestedSchema(itemType));
            return arraySchema;
        }

        // Handle Map<K, V> types as object with additionalProperties
        if (Map.class.isAssignableFrom(rawClass)) {
            ObjectSchema mapSchema = new ObjectSchema();
            if (javaType instanceof java.lang.reflect.ParameterizedType parameterizedType
                    && parameterizedType.getActualTypeArguments().length == 2) {
                Type valueType = parameterizedType.getActualTypeArguments()[1];
                Schema<?> valueSchema = resolveNestedSchema(valueType);
                mapSchema.setAdditionalProperties(valueSchema);
            } else {
                mapSchema.setAdditionalProperties(new ObjectSchema());
            }
            return mapSchema;
        }

        return null;
    }

    /**
     * Pre-scans a class's fields/record components for polymorphic types and Map value types,
     * generating their schemas first so they are available during victools processing.
     */
    private void preGeneratePolymorphicFields(Class<?> rawClass) {
        preGeneratePolymorphicFieldsRecursive(rawClass, new LinkedHashSet<>());
    }

    private void preGeneratePolymorphicFieldsRecursive(Class<?> rawClass, Set<Class<?>> visited) {
        if (!visited.add(rawClass)) return;
        // Check record components
        java.lang.reflect.RecordComponent[] components = rawClass.getRecordComponents();
        if (components != null) {
            for (java.lang.reflect.RecordComponent component : components) {
                preGenerateFieldType(component.getType(), component.getGenericType(), visited);
            }
        }
        // Check declared fields (for non-record classes)
        for (java.lang.reflect.Field field : rawClass.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            preGenerateFieldType(field.getType(), field.getGenericType(), visited);
        }
    }

    private void preGenerateFieldType(Class<?> fieldType, java.lang.reflect.Type genericType) {
        preGenerateFieldType(fieldType, genericType, new LinkedHashSet<>());
    }

    private void preGenerateFieldType(Class<?> fieldType, java.lang.reflect.Type genericType, Set<Class<?>> visited) {
        // Pre-generate polymorphic types
        if (isPolymorphicType(fieldType) && !generatedSchemas.containsKey(getTypeName(fieldType))) {
            generateSchema(fieldType);
            return;
        }
        // Pre-generate Map value types (so they exist when we inline Map refs)
        if (Map.class.isAssignableFrom(fieldType)
                && genericType instanceof java.lang.reflect.ParameterizedType pt
                && pt.getActualTypeArguments().length == 2) {
            java.lang.reflect.Type valueType = pt.getActualTypeArguments()[1];
            Class<?> valueClass = getRawClass(valueType);
            if (valueClass != null && trySimpleTypeSchema(valueClass) == null
                    && !generatedSchemas.containsKey(getTypeName(valueClass))) {
                generateSchema(valueType);
            }
        }
        // Recurse into collection/array element types to find nested polymorphic types
        Class<?> elementClass = null;
        if (genericType instanceof java.lang.reflect.ParameterizedType pt
                && (List.class.isAssignableFrom(fieldType) || Set.class.isAssignableFrom(fieldType)
                    || Collection.class.isAssignableFrom(fieldType))
                && pt.getActualTypeArguments().length == 1) {
            elementClass = getRawClass(pt.getActualTypeArguments()[0]);
        } else if (fieldType.isArray()) {
            elementClass = fieldType.getComponentType();
        }
        if (elementClass != null && !elementClass.isPrimitive()
                && trySimpleTypeSchema(elementClass) == null) {
            preGeneratePolymorphicFieldsRecursive(elementClass, visited);
        }
        // Recurse into nested complex types (records/POJOs) to find polymorphic fields
        if (!fieldType.isPrimitive() && !fieldType.isArray()
                && !Collection.class.isAssignableFrom(fieldType)
                && !Map.class.isAssignableFrom(fieldType)
                && trySimpleTypeSchema(fieldType) == null
                && !isPolymorphicType(fieldType)
                && !fieldType.isEnum()) {
            preGeneratePolymorphicFieldsRecursive(fieldType, visited);
        }
    }

    private Schema<?> resolveNestedSchema(Type nestedType) {
        Schema<?> schema = generateSchema(nestedType);
        if (schema == null) {
            return new ObjectSchema();
        }

        Class<?> nestedRawClass = getRawClass(nestedType);
        if (nestedRawClass != null) {
            String nestedTypeName = getTypeName(nestedRawClass);
            if (generatedSchemas.containsKey(nestedTypeName)) {
                return createReference(nestedTypeName);
            }
        }
        return schema;
    }

    private Schema<?> generateComplexSchema(Type javaType, Class<?> rawClass, String typeName) {
        try {
            logger.accept("Generating schema for: " + typeName);

            // Register class and all its nested types so defs post-processing can look them up.
            registerClassHierarchy(rawClass);

            // Pre-scan fields for polymorphic types and generate those first,
            // so their discriminated parent schemas exist before victools processes this type.
            preGeneratePolymorphicFields(rawClass);

            // Generate JSON Schema using victools
            ObjectNode jsonSchema = jsonSchemaGenerator.generateSchema(javaType);

            // Convert to OpenAPI Schema
            Schema<?> openApiSchema = convertJsonSchemaToOpenApi(jsonSchema, typeName);

            // Post-process: add required fields based on Optional vs non-Optional
            if (openApiSchema instanceof ObjectSchema) {
                enrichRequiredFields(rawClass, (ObjectSchema) openApiSchema);
            }

            // Store in generated schemas if it's a named type
            if (openApiSchema != null && !(openApiSchema instanceof ArraySchema)) {
                generatedSchemas.put(typeName, openApiSchema);
            }

            return openApiSchema;

        } catch (Exception e) {
            throw new SchemaGenerationException("Failed to generate schema for: " + typeName, e);
        }
    }

    /**
     * Checks if a type is a polymorphic type annotated with @JsonTypeInfo (or equivalent by name).
     */
    private boolean isPolymorphicType(Class<?> type) {
        for (java.lang.annotation.Annotation ann : type.getAnnotations()) {
            String annName = ann.annotationType().getName();
            if ("com.fasterxml.jackson.annotation.JsonTypeInfo".equals(annName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to generate a polymorphic schema for types annotated with
     * {@code @JsonTypeInfo} and {@code @JsonSubTypes}.
     *
     * @param rawClass the class to inspect
     * @param typeName the schema name for this type
     * @return a composed schema with oneOf/discriminator, or null if not polymorphic
     */
    private Schema<?> tryPolymorphicSchema(Class<?> rawClass, String typeName) {
        // Look up annotations by class identity first, then by name for cross-classloader support
        JsonTypeInfo typeInfo = rawClass.getAnnotation(JsonTypeInfo.class);
        JsonSubTypes subTypes = rawClass.getAnnotation(JsonSubTypes.class);

        // If direct annotation lookup fails, try by name (cross-classloader scenario)
        if (typeInfo == null || subTypes == null) {
            return tryPolymorphicSchemaByReflection(rawClass, typeName);
        }

        if (subTypes.value().length == 0) {
            return null;
        }

        List<PolymorphicSubtype> polymorphicSubtypes = new ArrayList<>();
        for (JsonSubTypes.Type subType : subTypes.value()) {
            Class<?> subClass = subType.value();
            polymorphicSubtypes.add(new PolymorphicSubtype(
                subClass, resolveDiscriminatorValue(subType.name(), subClass)));
        }

        return buildPolymorphicSchema(typeName, typeInfo.property(), polymorphicSubtypes, false);
    }

    /**
     * Fallback polymorphic schema generation using reflection to find annotations by name.
     * This handles the cross-classloader scenario where annotation classes loaded by the
     * plugin classloader differ from those loaded by the project classloader.
     */
    private Schema<?> tryPolymorphicSchemaByReflection(Class<?> rawClass, String typeName) {
        try {
            java.lang.annotation.Annotation typeInfoAnn = null;
            java.lang.annotation.Annotation subTypesAnn = null;

            for (java.lang.annotation.Annotation ann : rawClass.getAnnotations()) {
                String annName = ann.annotationType().getName();
                if ("com.fasterxml.jackson.annotation.JsonTypeInfo".equals(annName)) {
                    typeInfoAnn = ann;
                } else if ("com.fasterxml.jackson.annotation.JsonSubTypes".equals(annName)) {
                    subTypesAnn = ann;
                }
            }

            if (typeInfoAnn == null || subTypesAnn == null) {
                return null;
            }

            // Extract property from @JsonTypeInfo
            java.lang.reflect.Method propertyMethod = typeInfoAnn.annotationType().getMethod("property");
            String discriminatorProperty = (String) propertyMethod.invoke(typeInfoAnn);

            // Extract subtypes from @JsonSubTypes
            java.lang.reflect.Method valueMethod = subTypesAnn.annotationType().getMethod("value");
            Object[] subTypeArray = (Object[]) valueMethod.invoke(subTypesAnn);

            if (subTypeArray == null || subTypeArray.length == 0) {
                return null;
            }

            logger.accept("Detected polymorphic type (via reflection): " + typeName +
                " with " + subTypeArray.length + " subtypes");

            List<PolymorphicSubtype> polymorphicSubtypes = new ArrayList<>();

            for (Object subTypeObj : subTypeArray) {
                // Each element is a @JsonSubTypes.Type annotation
                java.lang.reflect.Method subValueMethod = subTypeObj.getClass().getMethod("value");
                java.lang.reflect.Method subNameMethod = subTypeObj.getClass().getMethod("name");

                Class<?> subClass = (Class<?>) subValueMethod.invoke(subTypeObj);
                String declaredName = (String) subNameMethod.invoke(subTypeObj);
                polymorphicSubtypes.add(new PolymorphicSubtype(
                    subClass, resolveDiscriminatorValue(declaredName, subClass)));
            }

            return buildPolymorphicSchema(typeName, discriminatorProperty, polymorphicSubtypes, true);

        } catch (Exception e) {
            logger.accept("Warning: reflection-based polymorphic detection failed for " +
                typeName + ": " + e.getMessage());
            return null;
        }
    }

    private Schema<?> buildPolymorphicSchema(
            String typeName,
            String discriminatorProperty,
            List<PolymorphicSubtype> subTypes,
            boolean detectedByReflection) {
        if (discriminatorProperty == null || discriminatorProperty.isBlank() || subTypes.isEmpty()) {
            return null;
        }

        String detectionMode = detectedByReflection ? " (via reflection)" : "";
        logger.accept("Detected polymorphic type" + detectionMode + ": " + typeName
            + " with " + subTypes.size() + " subtypes");

        List<Schema> oneOfSchemas = new ArrayList<>();
        Map<String, String> discriminatorMapping = new LinkedHashMap<>();

        for (PolymorphicSubtype subType : subTypes) {
            String subTypeName = getTypeName(subType.subClass());
            Schema<?> subSchema = generatedSchemas.get(subTypeName);
            if (subSchema == null) {
                subSchema = generateSubtypeSchema(
                    subType.subClass(), subTypeName, discriminatorProperty, subType.discriminatorValue());
            } else {
                ensureDiscriminatorProperty(subSchema, discriminatorProperty, subType.discriminatorValue());
            }
            // Tooling (Swagger UI, Redoc, Stoplight) labels each oneOf branch from the
            // referenced schema's title, so make sure each subtype carries one.
            ensurePolymorphicSubtypeTitle(subSchema, subTypeName);

            Schema<?> ref = new Schema<>();
            ref.set$ref("#/components/schemas/" + subTypeName);
            oneOfSchemas.add(ref);
            discriminatorMapping.put(
                subType.discriminatorValue(), "#/components/schemas/" + subTypeName);
        }

        ComposedSchema composedSchema = new ComposedSchema();
        composedSchema.setOneOf(oneOfSchemas);

        Discriminator discriminator = new Discriminator();
        discriminator.setPropertyName(discriminatorProperty);
        discriminator.setMapping(discriminatorMapping);
        composedSchema.setDiscriminator(discriminator);

        // Enforce discriminator property as required at the union root level
        composedSchema.setRequired(Collections.singletonList(discriminatorProperty));
        StringSchema discriminatorPropSchema = new StringSchema();
        composedSchema.addProperty(discriminatorProperty, discriminatorPropSchema);

        return composedSchema;
    }

    private Schema<?> generateSubtypeSchema(
            Class<?> subClass,
            String subTypeName,
            String discriminatorProperty,
            String discriminatorValue) {
        Schema<?> subSchema;
        try {
            ObjectNode subJsonSchema = jsonSchemaGenerator.generateSchema(subClass);
            subSchema = convertJsonSchemaToOpenApi(subJsonSchema, subTypeName);
            if (isEmptyObjectSchema(subSchema) || subSchema.get$ref() != null) {
                Schema<?> reflectionSchema = generateSchemaByReflection(subClass, subTypeName);
                if (reflectionSchema != null) {
                    subSchema = reflectionSchema;
                }
            }
        } catch (Exception e) {
            logger.accept("Warning: could not generate schema for subtype " +
                subTypeName + " via victools, trying reflection: " + e.getMessage());
            subSchema = generateSchemaByReflection(subClass, subTypeName);
        }

        if (subSchema == null) {
            subSchema = new ObjectSchema();
        }
        ensureDiscriminatorProperty(subSchema, discriminatorProperty, discriminatorValue);
        ensurePolymorphicSubtypeTitle(subSchema, subTypeName);
        generatedSchemas.put(subTypeName, subSchema);
        return subSchema;
    }

    private void ensurePolymorphicSubtypeTitle(Schema<?> subSchema, String subTypeName) {
        if (subSchema == null || subSchema.get$ref() != null) return;
        if (subSchema.getTitle() == null || subSchema.getTitle().isBlank()) {
            subSchema.setTitle(subTypeName);
        }
    }

    private boolean isEmptyObjectSchema(Schema<?> schema) {
        return schema instanceof ObjectSchema
            && (schema.getProperties() == null || schema.getProperties().isEmpty())
            && ((ObjectSchema) schema).getAdditionalProperties() == null;
    }

    /**
     * Extracts the value type name from a Map def name like "Map_String_ComponentHealth_".
     * Returns the last type segment (e.g. "ComponentHealth"), or null if unparseable.
     */
    private String extractMapValueType(String defName) {
        // Pattern: Map_KeyType_ValueType_ (underscores replacing angle brackets/commas)
        if (!defName.startsWith("Map_")) {
            return null;
        }
        String rest = defName.substring("Map_".length());
        // Remove trailing underscore
        if (rest.endsWith("_")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        // Split by underscore — first segment is key type, rest is value type
        int firstUnderscore = rest.indexOf('_');
        if (firstUnderscore < 0) {
            return null;
        }
        String valueType = rest.substring(firstUnderscore + 1);
        // Remove trailing underscore if present
        if (valueType.endsWith("_")) {
            valueType = valueType.substring(0, valueType.length() - 1);
        }
        return valueType.isEmpty() ? null : valueType;
    }

    private void ensureDiscriminatorProperty(
            Schema<?> schema, String discriminatorProperty, String discriminatorValue) {
        if (schema == null || schema.get$ref() != null
                || discriminatorProperty == null || discriminatorProperty.isBlank()) {
            return;
        }

        Schema<?> discriminatorSchema = schema.getProperties() != null
            ? schema.getProperties().get(discriminatorProperty)
            : null;
        if (discriminatorSchema == null) {
            discriminatorSchema = new StringSchema();
            schema.addProperty(discriminatorProperty, discriminatorSchema);
        }

        setSingleAllowedValue(discriminatorSchema, discriminatorValue);
        List<String> required = schema.getRequired() != null
            ? new ArrayList<>(schema.getRequired())
            : new ArrayList<>();
        if (!required.contains(discriminatorProperty)) {
            required.add(discriminatorProperty);
            schema.setRequired(required);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setSingleAllowedValue(Schema<?> schema, String value) {
        schema.setConst(value);
        ((Schema) schema).setEnum(Collections.singletonList(value));
    }

    private String resolveDiscriminatorValue(String declaredName, Class<?> subClass) {
        return declaredName != null && !declaredName.isBlank()
            ? declaredName
            : getTypeName(subClass);
    }

    private record PolymorphicSubtype(Class<?> subClass, String discriminatorValue) {
    }

    /**
     * Generates a schema for a class using pure Java reflection.
     *
     * <p>This is used as a fallback for types that victools cannot introspect
     * correctly (e.g., records implementing sealed interfaces loaded via a
     * different classloader). It inspects record components first, then
     * falls back to public getter methods.</p>
     *
     * @param clazz the class to inspect
     * @param typeName the schema name to use
     * @return an ObjectSchema with the discovered properties, or null if none found
     */
    private Schema<?> generateSchemaByReflection(Class<?> clazz, String typeName) {
        try {
            logger.accept("Generating schema via reflection for: " + typeName);

            // Check for @JsonValue annotation — the type serializes as the value type directly
            Schema<?> jsonValueSchema = tryJsonValueSchema(clazz);
            if (jsonValueSchema != null) {
                return jsonValueSchema;
            }

            ObjectSchema objectSchema = new ObjectSchema();
            List<String> requiredFields = new ArrayList<>();

            // Java records expose components via Class.getRecordComponents()
            java.lang.reflect.RecordComponent[] components = clazz.getRecordComponents();
            if (components != null && components.length > 0) {
                for (java.lang.reflect.RecordComponent component : components) {
                    java.lang.reflect.Method accessor = component.getAccessor();
                    if (hasJsonIgnore(component, accessor)) {
                        continue;
                    }
                    String propName = getJsonPropertyName(component.getName(), component, accessor);
                    Class<?> propType = component.getType();
                    Schema<?> propSchema = mapJavaTypeToSchema(propType, component.getGenericType());
                    if (propSchema != null) {
                        objectSchema.addProperty(propName, propSchema);
                        // Mark Optional properties as nullable (OAS 3.1 style)
                        if (Optional.class.isAssignableFrom(propType)) {
                            Schema<?> nullableSchema = makeNullableSchema(propSchema);
                            if (nullableSchema != propSchema) {
                                objectSchema.getProperties().put(propName, nullableSchema);
                            }
                        }
                    }
                    // Non-Optional fields are required
                    if (!Optional.class.isAssignableFrom(propType) && !isNullableAnnotated(component, accessor)) {
                        requiredFields.add(propName);
                    }
                }
                if (!requiredFields.isEmpty()) {
                    objectSchema.setRequired(requiredFields);
                }
                return objectSchema;
            }

            // For non-records, inspect public getter methods
            for (java.lang.reflect.Method method : clazz.getMethods()) {
                String methodName = method.getName();
                if (method.getParameterCount() != 0 || method.getDeclaringClass() == Object.class) {
                    continue;
                }
                String propName = null;
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    propName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                } else if (methodName.startsWith("is") && methodName.length() > 2) {
                    propName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
                }
                if (propName != null && !propName.isBlank()) {
                    if (hasJsonIgnore(method)) {
                        continue;
                    }
                    propName = getJsonPropertyName(propName, method);
                    Class<?> returnType = method.getReturnType();
                    Schema<?> propSchema = mapJavaTypeToSchema(returnType, method.getGenericReturnType());
                    if (propSchema != null) {
                        objectSchema.addProperty(propName, propSchema);
                        // Mark Optional return types as nullable (OAS 3.1 style)
                        if (Optional.class.isAssignableFrom(returnType)) {
                            Schema<?> nullableSchema = makeNullableSchema(propSchema);
                            if (nullableSchema != propSchema) {
                                objectSchema.getProperties().put(propName, nullableSchema);
                            }
                        }
                    }
                    // Non-Optional return types are required
                    if (!Optional.class.isAssignableFrom(returnType)
                            && !isNullableAnnotated(method)
                            && !returnType.isPrimitive()) {
                        requiredFields.add(propName);
                    }
                }
            }

            if (!requiredFields.isEmpty()) {
                objectSchema.setRequired(requiredFields);
            }
            return objectSchema.getProperties() != null && !objectSchema.getProperties().isEmpty()
                ? objectSchema : null;

        } catch (Exception e) {
            logger.accept("Warning: reflection-based schema generation failed for " +
                typeName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the class has a {@code @JsonValue} annotation on any field, method, or
     * record component, and if so returns the schema for the value type directly.
     * This handles value-wrapper types that serialize as their inner value.
     */
    private Schema<?> tryJsonValueSchema(Class<?> clazz) {
        if (clazz.isEnum()) {
            return null;
        }
        JsonValueSource source = findJsonValueSource(clazz);
        if (source == null) {
            return null;
        }
        Class<?> rawClass = getRawClass(source.type());
        if (rawClass == null) {
            return null;
        }
        Schema<?> schema = mapJavaTypeToSchema(rawClass, source.type());
        if (schema != null) {
            applyJakartaConstraintsToSchema(schema, source.primary(), source.accessor());
        }
        return schema;
    }

    /**
     * Checks if an annotated element has a {@code @JsonValue} annotation,
     * handling cross-classloader scenarios by checking annotation type name.
     */
    private boolean hasJsonValueAnnotation(java.lang.reflect.AnnotatedElement element) {
        for (java.lang.annotation.Annotation ann : element.getAnnotations()) {
            String annName = ann.annotationType().getName();
            if ("com.fasterxml.jackson.annotation.JsonValue".equals(annName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Locates the @JsonValue-annotated member in a class along with the annotated elements
     * (member + accessor) that may carry Jakarta Validation constraints. Returns null if
     * no @JsonValue is found.
     */
    private JsonValueSource findJsonValueSource(Class<?> clazz) {
        java.lang.reflect.RecordComponent[] components = clazz.getRecordComponents();
        if (components != null) {
            for (java.lang.reflect.RecordComponent component : components) {
                java.lang.reflect.Method accessor = component.getAccessor();
                if (hasJsonValueAnnotation(accessor) || hasJsonValueAnnotation(component)) {
                    return new JsonValueSource(component.getGenericType(), component, accessor);
                }
            }
        }
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && hasJsonValueAnnotation(method)) {
                return new JsonValueSource(method.getGenericReturnType(), method, null);
            }
        }
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (hasJsonValueAnnotation(field)) {
                return new JsonValueSource(field.getGenericType(), field, null);
            }
        }
        return null;
    }

    private Type findJsonValueType(Class<?> clazz) {
        JsonValueSource source = findJsonValueSource(clazz);
        return source != null ? source.type() : null;
    }

    private record JsonValueSource(Type type, AnnotatedElement primary, AnnotatedElement accessor) {}

    private ObjectNode createJsonValueDefinition(JsonValueSource source, SchemaGenerationContext context) {
        Class<?> rawClass = getRawClass(source.type());
        ObjectNode simpleSchema = rawClass != null ? createSimpleJsonSchema(rawClass) : null;
        if (simpleSchema == null) {
            return context.createDefinition(context.getTypeContext().resolve(source.type()));
        }
        applyJakartaConstraintsToObjectNode(simpleSchema, source.primary(), source.accessor());
        return simpleSchema;
    }

    private ObjectNode createSimpleJsonSchema(Class<?> type) {
        ObjectNode schema = objectMapper.createObjectNode();
        if (type == String.class || type == CharSequence.class) {
            schema.put("type", "string");
        } else if (type == char.class || type == Character.class) {
            schema.put("type", "string");
            schema.put("minLength", 1);
            schema.put("maxLength", 1);
        } else if (type == int.class || type == Integer.class ||
                   type == short.class || type == Short.class ||
                   type == byte.class || type == Byte.class) {
            schema.put("type", "integer");
            schema.put("format", "int32");
        } else if (type == long.class || type == Long.class) {
            schema.put("type", "integer");
            schema.put("format", "int64");
        } else if (type == BigInteger.class) {
            schema.put("type", "integer");
        } else if (type == float.class || type == Float.class) {
            schema.put("type", "number");
            schema.put("format", "float");
        } else if (type == double.class || type == Double.class) {
            schema.put("type", "number");
            schema.put("format", "double");
        } else if (type == BigDecimal.class) {
            schema.put("type", "number");
        } else if (type == boolean.class || type == Boolean.class) {
            schema.put("type", "boolean");
        } else if (type == UUID.class) {
            schema.put("type", "string");
            schema.put("format", "uuid");
        } else if (type == LocalDate.class) {
            schema.put("type", "string");
            schema.put("format", "date");
        } else if (type == LocalDateTime.class || type == ZonedDateTime.class ||
                   type == OffsetDateTime.class || type == Instant.class) {
            schema.put("type", "string");
            schema.put("format", "date-time");
        } else if (type == LocalTime.class || type == OffsetTime.class) {
            schema.put("type", "string");
            schema.put("format", "time");
        } else if (type == Duration.class || type == Period.class) {
            schema.put("type", "string");
            schema.put("format", "duration");
        } else if (type == byte[].class) {
            schema.put("type", "string");
            schema.put("format", "binary");
        } else if (type == Object.class) {
            schema.put("type", "object");
        } else {
            return null;
        }
        return schema;
    }

    private static final String JAKARTA_CONSTRAINTS_PKG = "jakarta.validation.constraints.";

    /**
     * Applies Jakarta Validation constraints (read from the given annotated elements) as
     * JSON Schema keywords on the supplied ObjectNode. Used for the @JsonValue unwrap
     * path where victools' own JakartaValidationModule does not run.
     */
    private void applyJakartaConstraintsToObjectNode(ObjectNode schema, AnnotatedElement... sources) {
        String typeStr = schema.path("type").asText("");
        for (AnnotatedElement source : sources) {
            if (source == null) continue;
            for (java.lang.annotation.Annotation ann : source.getAnnotations()) {
                try {
                    applyAnnotationToObjectNode(schema, typeStr, ann);
                } catch (Exception ignored) {
                    // Defensive: skip any annotation whose accessors don't match expected shapes.
                }
            }
        }
    }

    private void applyAnnotationToObjectNode(ObjectNode schema, String typeStr,
            java.lang.annotation.Annotation ann) throws Exception {
        String name = ann.annotationType().getName();
        if (!name.startsWith(JAKARTA_CONSTRAINTS_PKG)) return;
        String simple = name.substring(JAKARTA_CONSTRAINTS_PKG.length());
        switch (simple) {
            case "DecimalMin": {
                BigDecimal v = new BigDecimal(invokeAnnotationString(ann, "value"));
                if (invokeAnnotationBoolean(ann, "inclusive")) schema.put("minimum", v);
                else schema.put("exclusiveMinimum", v);
                break;
            }
            case "DecimalMax": {
                BigDecimal v = new BigDecimal(invokeAnnotationString(ann, "value"));
                if (invokeAnnotationBoolean(ann, "inclusive")) schema.put("maximum", v);
                else schema.put("exclusiveMaximum", v);
                break;
            }
            case "Min":
                schema.put("minimum", BigDecimal.valueOf(invokeAnnotationLong(ann, "value")));
                break;
            case "Max":
                schema.put("maximum", BigDecimal.valueOf(invokeAnnotationLong(ann, "value")));
                break;
            case "Positive":
                schema.put("exclusiveMinimum", BigDecimal.ZERO);
                break;
            case "PositiveOrZero":
                schema.put("minimum", BigDecimal.ZERO);
                break;
            case "Negative":
                schema.put("exclusiveMaximum", BigDecimal.ZERO);
                break;
            case "NegativeOrZero":
                schema.put("maximum", BigDecimal.ZERO);
                break;
            case "Size": {
                int min = invokeAnnotationInt(ann, "min");
                int max = invokeAnnotationInt(ann, "max");
                if ("string".equals(typeStr)) {
                    if (min > 0) schema.put("minLength", min);
                    if (max < Integer.MAX_VALUE) schema.put("maxLength", max);
                } else if ("array".equals(typeStr)) {
                    if (min > 0) schema.put("minItems", min);
                    if (max < Integer.MAX_VALUE) schema.put("maxItems", max);
                } else if ("object".equals(typeStr)) {
                    if (min > 0) schema.put("minProperties", min);
                    if (max < Integer.MAX_VALUE) schema.put("maxProperties", max);
                }
                break;
            }
            case "NotBlank":
            case "NotEmpty":
                if ("string".equals(typeStr)) {
                    if (!schema.has("minLength")) schema.put("minLength", 1);
                } else if ("array".equals(typeStr)) {
                    if (!schema.has("minItems")) schema.put("minItems", 1);
                } else if ("object".equals(typeStr)) {
                    if (!schema.has("minProperties")) schema.put("minProperties", 1);
                }
                break;
            case "Pattern":
                schema.put("pattern", invokeAnnotationString(ann, "regexp"));
                break;
            case "Digits": {
                int integerDigits = invokeAnnotationInt(ann, "integer");
                int fractionDigits = invokeAnnotationInt(ann, "fraction");
                String pattern = fractionDigits > 0
                    ? "^-?\\d{0," + integerDigits + "}(\\.\\d{1," + fractionDigits + "})?$"
                    : "^-?\\d{0," + integerDigits + "}$";
                if (!schema.has("pattern")) schema.put("pattern", pattern);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Applies Jakarta Validation constraints directly to a swagger Schema, for the
     * reflection-based @JsonValue path where the ObjectNode pipeline is skipped.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applyJakartaConstraintsToSchema(Schema<?> schema, AnnotatedElement... sources) {
        if (schema == null) return;
        String typeStr = inferSchemaTypeKey(schema);
        for (AnnotatedElement source : sources) {
            if (source == null) continue;
            for (java.lang.annotation.Annotation ann : source.getAnnotations()) {
                try {
                    applyAnnotationToSchema(schema, typeStr, ann);
                } catch (Exception ignored) {
                    // Defensive: skip annotations whose shapes don't match expectations.
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applyAnnotationToSchema(Schema schema, String typeStr,
            java.lang.annotation.Annotation ann) throws Exception {
        String name = ann.annotationType().getName();
        if (!name.startsWith(JAKARTA_CONSTRAINTS_PKG)) return;
        String simple = name.substring(JAKARTA_CONSTRAINTS_PKG.length());
        switch (simple) {
            case "DecimalMin": {
                BigDecimal v = new BigDecimal(invokeAnnotationString(ann, "value"));
                if (invokeAnnotationBoolean(ann, "inclusive")) schema.setMinimum(v);
                else schema.setExclusiveMinimumValue(v);
                break;
            }
            case "DecimalMax": {
                BigDecimal v = new BigDecimal(invokeAnnotationString(ann, "value"));
                if (invokeAnnotationBoolean(ann, "inclusive")) schema.setMaximum(v);
                else schema.setExclusiveMaximumValue(v);
                break;
            }
            case "Min":
                schema.setMinimum(BigDecimal.valueOf(invokeAnnotationLong(ann, "value")));
                break;
            case "Max":
                schema.setMaximum(BigDecimal.valueOf(invokeAnnotationLong(ann, "value")));
                break;
            case "Positive":
                schema.setExclusiveMinimumValue(BigDecimal.ZERO);
                break;
            case "PositiveOrZero":
                schema.setMinimum(BigDecimal.ZERO);
                break;
            case "Negative":
                schema.setExclusiveMaximumValue(BigDecimal.ZERO);
                break;
            case "NegativeOrZero":
                schema.setMaximum(BigDecimal.ZERO);
                break;
            case "Size": {
                int min = invokeAnnotationInt(ann, "min");
                int max = invokeAnnotationInt(ann, "max");
                if ("string".equals(typeStr)) {
                    if (min > 0) schema.setMinLength(min);
                    if (max < Integer.MAX_VALUE) schema.setMaxLength(max);
                } else if ("array".equals(typeStr)) {
                    if (min > 0) schema.setMinItems(min);
                    if (max < Integer.MAX_VALUE) schema.setMaxItems(max);
                }
                break;
            }
            case "NotBlank":
            case "NotEmpty":
                if ("string".equals(typeStr)) {
                    if (schema.getMinLength() == null) schema.setMinLength(1);
                } else if ("array".equals(typeStr)) {
                    if (schema.getMinItems() == null) schema.setMinItems(1);
                }
                break;
            case "Pattern":
                schema.setPattern(invokeAnnotationString(ann, "regexp"));
                break;
            case "Digits": {
                int integerDigits = invokeAnnotationInt(ann, "integer");
                int fractionDigits = invokeAnnotationInt(ann, "fraction");
                String pattern = fractionDigits > 0
                    ? "^-?\\d{0," + integerDigits + "}(\\.\\d{1," + fractionDigits + "})?$"
                    : "^-?\\d{0," + integerDigits + "}$";
                if (schema.getPattern() == null) schema.setPattern(pattern);
                break;
            }
            default:
                break;
        }
    }

    private static String inferSchemaTypeKey(Schema<?> schema) {
        if (schema.getType() != null) return schema.getType();
        if (schema instanceof StringSchema) return "string";
        if (schema instanceof IntegerSchema) return "integer";
        if (schema instanceof NumberSchema) return "number";
        if (schema instanceof ArraySchema) return "array";
        return "";
    }

    private static String invokeAnnotationString(java.lang.annotation.Annotation ann, String method)
            throws Exception {
        return (String) ann.annotationType().getMethod(method).invoke(ann);
    }

    private static boolean invokeAnnotationBoolean(java.lang.annotation.Annotation ann, String method)
            throws Exception {
        return (Boolean) ann.annotationType().getMethod(method).invoke(ann);
    }

    private static int invokeAnnotationInt(java.lang.annotation.Annotation ann, String method)
            throws Exception {
        return (Integer) ann.annotationType().getMethod(method).invoke(ann);
    }

    private static long invokeAnnotationLong(java.lang.annotation.Annotation ann, String method)
            throws Exception {
        return (Long) ann.annotationType().getMethod(method).invoke(ann);
    }

    /**
     * Post-processes a victools-generated ObjectSchema to add required fields
     * based on Optional vs non-Optional type declarations in the source class.
     * Also marks Optional properties as nullable.
     * This supplements what victools detects via @NotNull/@NotBlank.
     */
    @SuppressWarnings("unchecked")
    private void enrichRequiredFields(Class<?> rawClass, ObjectSchema schema) {
        if (schema.getProperties() == null) return;
        Set<String> existingRequired = schema.getRequired() != null
                ? new LinkedHashSet<>(schema.getRequired()) : new LinkedHashSet<>();
        List<String> toAdd = new ArrayList<>();

        // Check record components
        java.lang.reflect.RecordComponent[] components = rawClass.getRecordComponents();
        if (components != null) {
            for (java.lang.reflect.RecordComponent component : components) {
                String propName = getJsonPropertyName(component.getName(), component, component.getAccessor());
                if (!schema.getProperties().containsKey(propName)) continue;

                if (Optional.class.isAssignableFrom(component.getType())) {
                    // Mark Optional properties as nullable (OAS 3.1 style)
                    Schema<?> propSchema = (Schema<?>) schema.getProperties().get(propName);
                    if (propSchema != null) {
                        Schema<?> nullableSchema = makeNullableSchema(propSchema);
                        if (nullableSchema != propSchema) {
                            schema.getProperties().put(propName, nullableSchema);
                        }
                    }
                } else if (!existingRequired.contains(propName)
                        && !isNullableAnnotated(component, component.getAccessor())) {
                    toAdd.add(propName);
                }
            }
        } else {
            // Check declared fields for non-record classes
            for (java.lang.reflect.Field field : rawClass.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                String propName = getJsonPropertyName(field.getName(), field);
                if (!schema.getProperties().containsKey(propName)) continue;

                if (Optional.class.isAssignableFrom(field.getType())) {
                    // Mark Optional properties as nullable (OAS 3.1 style)
                    Schema<?> propSchema = (Schema<?>) schema.getProperties().get(propName);
                    if (propSchema != null) {
                        Schema<?> nullableSchema = makeNullableSchema(propSchema);
                        if (nullableSchema != propSchema) {
                            schema.getProperties().put(propName, nullableSchema);
                        }
                    }
                } else if (!existingRequired.contains(propName)
                        && !isNullableAnnotated(field)) {
                    toAdd.add(propName);
                }
            }
        }

        if (!toAdd.isEmpty()) {
            List<String> required = new ArrayList<>(existingRequired);
            required.addAll(toAdd);
            schema.setRequired(required);
        }
    }

    /**
     * Final pass: for every ObjectSchema stored in generatedSchemas, if its name maps to a known
     * class in the classRegistry, call enrichRequiredFields on it. This catches all schemas that
     * were stored from victools $defs processing and never individually enriched.
     */
    private void enrichAllGeneratedSchemas() {
        for (Map.Entry<String, Schema<?>> entry : generatedSchemas.entrySet()) {
            if (!(entry.getValue() instanceof ObjectSchema objectSchema)) continue;
            if (objectSchema.getProperties() == null) continue;
            Class<?> clazz = classRegistry.get(entry.getKey());
            if (clazz != null) {
                enrichRequiredFields(clazz, objectSchema);
            }
        }
    }

    /**
     * Returns true if any of the given annotated elements has a nullable annotation     * (e.g. @Nullable from any package, or @NotNull absent indicates nullable).
     * We use the simpler heuristic: Optional type = nullable, otherwise required.
     * This helper checks for explicit @Nullable annotation overrides.
     */
    private boolean isNullableAnnotated(java.lang.reflect.AnnotatedElement... elements) {
        for (java.lang.reflect.AnnotatedElement element : elements) {
            if (element == null) continue;
            for (java.lang.annotation.Annotation ann : element.getAnnotations()) {
                String name = ann.annotationType().getSimpleName();
                if ("Nullable".equals(name) || "Null".equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Makes a schema nullable in an OpenAPI 3.1-compatible way.
     * For $ref schemas, wraps in oneOf with a null type.
     * For typed schemas, adds "null" to the types set.
     * Idempotent: returns the schema unchanged if already nullable.
     *
     * @param schema the schema to make nullable
     * @return the nullable schema (may be a new ComposedSchema if input was a $ref)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Schema<?> makeNullableSchema(Schema<?> schema) {
        if (schema == null) return null;
        // Already nullable: check if types include "null" or oneOf already has a null entry
        if (schema.getTypes() != null && schema.getTypes().contains("null")) {
            return schema;
        }
        if (schema.getOneOf() != null && schema.getOneOf().stream().anyMatch(this::isNullTypeSchema)) {
            return schema;
        }
        if (schema.get$ref() != null) {
            // For $ref schemas in OAS 3.1, wrap in oneOf with null type
            ComposedSchema composed = new ComposedSchema();
            Schema<?> refPart = new Schema<>();
            refPart.set$ref(schema.get$ref());
            Schema<?> nullPart = new Schema<>();
            nullPart.addType("null");
            composed.setOneOf(List.of(refPart, nullPart));
            return composed;
        } else {
            // For typed schemas, add "null" to types
            schema.addType("null");
            return schema;
        }
    }

    private boolean isNullTypeSchema(Schema<?> schema) {
        return schema != null
            && schema.getTypes() != null
            && schema.getTypes().contains("null")
            && schema.getTypes().size() == 1;
    }

    private boolean hasJsonIgnore(java.lang.reflect.AnnotatedElement... elements) {
        for (AnnotatedElement element : elements) {
            if (element != null && element.getAnnotation(JsonIgnore.class) != null) {
                return true;
            }
        }
        return false;
    }

    private String getJsonPropertyName(String defaultName, AnnotatedElement... elements) {
        for (AnnotatedElement element : elements) {
            if (element == null) {
                continue;
            }
            JsonProperty jsonProperty = element.getAnnotation(JsonProperty.class);
            if (jsonProperty != null && jsonProperty.value() != null && !jsonProperty.value().isBlank()) {
                return jsonProperty.value();
            }
        }
        return defaultName;
    }

    /**
     * Maps a Java type to an OpenAPI schema using simple type matching.
     * Used by the reflection-based schema generator.
     */
    private Schema<?> mapJavaTypeToSchema(Class<?> type, java.lang.reflect.Type genericType) {
        // Unwrap Optional<T>
        if (Optional.class.isAssignableFrom(type) ||
            "java.util.Optional".equals(type.getName())) {
            if (genericType instanceof java.lang.reflect.ParameterizedType pt) {
                java.lang.reflect.Type[] args = pt.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> innerClass) {
                    return mapJavaTypeToSchema(innerClass, innerClass);
                } else if (args.length > 0 && args[0] instanceof java.lang.reflect.ParameterizedType innerPt) {
                    Class<?> rawInner = getRawClass(innerPt);
                    if (rawInner != null) {
                        return mapJavaTypeToSchema(rawInner, innerPt);
                    }
                }
            }
            return new ObjectSchema();
        }

        // Simple types
        Schema<?> simple = trySimpleTypeSchema(type);
        if (simple != null) {
            return simple;
        }

        // Collections: List, Set
        if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
            ArraySchema array = new ArraySchema();
            if (genericType instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.Type inner =
                    ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments()[0];
                if (inner instanceof Class<?>) {
                    array.setItems(mapJavaTypeToSchema((Class<?>) inner, inner));
                }
            }
            return array;
        }

        // Enum
        if (type.isEnum()) {
            // Register the enum class for collision detection
            registerClassHierarchy(type);
            String enumSchemaName = resolveSchemaName(type);
            StringSchema enumSchema = new StringSchema();
            for (Object constant : type.getEnumConstants()) {
                enumSchema.addEnumItem(((Enum<?>) constant).name());
            }
            // Store in generatedSchemas so it becomes a referenceable component
            if (!generatedSchemas.containsKey(enumSchemaName)) {
                generatedSchemas.put(enumSchemaName, enumSchema);
            }
            return createReference(enumSchemaName);
        }

        // Polymorphic type (sealed interface with @JsonTypeInfo) — emit $ref to parent discriminated schema
        if (isPolymorphicType(type)) {
            String refTypeName = resolveSchemaName(type);
            if (!generatedSchemas.containsKey(refTypeName)) {
                // Trigger polymorphic schema generation
                generateSchema(type);
            }
            return createReference(refTypeName);
        }

        // Complex object - use $ref if already generated, otherwise generate and store
        String refTypeName = resolveSchemaName(type);
        // Check if we need to generate or upgrade the schema
        boolean needsGeneration = !generatedSchemas.containsKey(refTypeName);
        if (!needsGeneration) {
            // Also regenerate if existing schema is an empty ObjectSchema (placeholder from $defs)
            Schema<?> existing = generatedSchemas.get(refTypeName);
            if (existing instanceof ObjectSchema &&
                (((ObjectSchema) existing).getProperties() == null ||
                 ((ObjectSchema) existing).getProperties().isEmpty()) &&
                ((ObjectSchema) existing).getAdditionalProperties() == null) {
                needsGeneration = true;
            }
        }
        if (needsGeneration) {
            // Register class so nested defs from this type can be enriched with required fields
            registerClassHierarchy(type);
            // Store placeholder first to prevent infinite recursion for circular references
            generatedSchemas.put(refTypeName, new ObjectSchema());
            // Try to generate schema by reflection for records and POJOs
            Schema<?> generated = generateSchemaByReflection(type, refTypeName);
            if (generated != null) {
                generatedSchemas.put(refTypeName, generated);
            }
        }
        return createReference(refTypeName);
    }

    private Schema<?> convertJsonSchemaToOpenApi(ObjectNode jsonSchema, String rootTypeName) {
        // Extract definitions first (they may be under $defs in draft 2020-12)
        JsonNode defs = getDefinitions(jsonSchema);
        Set<String> rootAliases = new LinkedHashSet<>();
        JsonNode rootSchema = resolveRootSchemaNode(jsonSchema, defs, rootAliases);
        rootAliases.forEach(alias -> schemaNameAliases.put(alias, rootTypeName));

        if (defs != null && defs.isObject()) {
            // First pass: process non-Map, non-nullable defs. Def names are canonical,
            // globally-unambiguous internal names, so each is stored directly under its
            // own name — no collision disambiguation required.
            defs.fields().forEachRemaining(entry -> {
                String defName = sanitizeSchemaName(entry.getKey());
                if (defName.endsWith("-nullable") || defName.startsWith("Map_")) {
                    return;
                }
                // Skip framework-internal/Akka SDK types
                if (isInternalSchemaName(defName)) {
                    return;
                }
                if (rootAliases.contains(defName) || generatedSchemas.containsKey(defName)) {
                    return;
                }
                Schema<?> defSchema = convertJsonNodeToSchema(entry.getValue(), defName);
                if (defSchema == null) {
                    return;
                }
                // A def that is purely a $ref to another schema is an alias — record it
                // instead of materialising a redundant component.
                if (defSchema.get$ref() != null) {
                    String aliasTarget = extractRefName(defSchema.get$ref(), defName);
                    if (aliasTarget != null && !aliasTarget.equals(defName)
                            && !defSchema.get$ref().equals("#/components/schemas/" + defName)) {
                        schemaNameAliases.put(defName, aliasTarget);
                        return;
                    }
                }
                // Avoid storing self-referencing schemas (e.g., from "#" circular refs)
                if (defSchema.get$ref() != null &&
                    defSchema.get$ref().equals("#/components/schemas/" + defName)) {
                    ObjectSchema objectSchema = new ObjectSchema();
                    objectSchema.setAdditionalProperties(new ObjectSchema());
                    generatedSchemas.put(defName, objectSchema);
                } else {
                    generatedSchemas.put(defName, defSchema);
                }
            });

            // Second pass: process Map-type defs (value types should now be registered)
            defs.fields().forEachRemaining(entry -> {
                String defName = sanitizeSchemaName(entry.getKey());
                if (!defName.startsWith("Map_")) {
                    return;
                }
                String valueTypeName = extractMapValueType(defName);
                if (valueTypeName != null) {
                    ObjectSchema mapSchema = new ObjectSchema();
                    mapSchema.setAdditionalProperties(createReference(valueTypeName));
                    generatedSchemas.put(defName, mapSchema);
                    // If value type wasn't generated in first pass, try from additionalProperties or defs
                    if (!generatedSchemas.containsKey(valueTypeName)) {
                        JsonNode mapNode = entry.getValue();
                        if (mapNode.has("additionalProperties")) {
                            JsonNode addPropsNode = mapNode.get("additionalProperties");
                            if (addPropsNode.has("$ref")) {
                                String valRef = extractRawRefName(addPropsNode.get("$ref").asText());
                                if (valRef != null) {
                                    JsonNode valDef = findDefinition(defs, valRef);
                                    if (valDef != null) {
                                        Schema<?> valSchema = convertJsonNodeToSchema(valDef, valueTypeName);
                                        if (valSchema != null) {
                                            generatedSchemas.put(valueTypeName, valSchema);
                                        }
                                    }
                                }
                            } else {
                                Schema<?> valSchema = convertJsonNodeToSchema(addPropsNode, valueTypeName);
                                if (valSchema != null) {
                                    generatedSchemas.put(valueTypeName, valSchema);
                                }
                            }
                        } else {
                            // Look for the value type in defs by name
                            JsonNode valDef = findDefinition(defs, valueTypeName);
                            if (valDef != null) {
                                Schema<?> valSchema = convertJsonNodeToSchema(valDef, valueTypeName);
                                if (valSchema != null) {
                                    generatedSchemas.put(valueTypeName, valSchema);
                                }
                            }
                        }
                    }
                }
            });
        }

        // Convert the main schema
        return convertJsonNodeToSchema(rootSchema, rootTypeName);
    }

    private JsonNode getDefinitions(JsonNode jsonSchema) {
        JsonNode defs = jsonSchema.get("$defs");
        return defs != null ? defs : jsonSchema.get("definitions");
    }

    private String stripNumericSuffix(String schemaName) {
        int dashIndex = schemaName.lastIndexOf('-');
        if (dashIndex <= 0 || dashIndex == schemaName.length() - 1) {
            return null;
        }
        for (int i = dashIndex + 1; i < schemaName.length(); i++) {
            if (!Character.isDigit(schemaName.charAt(i))) {
                return null;
            }
        }
        return schemaName.substring(0, dashIndex);
    }

    private JsonNode resolveRootSchemaNode(JsonNode jsonSchema, JsonNode defs, Set<String> rootAliases) {
        JsonNode current = jsonSchema;
        Set<String> visitedRefs = new LinkedHashSet<>();

        while (current != null && current.has("$ref")) {
            String refName = extractRawRefName(current.get("$ref").asText());
            if (refName == null || !visitedRefs.add(refName)) {
                break;
            }

            JsonNode referenced = findDefinition(defs, refName);
            if (referenced == null) {
                break;
            }

            rootAliases.add(sanitizeSchemaName(refName));
            current = referenced;
        }

        return current != null ? current : jsonSchema;
    }

    private JsonNode findDefinition(JsonNode defs, String refName) {
        if (defs == null || !defs.isObject() || refName == null) {
            return null;
        }
        if (defs.has(refName)) {
            return defs.get(refName);
        }

        String sanitizedRefName = sanitizeSchemaName(refName);
        Iterator<Map.Entry<String, JsonNode>> fields = defs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String sanitizedKey = sanitizeSchemaName(entry.getKey());
            if (sanitizedRefName.equals(sanitizedKey)) {
                return entry.getValue();
            }
            // Also match by suffix (for inner classes like HealthStatus.ComponentHealth → ComponentHealth)
            if (sanitizedKey.endsWith("_" + sanitizedRefName) || sanitizedKey.endsWith("." + refName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String extractRawRefName(String ref) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }
        int lastSlash = ref.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < ref.length() - 1) {
            return ref.substring(lastSlash + 1);
        }
        return null;
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
                // For Map-type refs, inline as object with additionalProperties
                if (refName.startsWith("Map_")) {
                    String valueTypeName = extractMapValueType(refName);
                    ObjectSchema mapSchema = new ObjectSchema();
                    if (valueTypeName != null && !valueTypeName.isEmpty()) {
                        mapSchema.setAdditionalProperties(createReference(valueTypeName));
                    } else {
                        mapSchema.setAdditionalProperties(new ObjectSchema());
                    }
                    return mapSchema;
                }
                // Suppress refs to internal framework types — treat as opaque object
                if (isInternalSchemaName(refName)) {
                    logger.accept("Suppressing $ref to internal framework type: " + refName);
                    return new ObjectSchema();
                }
                return createReference(refName);
            }
            // If we can't extract a proper ref name, return an object schema
            return new ObjectSchema();
        }

        Schema<?> composedSchema = tryComposedSchema(node, currentTypeName);
        if (composedSchema != null) {
            return applyCommonProperties(composedSchema, node);
        }

        String type = node.has("type") ? node.get("type").asText() : null;
        boolean nullableFromTypeArray = false;

        // Handle arrays with multiple types (JSON Schema 2020-12 style)
        if (node.has("type") && node.get("type").isArray()) {
            ArrayNode typeArray = (ArrayNode) node.get("type");
            type = null;
            // Find the non-null type and detect nullable
            for (JsonNode typeNode : typeArray) {
                String t = typeNode.asText();
                if ("null".equals(t)) {
                    nullableFromTypeArray = true;
                } else if (type == null) {
                    type = t;
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
            String format = node.has("format") ? node.get("format").asText() : null;
            Schema<?> stringSchema;
            if ("uuid".equals(format)) {
                stringSchema = new UUIDSchema();
            } else if ("date".equals(format)) {
                stringSchema = new DateSchema();
            } else if ("date-time".equals(format)) {
                stringSchema = new DateTimeSchema();
            } else if ("binary".equals(format)) {
                stringSchema = new BinarySchema();
            } else {
                stringSchema = new StringSchema();
            }
            if (node.has("format")) {
                stringSchema.setFormat(format);
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
                ((Schema) stringSchema).setEnum(enumValues);
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
            if (node.has("exclusiveMinimum") && node.get("exclusiveMinimum").isNumber()) {
                integerSchema.setExclusiveMinimumValue(node.get("exclusiveMinimum").decimalValue());
            }
            if (node.has("exclusiveMaximum") && node.get("exclusiveMaximum").isNumber()) {
                integerSchema.setExclusiveMaximumValue(node.get("exclusiveMaximum").decimalValue());
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
            if (node.has("exclusiveMinimum") && node.get("exclusiveMinimum").isNumber()) {
                numberSchema.setExclusiveMinimumValue(node.get("exclusiveMinimum").decimalValue());
            }
            if (node.has("exclusiveMaximum") && node.get("exclusiveMaximum").isNumber()) {
                numberSchema.setExclusiveMaximumValue(node.get("exclusiveMaximum").decimalValue());
            }
            schema = numberSchema;
        } else if ("boolean".equals(type)) {
            schema = new BooleanSchema();
        } else if ("null".equals(type)) {
            schema = new Schema<>().type("null");
        } else {
            // Default to object schema
            schema = new ObjectSchema();
        }

        if (nullableFromTypeArray) {
            schema.addType("null");
        }

        return applyCommonProperties(schema, node);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Schema<?> tryComposedSchema(JsonNode node, String currentTypeName) {
        String compositionKeyword = null;
        JsonNode composedNode = null;
        if (node.has("anyOf")) {
            compositionKeyword = "anyOf";
            composedNode = node.get("anyOf");
        } else if (node.has("oneOf")) {
            compositionKeyword = "oneOf";
            composedNode = node.get("oneOf");
        } else if (node.has("allOf")) {
            compositionKeyword = "allOf";
            composedNode = node.get("allOf");
        }

        if (composedNode == null || !composedNode.isArray()) {
            return null;
        }

        List<JsonNode> nonNullEntries = new ArrayList<>();
        for (JsonNode entry : composedNode) {
            if (!isNullSchema(entry)) {
                nonNullEntries.add(entry);
            }
        }

        if (nonNullEntries.size() == 1 && composedNode.size() == 2
                && ("anyOf".equals(compositionKeyword) || "oneOf".equals(compositionKeyword))) {
            Schema<?> collapsed = convertJsonNodeToSchema(nonNullEntries.get(0), currentTypeName);
            if (collapsed != null) {
                if (collapsed.get$ref() != null) {
                    // For $ref schemas, keep as oneOf with null type (OAS 3.1 nullable $ref)
                    ComposedSchema nullableRef = new ComposedSchema();
                    Schema<?> nullPart = new Schema<>();
                    nullPart.addType("null");
                    nullableRef.setOneOf(List.of(collapsed, nullPart));
                    return nullableRef;
                }
                // For typed schemas, add "null" to types
                collapsed.addType("null");
            }
            return collapsed;
        }

        ComposedSchema schema = new ComposedSchema();
        List<Schema> entries = new ArrayList<>();
        for (JsonNode entry : composedNode) {
            entries.add((Schema) convertJsonNodeToSchema(entry, currentTypeName));
        }

        // Deduplicate: if all entries resolve to the same $ref, collapse to a single ref
        if (entries.size() > 1 && entries.stream().allMatch(e -> e.get$ref() != null)) {
            String firstRef = entries.get(0).get$ref();
            if (entries.stream().allMatch(e -> firstRef.equals(e.get$ref()))) {
                return entries.get(0);
            }
        }

        if ("anyOf".equals(compositionKeyword)) {
            schema.setAnyOf(entries);
        } else if ("oneOf".equals(compositionKeyword)) {
            schema.setOneOf(entries);
        } else {
            schema.setAllOf(entries);
        }
        return schema;
    }

    private boolean isNullSchema(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }
        JsonNode typeNode = node.get("type");
        if (typeNode == null) {
            return false;
        }
        if (typeNode.isTextual()) {
            return "null".equals(typeNode.asText());
        }
        if (typeNode.isArray() && typeNode.size() == 1) {
            return "null".equals(typeNode.get(0).asText());
        }
        return false;
    }

    private Schema<?> applyCommonProperties(Schema<?> schema, JsonNode node) {
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
            schema.addType("null");
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

        if (refName == null) {
            return null;
        }

        // Def names are already canonical, globally-unambiguous internal names — no
        // collision disambiguation needed. Just normalise (strip -nullable, follow aliases).
        return canonicalSchemaName(refName);
    }

    private String canonicalSchemaName(String name) {
        String sanitizedName = sanitizeSchemaName(name);
        // Strip "-nullable" suffix — these are victools wrappers for Optional fields
        if (sanitizedName.endsWith("-nullable")) {
            sanitizedName = sanitizedName.substring(0, sanitizedName.length() - "-nullable".length());
        }
        return schemaNameAliases.getOrDefault(sanitizedName, sanitizedName);
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

    /**
     * Returns true if this schema name refers to a framework-internal or Akka SDK
     * type that should never appear as a component in a user-facing OpenAPI spec.
     *
     * <p>Examples that should be suppressed:</p>
     * <ul>
     *   <li>{@code Source_Object_NotUsed_} — Akka Streams Source materialized value</li>
     *   <li>{@code NotUsed} — Akka's scala.runtime.BoxedUnit equivalent</li>
     *   <li>{@code Done} — Akka's akka.Done</li>
     * </ul>
     */
    private boolean isInternalSchemaName(String sanitizedName) {
        if (sanitizedName == null) return false;
        // Akka Streams Source with any type params, e.g. Source_Object_NotUsed_
        // Also match the raw class simple name "Source" to catch it before specialisation
        if (sanitizedName.equals("Source") || sanitizedName.startsWith("Source_")) return true;
        // Akka internal marker types
        if (sanitizedName.equals("NotUsed")) return true;
        if (sanitizedName.equals("Done")) return true;
        // scala.runtime / scala stdlib leaking through
        if (sanitizedName.startsWith("scala_")) return true;
        if (sanitizedName.startsWith("akka_")) return true;
        return false;
    }

    /**
     * Generates a specialized type name for parameterized types.
     * E.g., PagedResponse<Customer> becomes "PagedResponseCustomer".
     */
    private String getSpecializedTypeName(Class<?> rawClass, java.lang.reflect.ParameterizedType parameterizedType) {
        StringBuilder name = new StringBuilder(getTypeName(rawClass));
        for (Type arg : parameterizedType.getActualTypeArguments()) {
            Class<?> argClass = getRawClass(arg);
            if (argClass != null) {
                name.append(getTypeName(argClass));
            }
        }
        return name.toString();
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
        return resolveSchemaName(type);
    }

    /**
     * Resolves the canonical internal schema name for a class: its enclosing-chain
     * qualified name (see {@link #internalNameForClass}). Always unambiguous; collapsed
     * to a minimal display name in {@link #getGeneratedSchemas}.
     *
     * @param type the class to resolve a schema name for
     * @return the canonical internal schema name
     */
    private String resolveSchemaName(Class<?> type) {
        String cached = resolvedSchemaNames.get(type);
        if (cached != null) {
            return cached;
        }
        String name = internalNameForClass(type);
        resolvedSchemaNames.put(type, name);
        classRegistry.putIfAbsent(name, type);
        return name;
    }

    /**
     * Returns all schemas generated so far, with post-processing to remove
     * duplicate "-nullable" wrapper schemas.
     *
     * <p>These schemas should be added to the OpenAPI components/schemas section.</p>
     *
     * @return map of schema names to schemas
     */
    public Map<String, Schema<?>> getGeneratedSchemas() {
        // Ensure required fields are correctly set on all generated component schemas.
        // This catches types that were stored from victools $defs processing without being
        // individually enriched (e.g. nested records, inner static classes, propagated types).
        enrichAllGeneratedSchemas();

        // Remove "-nullable" entries, numeric-suffixed duplicates, and Map_ entries
        Map<String, Schema<?>> result = new LinkedHashMap<>(generatedSchemas);
        // Build a map of removed name → canonical replacement name for ref rewriting
        Map<String, String> replacements = new LinkedHashMap<>();
        Map<String, String> componentRenames = new LinkedHashMap<>();
        List<String> toRemove = new ArrayList<>();
        Map<String, Integer> numericSuffixCounts = new LinkedHashMap<>();

        for (String name : result.keySet()) {
            String baseName = stripNumericSuffix(name);
            if (baseName != null) {
                numericSuffixCounts.merge(baseName, 1, Integer::sum);
            }
        }

        for (String name : result.keySet()) {
            if (name.endsWith("-nullable")) {
                String baseName = name.substring(0, name.length() - "-nullable".length());
                if (result.containsKey(baseName)) {
                    toRemove.add(name);
                    replacements.put(name, baseName);
                }
            } else if (name.startsWith("Map_")) {
                toRemove.add(name);
                // Map_ entries are inlined, no canonical replacement needed
            } else if (isInternalSchemaName(name)) {
                toRemove.add(name);
                // Internal framework types are silently dropped
            } else {
                String baseName = stripNumericSuffix(name);
                if (baseName != null) {
                    if (result.containsKey(baseName)) {
                        toRemove.add(name);
                        replacements.put(name, baseName);
                    } else if (classRegistry.containsKey(baseName)
                            && numericSuffixCounts.getOrDefault(baseName, 0) == 1) {
                        componentRenames.put(name, baseName);
                        replacements.put(name, baseName);
                    }
                }
            }
        }

        toRemove.forEach(result::remove);
        if (!componentRenames.isEmpty()) {
            Map<String, Schema<?>> renamed = new LinkedHashMap<>();
            result.forEach((name, schema) ->
                renamed.put(componentRenames.getOrDefault(name, name), schema));
            result.clear();
            result.putAll(renamed);
        }

        // Also include all schemaNameAliases entries so that refs to numbered names that were
        // never added to generatedSchemas (skipped by dedup) still get rewritten correctly.
        schemaNameAliases.forEach((aliasName, canonicalName) -> {
            if (!result.containsKey(aliasName)) {
                // Resolve transitively: if canonicalName is itself aliased
                String resolved = canonicalName;
                while (schemaNameAliases.containsKey(resolved)) {
                    resolved = schemaNameAliases.get(resolved);
                }
                replacements.put(aliasName, resolved);
            }
        });

        // Rewrite $refs in all remaining schemas to replace removed names with canonical ones
        if (!replacements.isEmpty()) {
            for (Schema<?> schema : result.values()) {
                rewriteRefs(schema, replacements);
            }
        }

        // Final pass: collapse each qualified internal name to its simple name when that
        // simple name is globally unique among components. Ambiguous ones stay qualified.
        Map<String, String> collapseReplacements = collapseToMinimalNames(result);

        // Redirect any dangling victools "-N" duplicate refs to their surviving component.
        repairDanglingNumberedRefs(result, collapseReplacements);

        // Collapse redundant anyOf/oneOf unions left by victools' inline rendering of
        // polymorphic fields back to a single $ref (restoring the discriminator link
        // for fields typed as a discriminated parent).
        collapseRedundantUnions(result);

        // Record the cumulative internal-name -> final-name mapping so callers can normalise
        // any $refs they built from internal names (e.g. path-level response refs).
        recordSchemaNameReplacements(replacements, collapseReplacements);

        // Final safety pass: strip any remaining $refs that point to internal framework types
        // (may have been emitted before interception was possible)
        for (Schema<?> schema : result.values()) {
            stripInternalRefs(schema);
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Composes the dedup and collapse replacement maps into a single, transitively-resolved
     * internal-name → final-display-name map and stores it for {@link #getSchemaNameReplacements()}.
     */
    private void recordSchemaNameReplacements(
            Map<String, String> dedupReplacements, Map<String, String> collapseReplacements) {
        Map<String, String> combined = new LinkedHashMap<>();
        combined.putAll(dedupReplacements);
        combined.putAll(collapseReplacements);

        lastSchemaNameReplacements.clear();
        for (String key : combined.keySet()) {
            String resolved = key;
            Set<String> seen = new LinkedHashSet<>();
            while (combined.containsKey(resolved) && seen.add(resolved)) {
                resolved = combined.get(resolved);
            }
            if (!resolved.equals(key)) {
                lastSchemaNameReplacements.put(key, resolved);
            }
        }
    }

    /**
     * Returns the internal-name → final-component-name replacements applied by the most
     * recent {@link #getGeneratedSchemas()} call. Callers that constructed {@code $ref}s from
     * internal (enclosing-chain qualified) names should apply these to keep refs resolvable.
     *
     * @return an unmodifiable view of the current replacement map
     */
    public Map<String, String> getSchemaNameReplacements() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(lastSchemaNameReplacements));
    }

    /**
     * Recursively rewrites $ref values in a schema using the given replacement map.
     */
    private void rewriteRefs(Schema<?> schema, Map<String, String> replacements) {
        if (schema == null) return;

        // Fix direct $ref
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String suffix = "#/components/schemas/";
            if (ref.startsWith(suffix)) {
                String name = ref.substring(suffix.length());
                String replacement = replacements.get(name);
                if (replacement != null) {
                    schema.set$ref(suffix + replacement);
                }
            }
        }

        // Recurse into properties
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(prop -> rewriteRefs((Schema<?>) prop, replacements));
        }

        // Recurse into additionalProperties
        Object addProps = schema.getAdditionalProperties();
        if (addProps instanceof Schema) {
            rewriteRefs((Schema<?>) addProps, replacements);
        }

        // Recurse into items (array)
        if (schema.getItems() != null) {
            rewriteRefs(schema.getItems(), replacements);
        }

        // Recurse into oneOf / anyOf / allOf (these fields are on base Schema class)
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(s -> rewriteRefs(s, replacements));
        }
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(s -> rewriteRefs(s, replacements));
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(s -> rewriteRefs(s, replacements));
        }

        // Rewrite a title that mirrors a renamed schema name (used by polymorphic subtypes).
        if (schema.getTitle() != null) {
            String titleReplacement = replacements.get(schema.getTitle());
            if (titleReplacement != null) {
                schema.setTitle(titleReplacement);
            }
        }

        // Rewrite discriminator mapping targets (values are #/components/schemas/<name>)
        if (schema.getDiscriminator() != null && schema.getDiscriminator().getMapping() != null) {
            String prefix = "#/components/schemas/";
            Map<String, String> mapping = schema.getDiscriminator().getMapping();
            for (Map.Entry<String, String> e : mapping.entrySet()) {
                String val = e.getValue();
                if (val != null && val.startsWith(prefix)) {
                    String replacement = replacements.get(val.substring(prefix.length()));
                    if (replacement != null) {
                        e.setValue(prefix + replacement);
                    }
                }
            }
        }
    }

    /**
     * Collapses qualified internal schema names to their simple names where unambiguous.
     *
     * <p>Every component is keyed by an unambiguous, enclosing-chain qualified internal
     * name. For each class-backed component whose simple name is globally unique among all
     * components, the key is renamed to that simple name. Components whose simple name is
     * shared by more than one class keep their qualified name. All {@code $ref}s and
     * discriminator mappings are rewritten to follow the renames.</p>
     */
    private Map<String, String> collapseToMinimalNames(Map<String, Schema<?>> schemas) {
        // Count how many class-backed component keys map to each simple name.
        Map<String, Integer> simpleCounts = new LinkedHashMap<>();
        for (String key : schemas.keySet()) {
            Class<?> cls = classRegistry.get(key);
            if (cls == null) continue;
            simpleCounts.merge(sanitizeSchemaName(cls.getSimpleName()), 1, Integer::sum);
        }

        // Plan renames: qualified key -> simple name when that simple name is unique.
        Map<String, String> replacements = new LinkedHashMap<>();
        for (String key : schemas.keySet()) {
            Class<?> cls = classRegistry.get(key);
            if (cls == null) continue;
            String simple = sanitizeSchemaName(cls.getSimpleName());
            if (simple.equals(key)) continue;
            if (simpleCounts.getOrDefault(simple, 0) == 1 && !schemas.containsKey(simple)) {
                replacements.put(key, simple);
            }
        }

        if (replacements.isEmpty()) return replacements;

        // Apply key renames preserving order.
        Map<String, Schema<?>> renamed = new LinkedHashMap<>();
        schemas.forEach((k, v) -> renamed.put(replacements.getOrDefault(k, k), v));
        schemas.clear();
        schemas.putAll(renamed);

        // Rewrite all $refs and discriminator mappings to the new names.
        for (Schema<?> schema : schemas.values()) {
            rewriteRefs(schema, replacements);
        }
        return replacements;
    }

    /**
     * Repairs dangling {@code $ref}s left by victools' duplicate-name suffixing.
     *
     * <p>When the same nested type is materialised both as a standalone definition and
     * inline (e.g. a sealed-interface field rendered as a {@code oneOf}), victools emits a
     * suffixed alias such as {@code FooBar-1} that is referenced but never stored as a
     * component. This redirects any such dangling reference to the surviving component,
     * following the simple-name collapse performed in {@link #collapseToMinimalNames}.</p>
     */
    private void repairDanglingNumberedRefs(Map<String, Schema<?>> schemas, Map<String, String> collapseReplacements) {
        Set<String> present = schemas.keySet();
        Set<String> targets = new LinkedHashSet<>();
        for (Schema<?> schema : schemas.values()) {
            collectRefTargets(schema, targets);
        }

        Map<String, String> fix = new LinkedHashMap<>();
        for (String target : targets) {
            if (present.contains(target)) continue;
            String base = stripNumericSuffix(target);
            if (base == null) continue;
            String resolved = collapseReplacements.getOrDefault(base, base);
            if (present.contains(resolved)) {
                fix.put(target, resolved);
            }
        }

        if (!fix.isEmpty()) {
            for (Schema<?> schema : schemas.values()) {
                rewriteRefs(schema, fix);
            }
        }
    }

    /**
     * Collapses redundant {@code anyOf}/{@code oneOf} unions produced when victools renders
     * a polymorphic field inline instead of referencing the parent component:
     *
     * <ul>
     *   <li>a union whose branches all reference the same component (e.g.
     *       {@code anyOf: [X, X, X]} after numbered-ref repair) becomes a single
     *       {@code $ref} to that component;</li>
     *   <li>a union whose branch set exactly matches the {@code oneOf} subtype set of a
     *       discriminated parent component becomes a {@code $ref} to that parent, restoring
     *       the discriminator link for consumers and code generators.</li>
     * </ul>
     */
    private void collapseRedundantUnions(Map<String, Schema<?>> schemas) {
        // Map each discriminated parent's exact subtype-ref set to the parent's name.
        Map<Set<String>, String> parentBySubtypes = new LinkedHashMap<>();
        for (Map.Entry<String, Schema<?>> entry : schemas.entrySet()) {
            Schema<?> schema = entry.getValue();
            if (schema.getDiscriminator() != null && schema.getOneOf() != null) {
                Set<String> subtypes = pureRefTargets(schema.getOneOf());
                if (subtypes != null && subtypes.size() > 1) {
                    parentBySubtypes.putIfAbsent(subtypes, entry.getKey());
                }
            }
        }
        for (Map.Entry<String, Schema<?>> entry : schemas.entrySet()) {
            collapseRedundantUnionsIn(entry.getValue(), entry.getKey(), true, parentBySubtypes);
        }
    }

    private void collapseRedundantUnionsIn(Schema<?> schema, String componentName,
            boolean isComponentRoot, Map<Set<String>, String> parentBySubtypes) {
        if (schema == null) {
            return;
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(p ->
                collapseRedundantUnionsIn((Schema<?>) p, componentName, false, parentBySubtypes));
        }
        Object addProps = schema.getAdditionalProperties();
        if (addProps instanceof Schema) {
            collapseRedundantUnionsIn((Schema<?>) addProps, componentName, false, parentBySubtypes);
        }
        if (schema.getItems() != null) {
            collapseRedundantUnionsIn(schema.getItems(), componentName, false, parentBySubtypes);
        }
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(s ->
                collapseRedundantUnionsIn(s, componentName, false, parentBySubtypes));
        }
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(s ->
                collapseRedundantUnionsIn(s, componentName, false, parentBySubtypes));
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(s ->
                collapseRedundantUnionsIn(s, componentName, false, parentBySubtypes));
        }

        // Only a node that is purely a union is a candidate; discriminated parents and
        // nodes carrying their own structure (type/properties/items/enum) are left intact.
        if (schema.get$ref() != null || schema.getDiscriminator() != null
                || schema.getProperties() != null || schema.getItems() != null
                || schema.getAdditionalProperties() != null || schema.getEnum() != null
                || schema.getType() != null || schema.getTypes() != null
                || schema.getAllOf() != null) {
            return;
        }
        boolean isAnyOf = schema.getAnyOf() != null;
        List<Schema> union = isAnyOf ? schema.getAnyOf() : schema.getOneOf();
        if (union == null || (isAnyOf && schema.getOneOf() != null)) {
            return;
        }
        Set<String> targets = pureRefTargets(union);
        if (targets == null) {
            return;
        }
        String replacement = targets.size() == 1
            ? targets.iterator().next()
            : parentBySubtypes.get(targets);
        if (replacement == null) {
            return;
        }
        // Never turn a component's own root into a self-reference.
        if (isComponentRoot && replacement.equals(componentName)) {
            return;
        }
        if (isAnyOf) {
            schema.setAnyOf(null);
        } else {
            schema.setOneOf(null);
        }
        schema.set$ref("#/components/schemas/" + replacement);
    }

    /**
     * Returns the distinct component names referenced by the given union branches, or null
     * if any branch is not a pure {@code $ref} to a component schema.
     */
    private Set<String> pureRefTargets(List<Schema> branches) {
        String prefix = "#/components/schemas/";
        Set<String> targets = new LinkedHashSet<>();
        for (Schema<?> branch : branches) {
            if (branch == null || branch.get$ref() == null || !branch.get$ref().startsWith(prefix)) {
                return null;
            }
            targets.add(branch.get$ref().substring(prefix.length()));
        }
        return targets.isEmpty() ? null : targets;
    }

    private void collectRefTargets(Schema<?> schema, Set<String> targets) {
        if (schema == null) return;
        String prefix = "#/components/schemas/";
        if (schema.get$ref() != null && schema.get$ref().startsWith(prefix)) {
            targets.add(schema.get$ref().substring(prefix.length()));
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(p -> collectRefTargets((Schema<?>) p, targets));
        }
        Object addProps = schema.getAdditionalProperties();
        if (addProps instanceof Schema) {
            collectRefTargets((Schema<?>) addProps, targets);
        }
        if (schema.getItems() != null) {
            collectRefTargets(schema.getItems(), targets);
        }
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(s -> collectRefTargets(s, targets));
        }
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(s -> collectRefTargets(s, targets));
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(s -> collectRefTargets(s, targets));
        }
        if (schema.getDiscriminator() != null && schema.getDiscriminator().getMapping() != null) {
            for (String val : schema.getDiscriminator().getMapping().values()) {
                if (val != null && val.startsWith(prefix)) {
                    targets.add(val.substring(prefix.length()));
                }
            }
        }
    }

    /**
     * replacing them with an inline empty object schema to avoid unresolved-ref errors.
     */
    @SuppressWarnings("unchecked")
    private void stripInternalRefs(Schema<?> schema) {
        if (schema == null) return;

        // Fix direct $ref
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String suffix = "#/components/schemas/";
            if (ref.startsWith(suffix)) {
                String name = ref.substring(suffix.length());
                if (isInternalSchemaName(name)) {
                    schema.set$ref(null);
                    schema.setType("object");
                }
            }
        }

        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(p -> stripInternalRefs((Schema<?>) p));
        }
        Object addProps = schema.getAdditionalProperties();
        if (addProps instanceof Schema) {
            stripInternalRefs((Schema<?>) addProps);
        }
        if (schema.getItems() != null) {
            stripInternalRefs(schema.getItems());
        }
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(this::stripInternalRefs);
        }
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(this::stripInternalRefs);
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(this::stripInternalRefs);
        }
    }

    /**
     * Clears all generated schemas.
     */
    public void clearSchemas() {
        generatedSchemas.clear();
        schemaNameAliases.clear();
        classRegistry.clear();
        resolvedSchemaNames.clear();
        lastSchemaNameReplacements.clear();
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
