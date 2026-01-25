package com.github.osodevops.akka.openapi.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents security metadata extracted from Akka SDK @Acl annotations.
 *
 * @since 1.0.0
 */
public final class SecurityMetadata {

    private final SecurityType type;
    private final String schemeName;
    private final String description;
    private final List<String> scopes;

    private SecurityMetadata(Builder builder) {
        this.type = Objects.requireNonNull(builder.type, "type must not be null");
        this.schemeName = builder.schemeName != null ? builder.schemeName : "default";
        this.description = builder.description != null ? builder.description : "";
        this.scopes = Collections.unmodifiableList(new ArrayList<>(builder.scopes));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the security scheme type.
     *
     * @return the type
     */
    public SecurityType getType() {
        return type;
    }

    /**
     * Gets the security scheme name.
     *
     * @return the scheme name
     */
    public String getSchemeName() {
        return schemeName;
    }

    /**
     * Gets the security scheme description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the required scopes.
     *
     * @return an unmodifiable list of scopes
     */
    public List<String> getScopes() {
        return scopes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityMetadata that = (SecurityMetadata) o;
        return type == that.type &&
               Objects.equals(schemeName, that.schemeName) &&
               Objects.equals(scopes, that.scopes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, schemeName, scopes);
    }

    @Override
    public String toString() {
        return "SecurityMetadata{" +
               "type=" + type +
               ", schemeName='" + schemeName + '\'' +
               ", scopes=" + scopes +
               '}';
    }

    /**
     * Security scheme types.
     */
    public enum SecurityType {
        /** No security required (public access) */
        NONE,
        /** Bearer token authentication */
        BEARER,
        /** API key authentication */
        API_KEY,
        /** OAuth2 */
        OAUTH2,
        /** OpenID Connect */
        OPENID_CONNECT,
        /** Internal service-to-service */
        INTERNAL
    }

    public static final class Builder {
        private SecurityType type = SecurityType.NONE;
        private String schemeName;
        private String description;
        private List<String> scopes = new ArrayList<>();

        private Builder() {
        }

        public Builder type(SecurityType type) {
            this.type = type;
            return this;
        }

        public Builder schemeName(String schemeName) {
            this.schemeName = schemeName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder scopes(List<String> scopes) {
            this.scopes = scopes != null ? new ArrayList<>(scopes) : new ArrayList<>();
            return this;
        }

        public Builder addScope(String scope) {
            if (scope != null && !scope.isBlank()) {
                this.scopes.add(scope);
            }
            return this;
        }

        public SecurityMetadata build() {
            return new SecurityMetadata(this);
        }
    }
}
