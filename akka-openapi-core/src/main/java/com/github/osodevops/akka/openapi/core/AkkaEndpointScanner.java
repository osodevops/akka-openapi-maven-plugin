package com.github.osodevops.akka.openapi.core;

import com.github.osodevops.akka.openapi.core.exception.ScanningException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Scans the classpath for Akka SDK HTTP endpoint classes.
 *
 * <p>Uses ClassGraph for efficient classpath scanning to find classes
 * annotated with {@code @HttpEndpoint}.</p>
 *
 * @since 1.0.0
 */
public class AkkaEndpointScanner {

    /** The fully qualified name of the Akka SDK HttpEndpoint annotation */
    public static final String HTTP_ENDPOINT_ANNOTATION = "akka.javasdk.annotations.http.HttpEndpoint";

    private final List<String> scanPackages;
    private final ClassLoader classLoader;
    private final Consumer<String> logger;

    /**
     * Creates a new scanner with the specified configuration.
     *
     * @param scanPackages packages to scan for endpoints
     * @param classLoader the classloader to use for scanning
     * @param logger a logger for diagnostic messages (can be null)
     */
    public AkkaEndpointScanner(List<String> scanPackages, ClassLoader classLoader, Consumer<String> logger) {
        this.scanPackages = scanPackages != null ? new ArrayList<>(scanPackages) : new ArrayList<>();
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
        this.logger = logger != null ? logger : msg -> {};
    }

    /**
     * Creates a new scanner with the specified configuration.
     *
     * @param scanPackages packages to scan for endpoints
     * @param classLoader the classloader to use for scanning
     */
    public AkkaEndpointScanner(List<String> scanPackages, ClassLoader classLoader) {
        this(scanPackages, classLoader, null);
    }

    /**
     * Scans the classpath for classes annotated with @HttpEndpoint.
     *
     * @return list of ClassInfo objects for discovered endpoint classes
     * @throws ScanningException if scanning fails
     */
    public List<ClassInfo> scanForEndpoints() throws ScanningException {
        logger.accept("Scanning for Akka HTTP endpoints in packages: " + scanPackages);

        try (ScanResult scanResult = buildClassGraph().scan()) {
            List<ClassInfo> endpoints = scanResult
                .getClassesWithAnnotation(HTTP_ENDPOINT_ANNOTATION)
                .stream()
                .filter(this::isConcreteClass)
                .collect(Collectors.toList());

            logger.accept("Found " + endpoints.size() + " HTTP endpoint class(es)");

            return endpoints;
        } catch (Exception e) {
            throw new ScanningException("Failed to scan classpath for HTTP endpoints: " + e.getMessage(), e);
        }
    }

    /**
     * Scans and loads endpoint classes.
     *
     * @return list of loaded Class objects for discovered endpoints
     * @throws ScanningException if scanning or class loading fails
     */
    public List<Class<?>> scanAndLoadEndpoints() throws ScanningException {
        List<ClassInfo> classInfos = scanForEndpoints();
        List<Class<?>> classes = new ArrayList<>();

        for (ClassInfo classInfo : classInfos) {
            classes.add(loadClass(classInfo));
        }

        return classes;
    }

    /**
     * Loads a Class object from ClassInfo.
     *
     * <p>Uses the configured classloader to ensure classes are loaded from
     * the correct classpath (e.g., project classes during Maven plugin execution).</p>
     *
     * @param classInfo the class info to load
     * @return the loaded Class object
     * @throws ScanningException if the class cannot be loaded
     */
    public Class<?> loadClass(ClassInfo classInfo) throws ScanningException {
        Objects.requireNonNull(classInfo, "classInfo must not be null");

        // Use our classLoader explicitly rather than classInfo.loadClass()
        // which may use ClassGraph's internal classloader
        return loadClass(classInfo.getName());
    }

    /**
     * Loads a Class object by name.
     *
     * @param className the fully qualified class name
     * @return the loaded Class object
     * @throws ScanningException if the class cannot be loaded
     */
    public Class<?> loadClass(String className) throws ScanningException {
        Objects.requireNonNull(className, "className must not be null");

        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new ScanningException("Class not found: " + className, e);
        } catch (NoClassDefFoundError e) {
            throw new ScanningException("Class dependency not found for: " + className + " - " + e.getMessage(), e);
        }
    }

    /**
     * Gets the packages being scanned.
     *
     * @return an unmodifiable list of package names
     */
    public List<String> getScanPackages() {
        return Collections.unmodifiableList(scanPackages);
    }

    private ClassGraph buildClassGraph() {
        ClassGraph classGraph = new ClassGraph()
            .overrideClassLoaders(classLoader)
            .enableClassInfo()
            .enableAnnotationInfo()
            .enableMethodInfo();

        if (!scanPackages.isEmpty()) {
            classGraph.acceptPackages(scanPackages.toArray(new String[0]));
        }

        return classGraph;
    }

    private boolean isConcreteClass(ClassInfo classInfo) {
        // Exclude abstract classes
        if (classInfo.isAbstract()) {
            logger.accept("  Skipping abstract class: " + classInfo.getName());
            return false;
        }

        // Exclude interfaces
        if (classInfo.isInterface()) {
            logger.accept("  Skipping interface: " + classInfo.getName());
            return false;
        }

        // Exclude annotations
        if (classInfo.isAnnotation()) {
            logger.accept("  Skipping annotation: " + classInfo.getName());
            return false;
        }

        // Exclude enums
        if (classInfo.isEnum()) {
            logger.accept("  Skipping enum: " + classInfo.getName());
            return false;
        }

        return true;
    }

    /**
     * Builder for creating AkkaEndpointScanner instances.
     */
    public static class Builder {
        private List<String> scanPackages = new ArrayList<>();
        private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        private Consumer<String> logger;

        public Builder scanPackages(Collection<String> packages) {
            if (packages != null) {
                this.scanPackages = new ArrayList<>(packages);
            }
            return this;
        }

        public Builder addScanPackage(String packageName) {
            if (packageName != null && !packageName.isBlank()) {
                this.scanPackages.add(packageName);
            }
            return this;
        }

        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder logger(Consumer<String> logger) {
            this.logger = logger;
            return this;
        }

        public AkkaEndpointScanner build() {
            return new AkkaEndpointScanner(scanPackages, classLoader, logger);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
