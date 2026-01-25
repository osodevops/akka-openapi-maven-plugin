# Getting Started with Akka OpenAPI Maven Plugin

This guide walks you through setting up the Akka OpenAPI Maven Plugin to generate OpenAPI specifications from your Akka SDK endpoints.

## Prerequisites

Before you begin, ensure you have:

- **Java 17** or later
- **Maven 3.6.3** or later
- An Akka SDK project with HTTP endpoints

## Installation

### Step 1: Add the Plugin

Add the plugin to your project's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>sh.oso</groupId>
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

### Step 2: (Optional) Add Custom Annotations

For enhanced documentation, add the annotations dependency:

```xml
<dependencies>
    <dependency>
        <groupId>sh.oso</groupId>
        <artifactId>akka-openapi-annotations</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Your First OpenAPI Specification

### Step 1: Create an Endpoint

Create an Akka SDK HTTP endpoint:

```java
package com.example.endpoints;

import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;

/**
 * Greeting endpoint for the application.
 */
@HttpEndpoint("/greetings")
public class GreetingEndpoint {

    /**
     * Returns a greeting message.
     *
     * @param name the name to greet
     * @return a personalized greeting
     */
    @Get("/{name}")
    public Greeting greet(String name) {
        return new Greeting("Hello, " + name + "!");
    }

    /**
     * Creates a new greeting.
     */
    @Post
    public Greeting createGreeting(CreateGreetingRequest request) {
        return new Greeting(request.message());
    }
}
```

### Step 2: Define DTOs

Create your data transfer objects:

```java
package com.example.endpoints;

public record Greeting(String message) {}

public record CreateGreetingRequest(String message) {}
```

### Step 3: Generate the Specification

Run Maven compile:

```bash
mvn compile
```

### Step 4: View the Result

The OpenAPI specification is generated at `target/openapi.yaml`:

```bash
cat target/openapi.yaml
```

## Adding API Metadata

Configure API title, version, and description:

```xml
<plugin>
    <groupId>sh.oso</groupId>
    <artifactId>akka-openapi-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <apiTitle>My API</apiTitle>
        <apiVersion>1.0.0</apiVersion>
        <apiDescription>A sample API built with Akka SDK</apiDescription>
        <contactName>API Support</contactName>
        <contactEmail>support@example.com</contactEmail>
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

## Adding Server Information

Define your API servers:

```xml
<configuration>
    <servers>
        <server>
            <url>https://api.example.com</url>
            <description>Production</description>
        </server>
        <server>
            <url>https://staging.example.com</url>
            <description>Staging</description>
        </server>
        <server>
            <url>http://localhost:8080</url>
            <description>Local Development</description>
        </server>
    </servers>
</configuration>
```

## Enhanced Documentation with Custom Annotations

Use the custom annotations for more control:

```java
import sh.oso.akka.openapi.annotations.OpenAPITag;
import sh.oso.akka.openapi.annotations.OpenAPIResponse;

@HttpEndpoint("/customers")
@OpenAPITag(name = "Customers", description = "Customer management operations")
public class CustomerEndpoint {

    @Get("/{id}")
    @OpenAPIResponse(status = "200", description = "Customer found successfully")
    @OpenAPIResponse(status = "404", description = "Customer not found")
    public Customer getCustomer(String id) {
        // ...
    }
}
```

## Validation Annotations

Use Jakarta Validation annotations for schema constraints:

```java
import jakarta.validation.constraints.*;

public class Customer {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Email
    @NotNull
    private String email;

    @Min(0)
    private int age;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String phone;
}
```

These constraints are automatically converted to OpenAPI schema properties:

```yaml
Customer:
  type: object
  required:
    - name
    - email
  properties:
    name:
      type: string
      maxLength: 100
      minLength: 1
    email:
      type: string
      format: email
    age:
      type: integer
      minimum: 0
    phone:
      type: string
      pattern: "^\\+?[1-9]\\d{1,14}$"
```

## Package Scanning

By default, the plugin scans all packages. To limit scanning to specific packages:

```xml
<configuration>
    <scanPackages>
        <package>com.example.endpoints</package>
        <package>com.example.api</package>
    </scanPackages>
</configuration>
```

## Output Formats

Generate JSON instead of YAML:

```xml
<configuration>
    <outputFormat>json</outputFormat>
    <outputFile>${project.build.directory}/openapi.json</outputFile>
</configuration>
```

## Next Steps

- See [Configuration Reference](CONFIGURATION.md) for all available options
- Check [Examples](EXAMPLES.md) for more complex use cases
- Read [Troubleshooting](TROUBLESHOOTING.md) if you encounter issues
