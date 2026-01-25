package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.core.config.PluginConfiguration;
import com.github.osodevops.akka.openapi.core.config.ServerConfig;
import com.github.osodevops.akka.openapi.core.model.*;
import com.github.osodevops.akka.openapi.core.model.OperationMetadata.HttpMethod;
import com.github.osodevops.akka.openapi.core.model.ParameterMetadata.ParameterLocation;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Builds a complete OpenAPI 3.1 specification from endpoint metadata.
 *
 * <p>This class takes endpoint metadata extracted by {@link AkkaAnnotationExtractor}
 * and constructs a complete OpenAPI document including:</p>
 * <ul>
 *   <li>Info section with title, version, description</li>
 *   <li>Servers section with configured server URLs</li>
 *   <li>Paths with all operations</li>
 *   <li>Tags derived from endpoint class names</li>
 *   <li>Components/schemas section with all referenced types</li>
 * </ul>
 */
public class OpenAPIModelBuilder {

    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String OPENAPI_VERSION = "3.1.0";

    private final PluginConfiguration config;
    private final SchemaGenerator schemaGenerator;
    private final Consumer<String> logger;
    private final Set<String> usedOperationIds;

    /**
     * Creates a new OpenAPIModelBuilder with the given configuration.
     *
     * @param config the plugin configuration
     */
    public OpenAPIModelBuilder(PluginConfiguration config) {
        this(config, msg -> {});
    }

    /**
     * Creates a new OpenAPIModelBuilder with the given configuration and logger.
     *
     * @param config the plugin configuration
     * @param logger consumer for log messages
     */
    public OpenAPIModelBuilder(PluginConfiguration config, Consumer<String> logger) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
        this.schemaGenerator = new SchemaGenerator(logger);
        this.usedOperationIds = new HashSet<>();
    }

    /**
     * Builds a complete OpenAPI specification from the given endpoint metadata.
     *
     * @param endpoints the list of endpoint metadata to include
     * @return the complete OpenAPI specification
     */
    public OpenAPI build(List<EndpointMetadata> endpoints) {
        Objects.requireNonNull(endpoints, "endpoints must not be null");

        logger.accept("Building OpenAPI specification from " + endpoints.size() + " endpoints");

        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi(OPENAPI_VERSION);

        // Build Info section
        openAPI.setInfo(buildInfo());

        // Build Servers section
        if (!config.getServers().isEmpty()) {
            openAPI.setServers(buildServers());
        }

        // Build Paths and collect tags
        Paths paths = new Paths();
        Set<String> allTags = new LinkedHashSet<>();

        for (EndpointMetadata endpoint : endpoints) {
            allTags.addAll(endpoint.getTags());
            addEndpointPaths(paths, endpoint);
        }

        openAPI.setPaths(paths);

        // Build Tags section
        if (!allTags.isEmpty()) {
            openAPI.setTags(buildTags(allTags));
        }

        // Build Components section with schemas
        Map<String, Schema<?>> schemas = schemaGenerator.getGeneratedSchemas();
        if (!schemas.isEmpty()) {
            Components components = openAPI.getComponents();
            if (components == null) {
                components = new Components();
                openAPI.setComponents(components);
            }
            components.setSchemas(new LinkedHashMap<>(schemas));
        }

        logger.accept("OpenAPI specification built successfully with " + paths.size() + " paths");

        return openAPI;
    }

    private Info buildInfo() {
        Info info = new Info();
        info.setTitle(config.getApiTitle());
        info.setVersion(config.getApiVersion());

        if (config.getApiDescription() != null && !config.getApiDescription().isEmpty()) {
            info.setDescription(config.getApiDescription());
        }

        if (config.getContactName() != null || config.getContactEmail() != null ||
            config.getContactUrl() != null) {
            Contact contact = new Contact();
            contact.setName(config.getContactName());
            contact.setEmail(config.getContactEmail());
            contact.setUrl(config.getContactUrl());
            info.setContact(contact);
        }

        if (config.getLicenseName() != null) {
            License license = new License();
            license.setName(config.getLicenseName());
            license.setUrl(config.getLicenseUrl());
            info.setLicense(license);
        }

        if (config.getTermsOfService() != null) {
            info.setTermsOfService(config.getTermsOfService());
        }

        return info;
    }

    private List<Server> buildServers() {
        return config.getServers().stream()
            .map(this::buildServer)
            .collect(Collectors.toList());
    }

    private Server buildServer(ServerConfig serverConfig) {
        Server server = new Server();
        server.setUrl(serverConfig.getUrl());
        server.setDescription(serverConfig.getDescription());
        return server;
    }

    private List<Tag> buildTags(Set<String> tagNames) {
        return tagNames.stream()
            .sorted()
            .map(name -> {
                Tag tag = new Tag();
                tag.setName(name);
                return tag;
            })
            .collect(Collectors.toList());
    }

    private void addEndpointPaths(Paths paths, EndpointMetadata endpoint) {
        for (OperationMetadata operation : endpoint.getOperations()) {
            String fullPath = normalizePath(endpoint.getBasePath(), operation.getPath());

            PathItem pathItem = paths.get(fullPath);
            if (pathItem == null) {
                pathItem = new PathItem();
                paths.put(fullPath, pathItem);
            }

            Operation openApiOp = buildOperation(operation, endpoint);
            setOperationOnPathItem(pathItem, operation.getHttpMethod(), openApiOp);
        }
    }

    private Operation buildOperation(OperationMetadata operationMeta, EndpointMetadata endpoint) {
        Operation operation = new Operation();

        // Set operation ID
        String operationId = generateOperationId(operationMeta, endpoint);
        operation.setOperationId(operationId);

        // Set summary and description
        if (operationMeta.getSummary() != null && !operationMeta.getSummary().isEmpty()) {
            operation.setSummary(operationMeta.getSummary());
        }
        if (operationMeta.getDescription() != null && !operationMeta.getDescription().isEmpty()) {
            operation.setDescription(operationMeta.getDescription());
        }

        // Set tags
        if (!endpoint.getTags().isEmpty()) {
            operation.setTags(new ArrayList<>(endpoint.getTags()));
        }

        // Add parameters
        List<Parameter> parameters = buildParameters(operationMeta);
        if (!parameters.isEmpty()) {
            operation.setParameters(parameters);
        }

        // Add request body if present
        if (operationMeta.getRequestBody() != null) {
            operation.setRequestBody(buildRequestBody(operationMeta.getRequestBody()));
        }

        // Add responses
        operation.setResponses(buildResponses(operationMeta));

        // Set deprecated flag
        if (operationMeta.isDeprecated()) {
            operation.setDeprecated(true);
        }

        return operation;
    }

    private String generateOperationId(OperationMetadata operation, EndpointMetadata endpoint) {
        String baseId;

        if (operation.getOperationId() != null && !operation.getOperationId().isEmpty()) {
            baseId = operation.getOperationId();
        } else {
            // Generate from method name
            baseId = operation.getMethodName();
        }

        // Ensure uniqueness
        String operationId = baseId;
        int counter = 1;
        while (usedOperationIds.contains(operationId)) {
            operationId = baseId + "_" + counter++;
        }
        usedOperationIds.add(operationId);

        return operationId;
    }

    private List<Parameter> buildParameters(OperationMetadata operation) {
        return operation.getParameters().stream()
            .map(this::buildParameter)
            .collect(Collectors.toList());
    }

    private Parameter buildParameter(ParameterMetadata paramMeta) {
        Parameter parameter = new Parameter();
        parameter.setName(paramMeta.getName());
        parameter.setIn(mapParameterLocation(paramMeta.getLocation()));
        parameter.setRequired(paramMeta.isRequired());

        if (paramMeta.getDescription() != null && !paramMeta.getDescription().isEmpty()) {
            parameter.setDescription(paramMeta.getDescription());
        }

        // Generate schema for the parameter type
        Schema<?> schema = schemaGenerator.generateSchema(paramMeta.getJavaType());
        parameter.setSchema(schema);

        if (paramMeta.getDefaultValue() != null) {
            if (schema != null) {
                schema.setDefault(paramMeta.getDefaultValue());
            }
        }

        if (paramMeta.getExample() != null) {
            parameter.setExample(paramMeta.getExample());
        }

        return parameter;
    }

    private String mapParameterLocation(ParameterLocation location) {
        return switch (location) {
            case PATH -> "path";
            case QUERY -> "query";
            case HEADER -> "header";
            case COOKIE -> "cookie";
        };
    }

    private RequestBody buildRequestBody(RequestBodyMetadata requestBodyMeta) {
        RequestBody requestBody = new RequestBody();
        requestBody.setRequired(requestBodyMeta.isRequired());

        if (requestBodyMeta.getDescription() != null && !requestBodyMeta.getDescription().isEmpty()) {
            requestBody.setDescription(requestBodyMeta.getDescription());
        }

        // Generate schema for the request body type
        java.lang.reflect.Type javaType = requestBodyMeta.getJavaType();
        Schema<?> schema = schemaGenerator.generateSchema(javaType);

        // Use reference if the type was added to schemas
        String typeName = getTypeName(javaType);
        if (schemaGenerator.hasSchema(typeName)) {
            Schema<?> refSchema = new Schema<>();
            refSchema.set$ref("#/components/schemas/" + typeName);
            schema = refSchema;
        }

        Content content = new Content();
        String contentType = requestBodyMeta.getMediaType() != null ?
            requestBodyMeta.getMediaType() : DEFAULT_CONTENT_TYPE;

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        if (requestBodyMeta.getExample() != null) {
            mediaType.setExample(requestBodyMeta.getExample());
        }

        content.addMediaType(contentType, mediaType);
        requestBody.setContent(content);

        return requestBody;
    }

    private ApiResponses buildResponses(OperationMetadata operation) {
        ApiResponses responses = new ApiResponses();

        for (ResponseMetadata responseMeta : operation.getResponses().values()) {
            ApiResponse response = buildResponse(responseMeta);
            responses.addApiResponse(responseMeta.getStatusCode(), response);
        }

        // Ensure at least a default response
        if (responses.isEmpty()) {
            ApiResponse defaultResponse = new ApiResponse();
            defaultResponse.setDescription("Successful operation");
            responses.addApiResponse("200", defaultResponse);
        }

        return responses;
    }

    private ApiResponse buildResponse(ResponseMetadata responseMeta) {
        ApiResponse response = new ApiResponse();
        response.setDescription(responseMeta.getDescription());

        java.lang.reflect.Type responseType = responseMeta.getResponseType();
        if (responseMeta.hasBody()) {
            Schema<?> schema = schemaGenerator.generateSchema(responseType);

            // Use reference if the type was added to schemas
            String typeName = getTypeName(responseType);
            if (schemaGenerator.hasSchema(typeName)) {
                Schema<?> refSchema = new Schema<>();
                refSchema.set$ref("#/components/schemas/" + typeName);
                schema = refSchema;
            }

            Content content = new Content();
            String contentType = responseMeta.getMediaType() != null ?
                responseMeta.getMediaType() : DEFAULT_CONTENT_TYPE;

            MediaType mediaType = new MediaType();
            mediaType.setSchema(schema);

            // Add examples if available
            if (!responseMeta.getExamples().isEmpty()) {
                // Use first example as the main example
                responseMeta.getExamples().values().stream().findFirst()
                    .ifPresent(mediaType::setExample);
            }

            content.addMediaType(contentType, mediaType);
            response.setContent(content);
        }

        return response;
    }

    private void setOperationOnPathItem(PathItem pathItem, HttpMethod method, Operation operation) {
        switch (method) {
            case GET -> pathItem.setGet(operation);
            case POST -> pathItem.setPost(operation);
            case PUT -> pathItem.setPut(operation);
            case DELETE -> pathItem.setDelete(operation);
            case PATCH -> pathItem.setPatch(operation);
            case HEAD -> pathItem.setHead(operation);
            case OPTIONS -> pathItem.setOptions(operation);
        }
    }

    private String getTypeName(java.lang.reflect.Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getSimpleName();
        } else if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.Type rawType = ((java.lang.reflect.ParameterizedType) type).getRawType();
            if (rawType instanceof Class<?>) {
                return ((Class<?>) rawType).getSimpleName();
            }
        }
        return type.getTypeName();
    }

    /**
     * Normalizes and combines base path and operation path.
     *
     * @param basePath the endpoint base path
     * @param operationPath the operation path
     * @return the normalized full path
     */
    public static String normalizePath(String basePath, String operationPath) {
        String base = basePath == null ? "" : basePath.trim();
        String op = operationPath == null ? "" : operationPath.trim();

        // Remove trailing slashes from base
        while (base.endsWith("/") && base.length() > 1) {
            base = base.substring(0, base.length() - 1);
        }

        // Ensure op starts with / if not empty
        if (!op.isEmpty() && !op.startsWith("/")) {
            op = "/" + op;
        }

        // Combine paths
        String fullPath = base + op;

        // Ensure path starts with /
        if (!fullPath.startsWith("/")) {
            fullPath = "/" + fullPath;
        }

        // Remove duplicate slashes
        fullPath = fullPath.replaceAll("/+", "/");

        // Remove trailing slash unless it's the root
        if (fullPath.length() > 1 && fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }

        return fullPath;
    }

    /**
     * Clears all generated schemas and operation IDs.
     * Useful for generating multiple independent specifications.
     */
    public void reset() {
        schemaGenerator.clearSchemas();
        usedOperationIds.clear();
    }
}
