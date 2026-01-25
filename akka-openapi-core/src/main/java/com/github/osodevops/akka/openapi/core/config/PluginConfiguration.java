package com.github.osodevops.akka.openapi.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration holder for the OpenAPI generator.
 *
 * @since 1.0.0
 */
public class PluginConfiguration {

    private final String apiTitle;
    private final String apiVersion;
    private final String apiDescription;
    private final String termsOfService;
    private final String contactName;
    private final String contactEmail;
    private final String contactUrl;
    private final String licenseName;
    private final String licenseUrl;
    private final List<ServerConfig> servers;
    private final List<String> scanPackages;
    private final boolean generateRequestSchemas;
    private final boolean generateResponseSchemas;
    private final boolean includeSecuritySchemes;
    private final boolean failOnValidationError;

    private PluginConfiguration(Builder builder) {
        this.apiTitle = builder.apiTitle;
        this.apiVersion = builder.apiVersion;
        this.apiDescription = builder.apiDescription;
        this.termsOfService = builder.termsOfService;
        this.contactName = builder.contactName;
        this.contactEmail = builder.contactEmail;
        this.contactUrl = builder.contactUrl;
        this.licenseName = builder.licenseName;
        this.licenseUrl = builder.licenseUrl;
        this.servers = Collections.unmodifiableList(new ArrayList<>(builder.servers));
        this.scanPackages = Collections.unmodifiableList(new ArrayList<>(builder.scanPackages));
        this.generateRequestSchemas = builder.generateRequestSchemas;
        this.generateResponseSchemas = builder.generateResponseSchemas;
        this.includeSecuritySchemes = builder.includeSecuritySchemes;
        this.failOnValidationError = builder.failOnValidationError;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getApiTitle() {
        return apiTitle;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getApiDescription() {
        return apiDescription;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactUrl() {
        return contactUrl;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public List<ServerConfig> getServers() {
        return servers;
    }

    public List<String> getScanPackages() {
        return scanPackages;
    }

    public boolean isGenerateRequestSchemas() {
        return generateRequestSchemas;
    }

    public boolean isGenerateResponseSchemas() {
        return generateResponseSchemas;
    }

    public boolean isIncludeSecuritySchemes() {
        return includeSecuritySchemes;
    }

    public boolean isFailOnValidationError() {
        return failOnValidationError;
    }

    public static class Builder {
        private String apiTitle = "API";
        private String apiVersion = "1.0.0";
        private String apiDescription = "";
        private String termsOfService;
        private String contactName;
        private String contactEmail;
        private String contactUrl;
        private String licenseName;
        private String licenseUrl;
        private List<ServerConfig> servers = new ArrayList<>();
        private List<String> scanPackages = new ArrayList<>();
        private boolean generateRequestSchemas = true;
        private boolean generateResponseSchemas = true;
        private boolean includeSecuritySchemes = true;
        private boolean failOnValidationError = true;

        public Builder apiTitle(String apiTitle) {
            this.apiTitle = Objects.requireNonNull(apiTitle, "apiTitle must not be null");
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            this.apiVersion = Objects.requireNonNull(apiVersion, "apiVersion must not be null");
            return this;
        }

        public Builder apiDescription(String apiDescription) {
            this.apiDescription = apiDescription;
            return this;
        }

        public Builder termsOfService(String termsOfService) {
            this.termsOfService = termsOfService;
            return this;
        }

        public Builder contactName(String contactName) {
            this.contactName = contactName;
            return this;
        }

        public Builder contactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
            return this;
        }

        public Builder contactUrl(String contactUrl) {
            this.contactUrl = contactUrl;
            return this;
        }

        public Builder licenseName(String licenseName) {
            this.licenseName = licenseName;
            return this;
        }

        public Builder licenseUrl(String licenseUrl) {
            this.licenseUrl = licenseUrl;
            return this;
        }

        public Builder servers(List<ServerConfig> servers) {
            this.servers = servers != null ? servers : new ArrayList<>();
            return this;
        }

        public Builder addServer(ServerConfig server) {
            this.servers.add(server);
            return this;
        }

        public Builder scanPackages(List<String> scanPackages) {
            this.scanPackages = scanPackages != null ? scanPackages : new ArrayList<>();
            return this;
        }

        public Builder addScanPackage(String scanPackage) {
            this.scanPackages.add(scanPackage);
            return this;
        }

        public Builder generateRequestSchemas(boolean generateRequestSchemas) {
            this.generateRequestSchemas = generateRequestSchemas;
            return this;
        }

        public Builder generateResponseSchemas(boolean generateResponseSchemas) {
            this.generateResponseSchemas = generateResponseSchemas;
            return this;
        }

        public Builder includeSecuritySchemes(boolean includeSecuritySchemes) {
            this.includeSecuritySchemes = includeSecuritySchemes;
            return this;
        }

        public Builder failOnValidationError(boolean failOnValidationError) {
            this.failOnValidationError = failOnValidationError;
            return this;
        }

        public PluginConfiguration build() {
            return new PluginConfiguration(this);
        }
    }
}
