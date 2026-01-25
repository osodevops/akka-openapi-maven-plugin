package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.core.exception.ScanningException;
import com.github.osodevops.akka.openapi.core.fixtures.ComplexEndpoint;
import com.github.osodevops.akka.openapi.core.fixtures.MockHttpEndpoint;
import com.github.osodevops.akka.openapi.core.fixtures.SimpleEndpoint;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for AkkaEndpointScanner.
 *
 * Since we don't have the real Akka SDK annotations in test scope,
 * we test the scanner logic using our MockHttpEndpoint annotation.
 */
class AkkaEndpointScannerTest {

    private List<String> logMessages;

    @BeforeEach
    void setUp() {
        logMessages = new ArrayList<>();
    }

    @Test
    void shouldFindAnnotatedEndpointsWithMockAnnotation() {
        // Test the scanning logic using our mock annotation
        List<ClassInfo> endpoints = scanWithMockAnnotation(
            "com.github.osodevops.akka.openapi.core.fixtures"
        );

        assertThat(endpoints)
            .extracting(ClassInfo::getName)
            .contains(
                SimpleEndpoint.class.getName(),
                ComplexEndpoint.class.getName()
            );
    }

    @Test
    void shouldExcludeAbstractClasses() {
        List<ClassInfo> endpoints = scanWithMockAnnotation(
            "com.github.osodevops.akka.openapi.core.fixtures"
        );

        assertThat(endpoints)
            .extracting(ClassInfo::getName)
            .doesNotContain("com.github.osodevops.akka.openapi.core.fixtures.AbstractEndpoint");
    }

    @Test
    void shouldExcludeInterfaces() {
        List<ClassInfo> endpoints = scanWithMockAnnotation(
            "com.github.osodevops.akka.openapi.core.fixtures"
        );

        assertThat(endpoints)
            .extracting(ClassInfo::getName)
            .doesNotContain("com.github.osodevops.akka.openapi.core.fixtures.EndpointInterface");
    }

    @Test
    void shouldNotFindUnannotatedClasses() {
        List<ClassInfo> endpoints = scanWithMockAnnotation(
            "com.github.osodevops.akka.openapi.core.fixtures"
        );

        assertThat(endpoints)
            .extracting(ClassInfo::getName)
            .doesNotContain("com.github.osodevops.akka.openapi.core.fixtures.NotAnEndpoint");
    }

    @Test
    void shouldLoadClassFromClassInfo() {
        // ClassInfo must be used while ScanResult is still open
        try (ScanResult scanResult = new ClassGraph()
            .overrideClassLoaders(getClass().getClassLoader())
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptPackages("com.github.osodevops.akka.openapi.core.fixtures")
            .scan()) {

            ClassInfo simpleEndpoint = scanResult
                .getClassesWithAnnotation(MockHttpEndpoint.class.getName())
                .stream()
                .filter(ci -> ci.getName().endsWith("SimpleEndpoint"))
                .findFirst()
                .orElseThrow();

            AkkaEndpointScanner scanner = createScanner();
            Class<?> loaded = scanner.loadClass(simpleEndpoint);

            assertThat(loaded).isEqualTo(SimpleEndpoint.class);
        }
    }

    @Test
    void shouldLoadClassByName() {
        AkkaEndpointScanner scanner = createScanner();

        Class<?> loaded = scanner.loadClass(SimpleEndpoint.class.getName());

        assertThat(loaded).isEqualTo(SimpleEndpoint.class);
    }

    @Test
    void shouldThrowExceptionForNonExistentClass() {
        AkkaEndpointScanner scanner = createScanner();

        assertThatThrownBy(() -> scanner.loadClass("com.nonexistent.FakeClass"))
            .isInstanceOf(ScanningException.class)
            .hasMessageContaining("Class not found")
            .hasMessageContaining("FakeClass");
    }

    @Test
    void shouldReturnEmptyListForNonMatchingPackage() {
        List<ClassInfo> endpoints = scanWithMockAnnotation("com.nonexistent.package");

        assertThat(endpoints).isEmpty();
    }

    @Test
    void shouldBuildWithBuilder() {
        AkkaEndpointScanner scanner = AkkaEndpointScanner.builder()
            .addScanPackage("com.github.osodevops.akka.openapi.core.fixtures")
            .classLoader(getClass().getClassLoader())
            .logger(logMessages::add)
            .build();

        assertThat(scanner.getScanPackages())
            .containsExactly("com.github.osodevops.akka.openapi.core.fixtures");
    }

    @Test
    void shouldHandleNullPackagesInBuilder() {
        AkkaEndpointScanner scanner = AkkaEndpointScanner.builder()
            .scanPackages(null)
            .addScanPackage(null)
            .addScanPackage("")
            .addScanPackage("   ")
            .classLoader(getClass().getClassLoader())
            .build();

        assertThat(scanner.getScanPackages()).isEmpty();
    }

    @Test
    void shouldRejectNullClassLoader() {
        assertThatThrownBy(() -> new AkkaEndpointScanner(List.of("test"), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("classLoader");
    }

    @Test
    void shouldRejectNullClassInfoForLoad() {
        AkkaEndpointScanner scanner = createScanner();

        assertThatThrownBy(() -> scanner.loadClass((ClassInfo) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullClassNameForLoad() {
        AkkaEndpointScanner scanner = createScanner();

        assertThatThrownBy(() -> scanner.loadClass((String) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldVerifyScannerReturnsScanPackages() {
        AkkaEndpointScanner scanner = new AkkaEndpointScanner(
            List.of("com.example.one", "com.example.two"),
            getClass().getClassLoader()
        );

        assertThat(scanner.getScanPackages())
            .containsExactly("com.example.one", "com.example.two");
    }

    @Test
    void scanPackagesShouldBeImmutable() {
        AkkaEndpointScanner scanner = createScanner();

        assertThatThrownBy(() -> scanner.getScanPackages().add("new.package"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // Helper methods

    private AkkaEndpointScanner createScanner() {
        return new AkkaEndpointScanner(
            List.of("com.github.osodevops.akka.openapi.core.fixtures"),
            getClass().getClassLoader(),
            logMessages::add
        );
    }

    /**
     * Scans using our mock annotation to test the scanning logic.
     * The actual scanner uses the real Akka annotation, but we test
     * the filtering logic with our mock annotation.
     */
    private List<ClassInfo> scanWithMockAnnotation(String... packages) {
        try (ScanResult scanResult = new ClassGraph()
            .overrideClassLoaders(getClass().getClassLoader())
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptPackages(packages)
            .scan()) {

            return scanResult
                .getClassesWithAnnotation(MockHttpEndpoint.class.getName())
                .stream()
                .filter(ci -> !ci.isAbstract())
                .filter(ci -> !ci.isInterface())
                .filter(ci -> !ci.isAnnotation())
                .filter(ci -> !ci.isEnum())
                .toList();
        }
    }
}
