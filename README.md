# Akka OpenAPI Maven Plugin

[![Maven Central](https://img.shields.io/maven-central/v/com.github.osodevops/akka-openapi-maven-plugin.svg)](https://search.maven.org/artifact/com.github.osodevops/akka-openapi-maven-plugin)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build](https://github.com/osodevops/akka-openapi-maven-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/osodevops/akka-openapi-maven-plugin/actions/workflows/ci.yml)

Generate OpenAPI 3.1 specifications from [Akka SDK](https://doc.akka.io/sdk/) HTTP endpoint annotations at compile time.

## Features

- **Zero Configuration** - Works out-of-the-box with sensible defaults
- **Compile-Time Generation** - No runtime overhead, perfect for serverless/containers
- **OpenAPI 3.1** - Latest specification with full JSON Schema support
- **Automatic Schema Generation** - POJOs converted to JSON schemas automatically
- **JavaDoc Extraction** - Uses your existing documentation for descriptions
- **Validation** - Ensures generated specs are valid before writing

## Quick Start

Add the plugin to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.osodevops</groupId>
            <artifactId>akka-openapi-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Run:

```bash
mvn compile
```

Your OpenAPI specification will be generated at `target/openapi.yaml`.

## Example

Given an Akka SDK endpoint:

```java
/**
 * Customer management endpoint.
 */
@HttpEndpoint("/customers")
public class CustomerEndpoint {

    /**
     * Get a customer by ID.
     * @param id the customer unique identifier
     * @return the customer or 404 if not found
     */
    @Get("/{id}")
    public Customer getCustomer(String id) {
        // ...
    }

    /**
     * Create a new customer.
     */
    @Post
    public Customer createCustomer(CreateCustomerRequest request) {
        // ...
    }
}
```

The plugin generates:

```yaml
openapi: 3.1.0
info:
  title: My API
  version: 1.0.0
paths:
  /customers/{id}:
    get:
      summary: Get a customer by ID.
      operationId: getCustomer
      parameters:
        - name: id
          in: path
          required: true
          description: the customer unique identifier
          schema:
            type: string
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Customer'
        '404':
          description: Not Found
  /customers:
    post:
      summary: Create a new customer.
      operationId: createCustomer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateCustomerRequest'
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Customer'
components:
  schemas:
    Customer:
      type: object
      properties:
        id:
          type: string
        name:
          type: string
        email:
          type: string
          format: email
    CreateCustomerRequest:
      type: object
      required:
        - name
        - email
      properties:
        name:
          type: string
        email:
          type: string
          format: email
```

## Configuration

```xml
<plugin>
    <groupId>com.github.osodevops</groupId>
    <artifactId>akka-openapi-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Output settings -->
        <outputFile>${project.build.directory}/openapi.yaml</outputFile>
        <outputFormat>yaml</outputFormat> <!-- yaml or json -->

        <!-- API metadata -->
        <apiTitle>${project.name}</apiTitle>
        <apiVersion>${project.version}</apiVersion>
        <apiDescription>${project.description}</apiDescription>

        <!-- Package scanning -->
        <scanPackages>
            <package>com.example.endpoints</package>
        </scanPackages>

        <!-- Server definitions -->
        <servers>
            <server>
                <url>https://api.example.com</url>
                <description>Production</description>
            </server>
        </servers>

        <!-- Validation -->
        <failOnValidationError>true</failOnValidationError>

        <!-- Skip generation -->
        <skip>false</skip>
    </configuration>
</plugin>
```

## Supported Akka SDK Annotations

| Annotation | OpenAPI Mapping |
|------------|-----------------|
| `@HttpEndpoint(path)` | Base path for all operations |
| `@Get`, `@Post`, `@Put`, `@Delete`, `@Patch` | HTTP methods |
| Path parameters (e.g., `/{id}`) | `parameters[in=path]` |
| Last complex type parameter | `requestBody` |
| Method return type | Response schema |
| JavaDoc comments | `summary` and `description` |

## Custom Annotations

For additional control, use the optional custom annotations:

```java
@HttpEndpoint("/customers")
@OpenAPITag(name = "Customers", description = "Customer management")
public class CustomerEndpoint {

    @Get("/{id}")
    @OpenAPIResponse(status = "200", description = "Customer found")
    @OpenAPIResponse(status = "404", description = "Customer not found")
    public Customer getCustomer(String id) {
        // ...
    }
}
```

## Documentation

- [Getting Started](docs/GETTING_STARTED.md)
- [Configuration Reference](docs/CONFIGURATION.md)
- [Examples](docs/EXAMPLES.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)

## Requirements

- Java 17 or later
- Maven 3.6.3 or later
- Akka SDK 3.0.0 or later

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Akka SDK](https://doc.akka.io/sdk/) - The Akka platform for building reactive applications
- [Swagger Core](https://github.com/swagger-api/swagger-core) - OpenAPI implementation for Java
- [ClassGraph](https://github.com/classgraph/classgraph) - Fast classpath scanner
