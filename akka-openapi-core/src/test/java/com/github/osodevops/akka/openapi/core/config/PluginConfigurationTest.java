package com.github.osodevops.akka.openapi.core.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for PluginConfiguration.
 */
class PluginConfigurationTest {

    @Test
    void shouldBuildWithDefaults() {
        PluginConfiguration config = PluginConfiguration.builder().build();

        assertThat(config.getApiTitle()).isEqualTo("API");
        assertThat(config.getApiVersion()).isEqualTo("1.0.0");
        assertThat(config.getApiDescription()).isEmpty();
        assertThat(config.getServers()).isEmpty();
        assertThat(config.getScanPackages()).isEmpty();
        assertThat(config.isGenerateRequestSchemas()).isTrue();
        assertThat(config.isGenerateResponseSchemas()).isTrue();
        assertThat(config.isIncludeSecuritySchemes()).isTrue();
        assertThat(config.isFailOnValidationError()).isTrue();
    }

    @Test
    void shouldBuildWithCustomValues() {
        ServerConfig server = new ServerConfig("https://api.example.com", "Production");

        PluginConfiguration config = PluginConfiguration.builder()
            .apiTitle("My API")
            .apiVersion("2.0.0")
            .apiDescription("Test API")
            .termsOfService("https://example.com/terms")
            .contactName("Support")
            .contactEmail("support@example.com")
            .contactUrl("https://example.com/support")
            .licenseName("Apache 2.0")
            .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
            .addServer(server)
            .addScanPackage("com.example")
            .generateRequestSchemas(false)
            .generateResponseSchemas(false)
            .includeSecuritySchemes(false)
            .failOnValidationError(false)
            .build();

        assertThat(config.getApiTitle()).isEqualTo("My API");
        assertThat(config.getApiVersion()).isEqualTo("2.0.0");
        assertThat(config.getApiDescription()).isEqualTo("Test API");
        assertThat(config.getTermsOfService()).isEqualTo("https://example.com/terms");
        assertThat(config.getContactName()).isEqualTo("Support");
        assertThat(config.getContactEmail()).isEqualTo("support@example.com");
        assertThat(config.getContactUrl()).isEqualTo("https://example.com/support");
        assertThat(config.getLicenseName()).isEqualTo("Apache 2.0");
        assertThat(config.getLicenseUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
        assertThat(config.getServers()).containsExactly(server);
        assertThat(config.getScanPackages()).containsExactly("com.example");
        assertThat(config.isGenerateRequestSchemas()).isFalse();
        assertThat(config.isGenerateResponseSchemas()).isFalse();
        assertThat(config.isIncludeSecuritySchemes()).isFalse();
        assertThat(config.isFailOnValidationError()).isFalse();
    }

    @Test
    void shouldRejectNullApiTitle() {
        assertThatThrownBy(() -> PluginConfiguration.builder().apiTitle(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("apiTitle");
    }

    @Test
    void shouldRejectNullApiVersion() {
        assertThatThrownBy(() -> PluginConfiguration.builder().apiVersion(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("apiVersion");
    }

    @Test
    void serversListShouldBeImmutable() {
        PluginConfiguration config = PluginConfiguration.builder()
            .addServer(new ServerConfig("https://api.example.com", "Production"))
            .build();

        assertThatThrownBy(() -> config.getServers().add(new ServerConfig("https://test.com", "Test")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void scanPackagesListShouldBeImmutable() {
        PluginConfiguration config = PluginConfiguration.builder()
            .addScanPackage("com.example")
            .build();

        assertThatThrownBy(() -> config.getScanPackages().add("com.other"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
