# Configuration Reference

This document describes all configuration options available for the Akka OpenAPI Maven Plugin.

## Basic Configuration

```xml
<plugin>
    <groupId>sh.oso</groupId>
    <artifactId>akka-openapi-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- Options here -->
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Configuration Options

### Output Settings

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `outputFile` | File | `${project.build.directory}/openapi.yaml` | Path to the generated OpenAPI file |
| `outputFormat` | String | `yaml` | Output format: `yaml` or `json` |
| `skip` | boolean | `false` | Skip plugin execution |
| `failOnValidationError` | boolean | `true` | Fail build if specification is invalid |

**Example:**
```xml
<configuration>
    <outputFile>${project.build.directory}/api/openapi.json</outputFile>
    <outputFormat>json</outputFormat>
    <skip>${skip.openapi}</skip>
    <failOnValidationError>true</failOnValidationError>
</configuration>
```

### API Information

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `apiTitle` | String | `${project.name}` | API title in the specification |
| `apiVersion` | String | `${project.version}` | API version |
| `apiDescription` | String | `${project.description}` | API description |
| `termsOfService` | String | - | URL to the terms of service |

**Example:**
```xml
<configuration>
    <apiTitle>Customer Management API</apiTitle>
    <apiVersion>2.0.0</apiVersion>
    <apiDescription>RESTful API for managing customer records</apiDescription>
    <termsOfService>https://example.com/terms</termsOfService>
</configuration>
```

### Contact Information

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `contactName` | String | - | Name of the contact person/organization |
| `contactEmail` | String | - | Email address for API support |
| `contactUrl` | String | - | URL for more contact information |

**Example:**
```xml
<configuration>
    <contactName>API Support Team</contactName>
    <contactEmail>api-support@example.com</contactEmail>
    <contactUrl>https://example.com/support</contactUrl>
</configuration>
```

### License Information

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `licenseName` | String | - | License name (e.g., "Apache License 2.0") |
| `licenseUrl` | String | - | URL to the license text |

**Example:**
```xml
<configuration>
    <licenseName>Apache License, Version 2.0</licenseName>
    <licenseUrl>https://www.apache.org/licenses/LICENSE-2.0</licenseUrl>
</configuration>
```

### Package Scanning

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `scanPackages` | List | All packages | Packages to scan for endpoints |

**Example:**
```xml
<configuration>
    <scanPackages>
        <package>com.example.api.v1</package>
        <package>com.example.api.v2</package>
        <package>com.example.health</package>
    </scanPackages>
</configuration>
```

**Note:** If no packages are specified, all packages in the project's compile classpath are scanned.

### Server Definitions

Servers are defined as a list of `<server>` elements:

| Element | Type | Required | Description |
|---------|------|----------|-------------|
| `url` | String | Yes | Server URL (can be relative or absolute) |
| `description` | String | No | Human-readable description |

**Example:**
```xml
<configuration>
    <servers>
        <server>
            <url>https://api.example.com/v1</url>
            <description>Production Server</description>
        </server>
        <server>
            <url>https://staging-api.example.com/v1</url>
            <description>Staging Server</description>
        </server>
        <server>
            <url>http://localhost:8080</url>
            <description>Local Development</description>
        </server>
    </servers>
</configuration>
```

## Command Line Properties

All configuration options can be specified on the command line using system properties:

```bash
# Override API title
mvn compile -Dopenapi.apiTitle="My Custom Title"

# Skip generation
mvn compile -Dopenapi.skip=true

# Change output format
mvn compile -Dopenapi.outputFormat=json
```

### Property Mapping

| Configuration | Property |
|--------------|----------|
| `outputFile` | `openapi.outputFile` |
| `outputFormat` | `openapi.outputFormat` |
| `skip` | `openapi.skip` |
| `apiTitle` | `openapi.apiTitle` |
| `apiVersion` | `openapi.apiVersion` |
| `apiDescription` | `openapi.apiDescription` |
| `failOnValidationError` | `openapi.failOnValidationError` |
| `contactName` | `openapi.contactName` |
| `contactEmail` | `openapi.contactEmail` |
| `contactUrl` | `openapi.contactUrl` |
| `licenseName` | `openapi.licenseName` |
| `licenseUrl` | `openapi.licenseUrl` |
| `termsOfService` | `openapi.termsOfService` |

## Full Example

Here's a complete configuration example:

```xml
<plugin>
    <groupId>sh.oso</groupId>
    <artifactId>akka-openapi-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- Output Settings -->
        <outputFile>${project.build.directory}/openapi.yaml</outputFile>
        <outputFormat>yaml</outputFormat>
        <failOnValidationError>true</failOnValidationError>

        <!-- API Information -->
        <apiTitle>E-Commerce API</apiTitle>
        <apiVersion>2.1.0</apiVersion>
        <apiDescription>Complete e-commerce platform API for managing customers, orders, and products</apiDescription>
        <termsOfService>https://api.example.com/terms</termsOfService>

        <!-- Contact Information -->
        <contactName>API Development Team</contactName>
        <contactEmail>api@example.com</contactEmail>
        <contactUrl>https://developer.example.com</contactUrl>

        <!-- License Information -->
        <licenseName>Apache License, Version 2.0</licenseName>
        <licenseUrl>https://www.apache.org/licenses/LICENSE-2.0</licenseUrl>

        <!-- Package Scanning -->
        <scanPackages>
            <package>com.example.api.customer</package>
            <package>com.example.api.order</package>
            <package>com.example.api.product</package>
            <package>com.example.api.health</package>
        </scanPackages>

        <!-- Server Definitions -->
        <servers>
            <server>
                <url>https://api.example.com</url>
                <description>Production</description>
            </server>
            <server>
                <url>https://api-staging.example.com</url>
                <description>Staging</description>
            </server>
            <server>
                <url>http://localhost:8080</url>
                <description>Local Development</description>
            </server>
        </servers>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Maven Profiles

Use Maven profiles for environment-specific configurations:

```xml
<profiles>
    <profile>
        <id>prod-docs</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>sh.oso</groupId>
                    <artifactId>akka-openapi-maven-plugin</artifactId>
                    <configuration>
                        <servers>
                            <server>
                                <url>https://api.example.com</url>
                                <description>Production</description>
                            </server>
                        </servers>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>

    <profile>
        <id>dev-docs</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>sh.oso</groupId>
                    <artifactId>akka-openapi-maven-plugin</artifactId>
                    <configuration>
                        <servers>
                            <server>
                                <url>http://localhost:8080</url>
                                <description>Local Development</description>
                            </server>
                        </servers>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Activate a profile:
```bash
mvn compile -Pprod-docs
```

## Lifecycle Binding

By default, the plugin binds to the `compile` phase. To run in a different phase:

```xml
<executions>
    <execution>
        <phase>prepare-package</phase>
        <goals>
            <goal>generate</goal>
        </goals>
    </execution>
</executions>
```

## See Also

- [Getting Started](GETTING_STARTED.md)
- [Examples](EXAMPLES.md)
- [Troubleshooting](TROUBLESHOOTING.md)
