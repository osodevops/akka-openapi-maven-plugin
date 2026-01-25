# Troubleshooting Guide

This guide helps you diagnose and resolve common issues with the Akka OpenAPI Maven Plugin.

## Common Issues

### No Endpoints Found

**Symptom:** The plugin outputs "No @HttpEndpoint annotated classes found"

**Causes and Solutions:**

1. **Classes not compiled**
   ```bash
   # Ensure classes are compiled before the plugin runs
   mvn clean compile
   ```

2. **Wrong package scanning**
   ```xml
   <configuration>
       <scanPackages>
           <package>com.example.endpoints</package>  <!-- Check this matches your package -->
       </scanPackages>
   </configuration>
   ```

3. **Missing Akka SDK dependency**
   ```xml
   <dependency>
       <groupId>io.akka</groupId>
       <artifactId>akka-javasdk</artifactId>
       <version>3.0.2</version>
   </dependency>
   ```

4. **Classes in test scope**
   - The plugin only scans main source classes, not test classes
   - Move endpoint classes to `src/main/java`

### Plugin Descriptor Not Found

**Symptom:** Error message "No plugin descriptor found at META-INF/maven/plugin.xml"

**Cause:** Running `mvn compile` in a multi-module project without installing the plugin first.

**Solution:**
```bash
# Install all modules first
mvn clean install -DskipTests

# Or build from the parent project
mvn clean compile -N && mvn compile
```

### Class Loading Errors

**Symptom:** "Failed to load class" or "ClassNotFoundException" warnings

**Causes and Solutions:**

1. **Missing dependencies**
   - Ensure all required dependencies are in compile scope
   - Check for transitive dependency conflicts

2. **Dependency version conflicts**
   ```bash
   # Check for dependency conflicts
   mvn dependency:tree
   ```

3. **Circular dependencies**
   - The plugin handles most circular references
   - If issues persist, simplify your class hierarchy

### Validation Errors

**Symptom:** "OpenAPI validation failed" error

**Common Validation Issues:**

1. **Missing required fields**
   ```xml
   <configuration>
       <apiTitle>My API</apiTitle>      <!-- Required -->
       <apiVersion>1.0.0</apiVersion>   <!-- Required -->
   </configuration>
   ```

2. **Invalid schema references**
   - Check that all referenced types are accessible
   - Ensure DTOs are in scanned packages

3. **Duplicate operationIds**
   - Each operation must have a unique operationId
   - Rename methods if duplicates occur

**To see detailed validation errors:**
```bash
mvn compile -X 2>&1 | grep -A10 "validation"
```

**To skip validation temporarily:**
```xml
<configuration>
    <failOnValidationError>false</failOnValidationError>
</configuration>
```

### Schema Generation Issues

**Symptom:** Incorrect or missing schema properties

**Solutions:**

1. **Add Jackson annotations for proper naming**
   ```java
   @JsonProperty("firstName")
   private String firstName;
   ```

2. **Use Jakarta Validation for constraints**
   ```java
   @NotBlank
   @Size(min = 1, max = 100)
   private String name;
   ```

3. **Check for generic type issues**
   - Use concrete types when possible
   - Avoid raw generic types

### Map Type Issues

**Symptom:** Schema references to "Map" fail validation

**Cause:** Generic Map types require special handling

**Solution:** The plugin sanitizes schema names for Map types. If you see issues:
```java
// Instead of Map<String, Object>, use a concrete type
public class CustomAttributes {
    private String key;
    private String value;
}
```

## Debug Mode

Enable debug output for detailed logging:

```bash
mvn compile -X
```

Key debug messages to look for:
- "Scanning for Akka HTTP endpoints"
- "Found N HTTP endpoint class(es)"
- "Generating schema for: ClassName"
- "OpenAPI specification generated: path/to/file"

## Plugin Execution Phase

By default, the plugin runs during the `compile` phase. To verify it's running:

```bash
mvn compile -X 2>&1 | grep "akka-openapi"
```

To change the phase:
```xml
<executions>
    <execution>
        <phase>process-classes</phase>
        <goals>
            <goal>generate</goal>
        </goals>
    </execution>
</executions>
```

## Classpath Issues

**Symptom:** Dependencies not found during schema generation

**Check classpath:**
```bash
mvn dependency:build-classpath
```

**Ensure compile scope:**
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>shared-types</artifactId>
    <scope>compile</scope>  <!-- Not provided or runtime -->
</dependency>
```

## Multi-Module Projects

For multi-module projects:

1. **Build order matters**
   ```xml
   <modules>
       <module>shared-types</module>    <!-- Build first -->
       <module>api-module</module>      <!-- Then API with plugin -->
   </modules>
   ```

2. **Use reactor build**
   ```bash
   mvn clean install -pl api-module -am
   ```

3. **Plugin should be in API module only**
   - Don't add the plugin to parent POM
   - Add it to the module with endpoints

## Jackson Compatibility

The plugin uses Jackson 2.18.x. If you have version conflicts:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.18.2</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Akka SDK Version

The plugin is tested with Akka SDK 3.0.2. For other versions:

1. Check annotation package names haven't changed
2. Verify HTTP annotations are available:
   ```java
   import akka.javasdk.annotations.http.HttpEndpoint;
   import akka.javasdk.annotations.http.Get;
   ```

## Getting Help

If you can't resolve your issue:

1. **Check existing issues**
   - [GitHub Issues](https://github.com/osodevops/akka-openapi-maven-plugin/issues)

2. **Create a new issue with**
   - Java version (`java -version`)
   - Maven version (`mvn -version`)
   - Akka SDK version
   - Full error message
   - Minimal reproducing example

3. **Include debug output**
   ```bash
   mvn compile -X > debug.log 2>&1
   ```

## See Also

- [Getting Started](GETTING_STARTED.md)
- [Configuration Reference](CONFIGURATION.md)
- [Examples](EXAMPLES.md)
