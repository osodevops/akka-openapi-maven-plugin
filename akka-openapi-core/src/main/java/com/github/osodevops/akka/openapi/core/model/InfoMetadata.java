package com.github.osodevops.akka.openapi.core.model;

/**
 * Represents API info metadata extracted from @OpenAPIInfo annotations.
 *
 * @since 1.1.0
 */
public final class InfoMetadata {

    private final String title;
    private final String version;
    private final String description;
    private final String termsOfService;
    private final String contactName;
    private final String contactEmail;
    private final String contactUrl;
    private final String licenseName;
    private final String licenseUrl;

    private InfoMetadata(Builder builder) {
        this.title = builder.title != null ? builder.title : "";
        this.version = builder.version != null ? builder.version : "";
        this.description = builder.description != null ? builder.description : "";
        this.termsOfService = builder.termsOfService != null ? builder.termsOfService : "";
        this.contactName = builder.contactName != null ? builder.contactName : "";
        this.contactEmail = builder.contactEmail != null ? builder.contactEmail : "";
        this.contactUrl = builder.contactUrl != null ? builder.contactUrl : "";
        this.licenseName = builder.licenseName != null ? builder.licenseName : "";
        this.licenseUrl = builder.licenseUrl != null ? builder.licenseUrl : "";
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() { return title; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getTermsOfService() { return termsOfService; }
    public String getContactName() { return contactName; }
    public String getContactEmail() { return contactEmail; }
    public String getContactUrl() { return contactUrl; }
    public String getLicenseName() { return licenseName; }
    public String getLicenseUrl() { return licenseUrl; }

    /**
     * Returns true if any field has a non-empty value.
     */
    public boolean hasContent() {
        return !title.isEmpty() || !version.isEmpty() || !description.isEmpty() ||
               !termsOfService.isEmpty() || !contactName.isEmpty() || !contactEmail.isEmpty() ||
               !contactUrl.isEmpty() || !licenseName.isEmpty() || !licenseUrl.isEmpty();
    }

    @Override
    public String toString() {
        return "InfoMetadata{title='" + title + "', version='" + version + "'}";
    }

    public static final class Builder {
        private String title;
        private String version;
        private String description;
        private String termsOfService;
        private String contactName;
        private String contactEmail;
        private String contactUrl;
        private String licenseName;
        private String licenseUrl;

        private Builder() {}

        public Builder title(String title) { this.title = title; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder termsOfService(String termsOfService) { this.termsOfService = termsOfService; return this; }
        public Builder contactName(String contactName) { this.contactName = contactName; return this; }
        public Builder contactEmail(String contactEmail) { this.contactEmail = contactEmail; return this; }
        public Builder contactUrl(String contactUrl) { this.contactUrl = contactUrl; return this; }
        public Builder licenseName(String licenseName) { this.licenseName = licenseName; return this; }
        public Builder licenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; return this; }

        public InfoMetadata build() {
            return new InfoMetadata(this);
        }
    }
}
