package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.annotations.*;
import com.github.osodevops.akka.openapi.core.model.*;
import com.github.osodevops.akka.openapi.core.model.OperationMetadata.HttpMethod;
import com.github.osodevops.akka.openapi.core.model.ParameterMetadata.ParameterLocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts metadata from Akka SDK HTTP endpoint annotations.
 *
 * <p>Processes classes annotated with @HttpEndpoint and methods annotated
 * with HTTP method annotations (@Get, @Post, etc.) to build metadata
 * for OpenAPI generation.</p>
 *
 * @since 1.0.0
 */
public class AkkaAnnotationExtractor {

    // Akka SDK annotation class names
    private static final String HTTP_ENDPOINT = "akka.javasdk.annotations.http.HttpEndpoint";
    private static final String GET = "akka.javasdk.annotations.http.Get";
    private static final String POST = "akka.javasdk.annotations.http.Post";
    private static final String PUT = "akka.javasdk.annotations.http.Put";
    private static final String DELETE = "akka.javasdk.annotations.http.Delete";
    private static final String PATCH = "akka.javasdk.annotations.http.Patch";
    private static final String HEAD = "akka.javasdk.annotations.http.Head";
    private static final String OPTIONS = "akka.javasdk.annotations.http.Options";

    private static final Map<String, HttpMethod> HTTP_METHOD_MAP = Map.of(
        GET, HttpMethod.GET,
        POST, HttpMethod.POST,
        PUT, HttpMethod.PUT,
        DELETE, HttpMethod.DELETE,
        PATCH, HttpMethod.PATCH,
        HEAD, HttpMethod.HEAD,
        OPTIONS, HttpMethod.OPTIONS
    );

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(
        String.class, Integer.class, int.class, Long.class, long.class,
        Double.class, double.class, Float.class, float.class,
        Boolean.class, boolean.class, Short.class, short.class,
        Byte.class, byte.class, Character.class, char.class
    );

    private final Consumer<String> logger;

    /**
     * Creates a new extractor with the specified logger.
     *
     * @param logger a logger for diagnostic messages (can be null)
     */
    public AkkaAnnotationExtractor(Consumer<String> logger) {
        this.logger = logger != null ? logger : msg -> {};
    }

    /**
     * Creates a new extractor without logging.
     */
    public AkkaAnnotationExtractor() {
        this(null);
    }

    /**
     * Extracts endpoint metadata from a class.
     *
     * @param endpointClass the class to extract metadata from
     * @return the extracted endpoint metadata
     * @throws IllegalArgumentException if the class is not annotated with @HttpEndpoint
     */
    public EndpointMetadata extractEndpoint(Class<?> endpointClass) {
        Objects.requireNonNull(endpointClass, "endpointClass must not be null");
        logger.accept("Extracting metadata from: " + endpointClass.getName());

        // Extract base path from @HttpEndpoint
        String basePath = extractBasePath(endpointClass);

        // Extract operations from methods
        List<OperationMetadata> operations = new ArrayList<>();
        for (Method method : endpointClass.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && hasHttpMethodAnnotation(method)) {
                OperationMetadata operation = extractOperation(method, basePath);
                if (operation != null) {
                    operations.add(operation);
                }
            }
        }

        // Extract tag from @OpenAPITag annotation or generate from class name
        EndpointMetadata.Builder builder = EndpointMetadata.builder()
            .className(endpointClass.getName())
            .basePath(basePath)
            .description("")
            .operations(operations);

        // Extract @OpenAPITag
        OpenAPITag tagAnnotation = endpointClass.getAnnotation(OpenAPITag.class);
        if (tagAnnotation != null) {
            builder.addTag(tagAnnotation.name());
            builder.addTagMetadata(new TagMetadata(
                tagAnnotation.name(),
                tagAnnotation.description(),
                tagAnnotation.externalDocsUrl(),
                tagAnnotation.externalDocsDescription()
            ));
            logger.accept("Found @OpenAPITag: " + tagAnnotation.name());
        } else {
            builder.addTag(generateTag(endpointClass));
        }

        // Extract @OpenAPIInfo
        OpenAPIInfo infoAnnotation = endpointClass.getAnnotation(OpenAPIInfo.class);
        if (infoAnnotation != null) {
            builder.infoMetadata(InfoMetadata.builder()
                .title(infoAnnotation.title())
                .version(infoAnnotation.version())
                .description(infoAnnotation.description())
                .termsOfService(infoAnnotation.termsOfService())
                .contactName(infoAnnotation.contactName())
                .contactEmail(infoAnnotation.contactEmail())
                .contactUrl(infoAnnotation.contactUrl())
                .licenseName(infoAnnotation.licenseName())
                .licenseUrl(infoAnnotation.licenseUrl())
                .build());
            logger.accept("Found @OpenAPIInfo on: " + endpointClass.getSimpleName());
        }

        // Extract @OpenAPIServer(s)
        OpenAPIServer[] serverAnnotations = endpointClass.getAnnotationsByType(OpenAPIServer.class);
        for (OpenAPIServer serverAnn : serverAnnotations) {
            builder.addServerMetadata(new ServerMetadata(serverAnn.url(), serverAnn.description()));
            logger.accept("Found @OpenAPIServer: " + serverAnn.url());
        }

        return builder.build();
    }

    /**
     * Extracts the base path from @HttpEndpoint annotation.
     */
    private String extractBasePath(Class<?> endpointClass) {
        Annotation httpEndpoint = findAnnotation(endpointClass, HTTP_ENDPOINT);
        if (httpEndpoint == null) {
            throw new IllegalArgumentException(
                "Class " + endpointClass.getName() + " is not annotated with @HttpEndpoint"
            );
        }
        return getAnnotationValue(httpEndpoint, "value", String.class, "/");
    }

    /**
     * Checks if a method has any HTTP method annotation.
     */
    private boolean hasHttpMethodAnnotation(Method method) {
        for (String annotationName : HTTP_METHOD_MAP.keySet()) {
            if (findAnnotation(method, annotationName) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts operation metadata from a method.
     */
    private OperationMetadata extractOperation(Method method, String basePath) {
        HttpMethod httpMethod = null;
        String operationPath = "";

        // Find the HTTP method annotation
        for (Map.Entry<String, HttpMethod> entry : HTTP_METHOD_MAP.entrySet()) {
            Annotation annotation = findAnnotation(method, entry.getKey());
            if (annotation != null) {
                httpMethod = entry.getValue();
                operationPath = getAnnotationValue(annotation, "value", String.class, "");
                break;
            }
        }

        if (httpMethod == null) {
            return null;
        }

        // Extract path parameters
        Set<String> pathParamNames = extractPathParameterNames(operationPath);
        if (operationPath.isEmpty()) {
            pathParamNames = extractPathParameterNames(basePath);
        }

        // Build parameters list
        List<ParameterMetadata> parameters = new ArrayList<>();
        RequestBodyMetadata requestBody = null;

        Parameter[] methodParams = method.getParameters();
        for (int i = 0; i < methodParams.length; i++) {
            Parameter param = methodParams[i];
            String paramName = param.getName();
            Type paramType = param.getParameterizedType();

            if (pathParamNames.contains(paramName)) {
                // Path parameter
                parameters.add(ParameterMetadata.builder()
                    .name(paramName)
                    .location(ParameterLocation.PATH)
                    .javaType(paramType)
                    .required(true)
                    .description("")
                    .build());
            } else if (isSimpleType(param.getType())) {
                // Query parameter (simple types not in path)
                parameters.add(ParameterMetadata.builder()
                    .name(paramName)
                    .location(ParameterLocation.QUERY)
                    .javaType(paramType)
                    .required(false)
                    .description("")
                    .build());
            } else if (isComplexType(param.getType()) && i == methodParams.length - 1) {
                // Request body (last complex type parameter)
                requestBody = RequestBodyMetadata.builder()
                    .javaType(paramType)
                    .required(true)
                    .description("")
                    .build();
            }
        }

        // Build responses: start with inferred, then merge @OpenAPIResponse annotations
        Map<String, ResponseMetadata> responses = inferResponses(method, httpMethod);
        mergeAnnotatedResponses(responses, method);

        // Extract @OpenAPIExample annotations for request body
        if (requestBody != null) {
            OpenAPIExample[] examples = method.getAnnotationsByType(OpenAPIExample.class);
            if (examples.length > 0) {
                requestBody = RequestBodyMetadata.builder()
                    .javaType(requestBody.getJavaType())
                    .required(requestBody.isRequired())
                    .description(requestBody.getDescription())
                    .example(examples[0].value())
                    .build();
                logger.accept("Found " + examples.length + " @OpenAPIExample annotations on " + method.getName());
            }
        }

        // Generate operation ID
        String operationId = method.getName();

        return OperationMetadata.builder()
            .methodName(method.getName())
            .httpMethod(httpMethod)
            .path(operationPath)
            .operationId(operationId)
            .summary("")
            .description("")
            .parameters(parameters)
            .requestBody(requestBody)
            .responses(responses)
            .deprecated(method.isAnnotationPresent(Deprecated.class))
            .build();
    }

    /**
     * Extracts path parameter names from a path string.
     */
    private Set<String> extractPathParameterNames(String path) {
        Set<String> params = new LinkedHashSet<>();
        if (path == null || path.isEmpty()) {
            return params;
        }
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }
        return params;
    }

    /**
     * Infers response metadata from method signature.
     */
    private Map<String, ResponseMetadata> inferResponses(Method method, HttpMethod httpMethod) {
        Map<String, ResponseMetadata> responses = new LinkedHashMap<>();
        Type returnType = method.getGenericReturnType();

        // Determine success status code
        String successCode = switch (httpMethod) {
            case POST -> "201";
            case DELETE -> "204";
            default -> "200";
        };

        // Add success response
        if (returnType.equals(void.class) || returnType.equals(Void.class)) {
            responses.put(successCode, ResponseMetadata.builder()
                .statusCode(successCode)
                .description("Success")
                .build());
        } else {
            responses.put(successCode, ResponseMetadata.builder()
                .statusCode(successCode)
                .description("Success")
                .responseType(returnType)
                .build());
        }

        // Add common error responses
        if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.PUT ||
            httpMethod == HttpMethod.DELETE || httpMethod == HttpMethod.PATCH) {
            responses.put("404", ResponseMetadata.builder()
                .statusCode("404")
                .description("Not Found")
                .build());
        }

        if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT ||
            httpMethod == HttpMethod.PATCH) {
            responses.put("400", ResponseMetadata.builder()
                .statusCode("400")
                .description("Bad Request")
                .build());
        }

        return responses;
    }

    /**
     * Merges @OpenAPIResponse annotations into the inferred responses map.
     * Annotation values override inferred values for matching status codes,
     * and new status codes are added.
     */
    private void mergeAnnotatedResponses(Map<String, ResponseMetadata> responses, Method method) {
        OpenAPIResponse[] annotations = method.getAnnotationsByType(OpenAPIResponse.class);
        if (annotations.length == 0) {
            return;
        }

        logger.accept("Found " + annotations.length + " @OpenAPIResponse annotations on " + method.getName());

        for (OpenAPIResponse ann : annotations) {
            String statusCode = ann.status();
            Type responseType = ann.responseType().equals(Void.class) ? null : ann.responseType();

            // If we're overriding an existing inferred response and no explicit type given,
            // preserve the inferred response type
            if (responseType == null && responses.containsKey(statusCode)) {
                responseType = responses.get(statusCode).getResponseType();
            }

            ResponseMetadata.Builder builder = ResponseMetadata.builder()
                .statusCode(statusCode)
                .description(ann.description())
                .mediaType(ann.mediaType());

            if (responseType != null) {
                builder.responseType(responseType);
            }

            responses.put(statusCode, builder.build());
        }
    }

    /**
     * Generates a tag from the class name.
     */
    private String generateTag(Class<?> endpointClass) {
        String simpleName = endpointClass.getSimpleName();
        // Remove common suffixes
        for (String suffix : List.of("Endpoint", "Controller", "Resource", "Api")) {
            if (simpleName.endsWith(suffix) && simpleName.length() > suffix.length()) {
                simpleName = simpleName.substring(0, simpleName.length() - suffix.length());
                break;
            }
        }
        return simpleName;
    }

    /**
     * Checks if a type is a simple/primitive type.
     */
    private boolean isSimpleType(Class<?> type) {
        return SIMPLE_TYPES.contains(type) || type.isEnum();
    }

    /**
     * Checks if a type is a complex type suitable for request body.
     */
    private boolean isComplexType(Class<?> type) {
        return !type.isPrimitive() &&
               !SIMPLE_TYPES.contains(type) &&
               !type.isEnum() &&
               !type.isArray() &&
               !Collection.class.isAssignableFrom(type) &&
               !Map.class.isAssignableFrom(type);
    }

    /**
     * Finds an annotation by class name using reflection.
     */
    private Annotation findAnnotation(Class<?> clazz, String annotationClassName) {
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationClassName)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Finds an annotation by class name using reflection.
     */
    private Annotation findAnnotation(Method method, String annotationClassName) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationClassName)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Gets an annotation attribute value using reflection.
     */
    @SuppressWarnings("unchecked")
    private <T> T getAnnotationValue(Annotation annotation, String attributeName,
                                      Class<T> type, T defaultValue) {
        try {
            Method valueMethod = annotation.annotationType().getMethod(attributeName);
            Object value = valueMethod.invoke(annotation);
            if (type.isInstance(value)) {
                return (T) value;
            }
        } catch (Exception e) {
            // Ignore and return default
        }
        return defaultValue;
    }
}
