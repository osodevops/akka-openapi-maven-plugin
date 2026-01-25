# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release of Akka OpenAPI Maven Plugin
- OpenAPI 3.1 specification generation from Akka SDK `@HttpEndpoint` annotations
- Automatic schema generation from Java POJOs using jsonschema-generator
- Support for Akka SDK HTTP method annotations (`@Get`, `@Post`, `@Put`, `@Delete`, `@Patch`)
- Path parameter extraction from URL patterns
- Request body inference from complex type parameters
- Response schema generation from method return types
- Custom OpenAPI annotations for enhanced documentation:
  - `@OpenAPITag` for endpoint grouping
  - `@OpenAPIResponse` for documenting response codes
  - `@OpenAPIInfo` for API metadata
  - `@OpenAPIExample` for request/response examples
- Jakarta Validation annotation support for schema constraints
- Jackson annotation support for property naming and formatting
- Server configuration in plugin settings
- YAML and JSON output format options
- OpenAPI specification validation using swagger-parser
- Circular reference detection and handling
- Example project demonstrating plugin usage

### Dependencies
- Akka SDK 3.0.2
- Swagger Core 2.2.25
- Swagger Parser 2.1.22
- ClassGraph 4.8.182
- jsonschema-generator 4.38.0
- Jackson 2.18.2
- Java 17+

## [1.0.0] - TBD

Initial public release.

[Unreleased]: https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/osodevops/akka-openapi-maven-plugin/releases/tag/v1.0.0
