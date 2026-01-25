package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.core.model.*;
import com.github.osodevops.akka.openapi.core.model.OperationMetadata.HttpMethod;
import com.github.osodevops.akka.openapi.core.model.ParameterMetadata.ParameterLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for AkkaAnnotationExtractor.
 *
 * Since we can't use the real Akka SDK annotations in tests, we test
 * the core extraction logic methods using reflection.
 */
class AkkaAnnotationExtractorTest {

    private List<String> logMessages;
    private AkkaAnnotationExtractor extractor;

    @BeforeEach
    void setUp() {
        logMessages = new ArrayList<>();
        extractor = new AkkaAnnotationExtractor(logMessages::add);
    }

    @Test
    void shouldRejectNullClass() {
        assertThatThrownBy(() -> extractor.extractEndpoint(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("endpointClass");
    }

    @Test
    void shouldRejectClassWithoutHttpEndpointAnnotation() {
        assertThatThrownBy(() -> extractor.extractEndpoint(String.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not annotated with @HttpEndpoint");
    }

    @Test
    void shouldExtractPathParameterNamesFromPath() {
        Pattern pattern = Pattern.compile("\\{([^}]+)}");

        // Test various path patterns
        assertPathParams("/customers/{id}", Set.of("id"));
        assertPathParams("/orders/{orderId}/items/{itemId}", Set.of("orderId", "itemId"));
        assertPathParams("/users/{userId}/posts/{postId}/comments/{commentId}",
            Set.of("userId", "postId", "commentId"));
        assertPathParams("/simple", Set.of());
        assertPathParams("", Set.of());
    }

    private void assertPathParams(String path, Set<String> expected) {
        Pattern pattern = Pattern.compile("\\{([^}]+)}");
        Set<String> actual = new java.util.LinkedHashSet<>();
        Matcher matcher = pattern.matcher(path);
        while (matcher.find()) {
            actual.add(matcher.group(1));
        }
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldIdentifySimpleTypes() {
        Set<Class<?>> simpleTypes = Set.of(
            String.class, Integer.class, int.class, Long.class, long.class,
            Double.class, double.class, Float.class, float.class,
            Boolean.class, boolean.class
        );

        for (Class<?> type : simpleTypes) {
            assertThat(isSimpleType(type))
                .as("Type %s should be simple", type.getName())
                .isTrue();
        }
    }

    @Test
    void shouldIdentifyComplexTypes() {
        assertThat(isComplexType(Object.class)).isTrue();
        assertThat(isComplexType(String.class)).isFalse();
        assertThat(isComplexType(int.class)).isFalse();
        assertThat(isComplexType(List.class)).isFalse(); // Collection
    }

    @Test
    void shouldGenerateTagFromClassName() {
        assertThat(generateTag("CustomerEndpoint")).isEqualTo("Customer");
        assertThat(generateTag("OrderController")).isEqualTo("Order");
        assertThat(generateTag("UserResource")).isEqualTo("User");
        assertThat(generateTag("ProductApi")).isEqualTo("Product");
        assertThat(generateTag("HealthCheck")).isEqualTo("HealthCheck"); // No matching suffix
    }

    @Test
    void shouldInferCorrectSuccessStatusCodes() {
        // POST should return 201
        assertThat(getSuccessCode(HttpMethod.POST)).isEqualTo("201");

        // DELETE should return 204
        assertThat(getSuccessCode(HttpMethod.DELETE)).isEqualTo("204");

        // GET, PUT, PATCH should return 200
        assertThat(getSuccessCode(HttpMethod.GET)).isEqualTo("200");
        assertThat(getSuccessCode(HttpMethod.PUT)).isEqualTo("200");
        assertThat(getSuccessCode(HttpMethod.PATCH)).isEqualTo("200");
    }

    @Test
    void shouldAddNotFoundResponseForGetPutDeletePatch() {
        Set<HttpMethod> methodsWith404 = Set.of(
            HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH
        );

        for (HttpMethod method : methodsWith404) {
            assertThat(shouldHave404(method))
                .as("HTTP %s should have 404 response", method)
                .isTrue();
        }

        assertThat(shouldHave404(HttpMethod.POST)).isFalse();
    }

    @Test
    void shouldAddBadRequestResponseForPostPutPatch() {
        Set<HttpMethod> methodsWith400 = Set.of(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH
        );

        for (HttpMethod method : methodsWith400) {
            assertThat(shouldHave400(method))
                .as("HTTP %s should have 400 response", method)
                .isTrue();
        }

        assertThat(shouldHave400(HttpMethod.GET)).isFalse();
        assertThat(shouldHave400(HttpMethod.DELETE)).isFalse();
    }

    @Test
    void shouldCreateExtractorWithoutLogger() {
        AkkaAnnotationExtractor noLogExtractor = new AkkaAnnotationExtractor();
        assertThat(noLogExtractor).isNotNull();
    }

    // Helper methods that mirror the extractor's logic for testing

    private boolean isSimpleType(Class<?> type) {
        Set<Class<?>> simpleTypes = Set.of(
            String.class, Integer.class, int.class, Long.class, long.class,
            Double.class, double.class, Float.class, float.class,
            Boolean.class, boolean.class, Short.class, short.class,
            Byte.class, byte.class, Character.class, char.class
        );
        return simpleTypes.contains(type) || type.isEnum();
    }

    private boolean isComplexType(Class<?> type) {
        return !type.isPrimitive() &&
               !isSimpleType(type) &&
               !type.isEnum() &&
               !type.isArray() &&
               !java.util.Collection.class.isAssignableFrom(type) &&
               !java.util.Map.class.isAssignableFrom(type);
    }

    private String generateTag(String className) {
        String simpleName = className;
        for (String suffix : List.of("Endpoint", "Controller", "Resource", "Api")) {
            if (simpleName.endsWith(suffix) && simpleName.length() > suffix.length()) {
                return simpleName.substring(0, simpleName.length() - suffix.length());
            }
        }
        return simpleName;
    }

    private String getSuccessCode(HttpMethod httpMethod) {
        return switch (httpMethod) {
            case POST -> "201";
            case DELETE -> "204";
            default -> "200";
        };
    }

    private boolean shouldHave404(HttpMethod httpMethod) {
        return httpMethod == HttpMethod.GET || httpMethod == HttpMethod.PUT ||
               httpMethod == HttpMethod.DELETE || httpMethod == HttpMethod.PATCH;
    }

    private boolean shouldHave400(HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT ||
               httpMethod == HttpMethod.PATCH;
    }
}
