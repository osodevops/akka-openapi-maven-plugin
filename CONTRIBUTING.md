# Contributing to Akka OpenAPI Maven Plugin

Thank you for your interest in contributing to the Akka OpenAPI Maven Plugin! This document provides guidelines and instructions for contributing.

## Code of Conduct

This project adheres to a Code of Conduct. By participating, you are expected to uphold this code.

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- **Clear title** describing the issue
- **Steps to reproduce** the behavior
- **Expected behavior** vs. what actually happened
- **Environment details** (Java version, Maven version, Akka SDK version)
- **Sample code** if applicable
- **Generated OpenAPI output** if relevant

### Suggesting Enhancements

Enhancement suggestions are welcome! Please include:

- **Use case** - Why is this enhancement needed?
- **Proposed solution** - How should it work?
- **Alternatives considered** - What other approaches did you consider?

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Write tests** for any new functionality
3. **Follow the code style** of the existing codebase
4. **Update documentation** if needed
5. **Ensure all tests pass** with `mvn verify`
6. **Submit your pull request**

## Development Setup

### Prerequisites

- JDK 17 or later
- Maven 3.6.3 or later
- Git

### Building

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/akka-openapi-maven-plugin.git
cd akka-openapi-maven-plugin

# Build the project
mvn clean install

# Run tests only
mvn test

# Run integration tests
mvn verify -pl akka-openapi-maven-plugin
```

### Project Structure

```
akka-openapi-maven-plugin/
├── akka-openapi-annotations/    # Custom OpenAPI annotations
├── akka-openapi-core/           # Core generation logic
├── akka-openapi-maven-plugin/   # Maven plugin
└── akka-openapi-example/        # Example project
```

### Running the Example

```bash
mvn compile -pl akka-openapi-example
cat akka-openapi-example/target/openapi.yaml
```

## Code Style

- Use 4 spaces for indentation (no tabs)
- Follow standard Java naming conventions
- Add Javadoc to all public classes and methods
- Keep methods focused and under 30 lines when possible
- Write self-documenting code with meaningful names

## Testing Guidelines

- Write unit tests for all new functionality
- Use descriptive test method names
- Test edge cases and error conditions
- Integration tests go in `src/it/` directory

### Test Naming Convention

```java
@Test
void shouldGenerateSchemaForSimplePojo() { ... }

@Test
void shouldThrowExceptionWhenClassNotFound() { ... }
```

## Commit Messages

Follow conventional commit format:

```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Adding or updating tests
- `refactor`: Code refactoring
- `chore`: Maintenance tasks

Examples:
```
feat(scanner): add support for @Patch annotation
fix(schema): handle circular references in nested types
docs: update configuration examples in README
```

## Release Process

Releases are managed by maintainers. If you believe a release is needed:

1. Check that all tests pass
2. Ensure documentation is up to date
3. Open an issue requesting a release

## Questions?

Feel free to open an issue for any questions about contributing.

Thank you for contributing!
