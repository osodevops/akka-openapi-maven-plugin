package com.github.osodevops.akka.openapi.maven;

import com.github.osodevops.akka.openapi.core.*;
import com.github.osodevops.akka.openapi.core.config.PluginConfiguration;
import com.github.osodevops.akka.openapi.core.config.ServerConfig;
import com.github.osodevops.akka.openapi.core.exception.ScanningException;
import com.github.osodevops.akka.openapi.core.model.EndpointMetadata;
import io.github.classgraph.ClassInfo;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates an OpenAPI 3.1 specification from Akka SDK HTTP endpoint annotations.
 *
 * <p>This mojo scans the project's compiled classes for Akka SDK {@code @HttpEndpoint}
 * annotations and generates a complete OpenAPI specification file.</p>
 *
 * <p>Usage in pom.xml:</p>
 * <pre>{@code
 * <plugin>
 *   <groupId>com.github.osodevops</groupId>
 *   <artifactId>akka-openapi-maven-plugin</artifactId>
 *   <version>1.0.0-SNAPSHOT</version>
 *   <executions>
 *     <execution>
 *       <goals>
 *         <goal>generate</goal>
 *       </goals>
 *     </execution>
 *   </executions>
 *   <configuration>
 *     <apiTitle>My API</apiTitle>
 *     <apiVersion>1.0.0</apiVersion>
 *   </configuration>
 * </plugin>
 * }</pre>
 */
@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true
)
public class GenerateOpenAPIMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The output file path for the generated OpenAPI specification.
     */
    @Parameter(property = "openapi.outputFile", defaultValue = "${project.build.directory}/openapi.yaml")
    private File outputFile;

    /**
     * The output format: "yaml" or "json".
     */
    @Parameter(property = "openapi.outputFormat", defaultValue = "yaml")
    private String outputFormat;

    /**
     * The API title.
     */
    @Parameter(property = "openapi.apiTitle", defaultValue = "${project.name}")
    private String apiTitle;

    /**
     * The API version.
     */
    @Parameter(property = "openapi.apiVersion", defaultValue = "${project.version}")
    private String apiVersion;

    /**
     * The API description.
     */
    @Parameter(property = "openapi.apiDescription", defaultValue = "${project.description}")
    private String apiDescription;

    /**
     * The contact name.
     */
    @Parameter(property = "openapi.contactName")
    private String contactName;

    /**
     * The contact email.
     */
    @Parameter(property = "openapi.contactEmail")
    private String contactEmail;

    /**
     * The contact URL.
     */
    @Parameter(property = "openapi.contactUrl")
    private String contactUrl;

    /**
     * The license name.
     */
    @Parameter(property = "openapi.licenseName")
    private String licenseName;

    /**
     * The license URL.
     */
    @Parameter(property = "openapi.licenseUrl")
    private String licenseUrl;

    /**
     * The terms of service URL.
     */
    @Parameter(property = "openapi.termsOfService")
    private String termsOfService;

    /**
     * Packages to scan for endpoints. If empty, scans all packages.
     */
    @Parameter(property = "openapi.scanPackages")
    private List<String> scanPackages;

    /**
     * Server configurations for the OpenAPI spec.
     */
    @Parameter
    private List<ServerConfigParam> servers;

    /**
     * Whether to skip plugin execution.
     */
    @Parameter(property = "openapi.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Whether to fail the build on validation errors.
     */
    @Parameter(property = "openapi.failOnValidationError", defaultValue = "true")
    private boolean failOnValidationError;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping OpenAPI generation");
            return;
        }

        getLog().info("Generating OpenAPI specification...");

        try {
            // Build configuration
            PluginConfiguration config = buildConfiguration();

            // Create class loader for project classes
            ClassLoader projectClassLoader = createProjectClassLoader();

            // Scan for endpoints
            List<EndpointMetadata> endpoints = scanAndExtractEndpoints(config, projectClassLoader);

            if (endpoints.isEmpty()) {
                getLog().warn("No @HttpEndpoint annotated classes found");
                return;
            }

            getLog().info("Found " + endpoints.size() + " endpoints");

            // Build OpenAPI specification
            OpenAPIModelBuilder builder = new OpenAPIModelBuilder(config, msg -> getLog().debug(msg));
            OpenAPI openAPI = builder.build(endpoints);

            // Validate
            validateSpec(openAPI);

            // Serialize and write
            writeSpec(openAPI);

            getLog().info("OpenAPI specification generated: " + outputFile.getAbsolutePath());

        } catch (MojoFailureException | MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate OpenAPI specification", e);
        }
    }

    private PluginConfiguration buildConfiguration() {
        PluginConfiguration.Builder builder = PluginConfiguration.builder()
            .apiTitle(apiTitle != null ? apiTitle : "API")
            .apiVersion(apiVersion != null ? apiVersion : "1.0.0")
            .apiDescription(apiDescription)
            .contactName(contactName)
            .contactEmail(contactEmail)
            .contactUrl(contactUrl)
            .licenseName(licenseName)
            .licenseUrl(licenseUrl)
            .termsOfService(termsOfService)
            .failOnValidationError(failOnValidationError);

        if (scanPackages != null && !scanPackages.isEmpty()) {
            builder.scanPackages(scanPackages);
        }

        if (servers != null) {
            for (ServerConfigParam server : servers) {
                builder.addServer(new ServerConfig(server.url, server.description));
            }
        }

        return builder.build();
    }

    private ClassLoader createProjectClassLoader() throws MojoExecutionException {
        try {
            List<URL> urls = new ArrayList<>();

            // Add project output directory
            File classesDir = new File(project.getBuild().getOutputDirectory());
            if (classesDir.exists()) {
                urls.add(classesDir.toURI().toURL());
            }

            // Add compile classpath elements
            for (Object element : project.getCompileClasspathElements()) {
                File file = new File(element.toString());
                if (file.exists()) {
                    urls.add(file.toURI().toURL());
                }
            }

            getLog().debug("Class loader URLs: " + urls);

            return new URLClassLoader(
                urls.toArray(new URL[0]),
                getClass().getClassLoader()
            );

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to create project class loader", e);
        }
    }

    private List<EndpointMetadata> scanAndExtractEndpoints(PluginConfiguration config,
                                                           ClassLoader classLoader)
            throws MojoExecutionException {

        AkkaEndpointScanner scanner = new AkkaEndpointScanner(
            config.getScanPackages(),
            classLoader,
            msg -> getLog().debug(msg)
        );

        AkkaAnnotationExtractor extractor = new AkkaAnnotationExtractor(msg -> getLog().debug(msg));

        List<EndpointMetadata> endpoints = new ArrayList<>();

        try {
            List<ClassInfo> endpointClasses = scanner.scanForEndpoints();

            for (ClassInfo classInfo : endpointClasses) {
                try {
                    Class<?> clazz = scanner.loadClass(classInfo);
                    EndpointMetadata metadata = extractor.extractEndpoint(clazz);
                    endpoints.add(metadata);
                    getLog().info("  Processed: " + classInfo.getName());
                } catch (Exception e) {
                    getLog().warn("Failed to process endpoint: " + classInfo.getName() + " - " + e.getMessage());
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Exception details:", e);
                    }
                }
            }

        } catch (ScanningException e) {
            throw new MojoExecutionException("Failed to scan for endpoints", e);
        }

        return endpoints;
    }

    private void validateSpec(OpenAPI openAPI) throws MojoFailureException {
        OpenAPIValidator validator = new OpenAPIValidator(msg -> getLog().debug(msg));
        OpenAPIValidator.ValidationResult result = validator.validate(openAPI);

        if (!result.isValid()) {
            String errorSummary = result.getErrorSummary();
            getLog().error("OpenAPI validation failed:\n" + errorSummary);

            if (failOnValidationError) {
                throw new MojoFailureException("OpenAPI validation failed. " +
                    "Set failOnValidationError=false to ignore validation errors.");
            }
        }

        if (result.hasWarnings()) {
            for (String warning : result.getWarnings()) {
                getLog().warn(warning);
            }
        }
    }

    private void writeSpec(OpenAPI openAPI) throws MojoExecutionException {
        OpenAPISerializer serializer = new OpenAPISerializer(msg -> getLog().debug(msg));

        OpenAPISerializer.Format format;
        if ("json".equalsIgnoreCase(outputFormat)) {
            format = OpenAPISerializer.Format.JSON;
        } else {
            format = OpenAPISerializer.Format.YAML;
        }

        try {
            Path path = outputFile.toPath();
            serializer.writeToFile(openAPI, path, format);
        } catch (OpenAPISerializer.SerializationException e) {
            throw new MojoExecutionException("Failed to write OpenAPI specification", e);
        }
    }

    // Setters for testing

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setApiTitle(String apiTitle) {
        this.apiTitle = apiTitle;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public void setApiDescription(String apiDescription) {
        this.apiDescription = apiDescription;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public void setFailOnValidationError(boolean failOnValidationError) {
        this.failOnValidationError = failOnValidationError;
    }

    public void setScanPackages(List<String> scanPackages) {
        this.scanPackages = scanPackages;
    }

    public void setServers(List<ServerConfigParam> servers) {
        this.servers = servers;
    }

    /**
     * Server configuration parameter for Maven plugin configuration.
     */
    public static class ServerConfigParam {
        @Parameter
        private String url;

        @Parameter
        private String description;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
