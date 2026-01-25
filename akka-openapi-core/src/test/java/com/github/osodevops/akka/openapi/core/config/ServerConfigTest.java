package com.github.osodevops.akka.openapi.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ServerConfig.
 */
class ServerConfigTest {

    @Test
    void shouldCreateWithConstructor() {
        ServerConfig config = new ServerConfig("https://api.example.com", "Production");

        assertThat(config.getUrl()).isEqualTo("https://api.example.com");
        assertThat(config.getDescription()).isEqualTo("Production");
    }

    @Test
    void shouldCreateWithDefaultConstructorAndSetters() {
        ServerConfig config = new ServerConfig();
        config.setUrl("https://api.example.com");
        config.setDescription("Production");

        assertThat(config.getUrl()).isEqualTo("https://api.example.com");
        assertThat(config.getDescription()).isEqualTo("Production");
    }

    @Test
    void shouldRejectNullUrlInConstructor() {
        assertThatThrownBy(() -> new ServerConfig(null, "Production"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("url");
    }

    @Test
    void shouldAllowNullDescription() {
        ServerConfig config = new ServerConfig("https://api.example.com", null);

        assertThat(config.getUrl()).isEqualTo("https://api.example.com");
        assertThat(config.getDescription()).isNull();
    }

    @Test
    void shouldImplementEquals() {
        ServerConfig config1 = new ServerConfig("https://api.example.com", "Production");
        ServerConfig config2 = new ServerConfig("https://api.example.com", "Production");
        ServerConfig config3 = new ServerConfig("https://api.example.com", "Staging");

        assertThat(config1).isEqualTo(config2);
        assertThat(config1).isNotEqualTo(config3);
    }

    @Test
    void shouldImplementHashCode() {
        ServerConfig config1 = new ServerConfig("https://api.example.com", "Production");
        ServerConfig config2 = new ServerConfig("https://api.example.com", "Production");

        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        ServerConfig config = new ServerConfig("https://api.example.com", "Production");

        assertThat(config.toString())
            .contains("https://api.example.com")
            .contains("Production");
    }
}
