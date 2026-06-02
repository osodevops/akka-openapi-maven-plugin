# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6.0](https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.5.0...v1.6.0) (2026-06-02)


### Features

* add Akka SDK shim for compile-time compatibility ([80377d6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/80377d67f9e7bb1ef89953b68df3d1f0a968b0df))
* initial release of Akka OpenAPI Maven Plugin ([77b75f2](https://github.com/osodevops/akka-openapi-maven-plugin/commit/77b75f2fbc9fee23bdd6e3ea8605ed5405e549fe))
* Query parameter support and nested component deduplication ([16c71f7](https://github.com/osodevops/akka-openapi-maven-plugin/commit/16c71f74f695aa0c7084613e3b7f25b557bdc6c8))
* **security:** honour includeSecuritySchemes flag ([10455ee](https://github.com/osodevops/akka-openapi-maven-plugin/commit/10455eee8f6f663b5a925d5f2d1c21ac8720e924))
* **security:** Mojo-side validation for missing schemeName/name ([8b491eb](https://github.com/osodevops/akka-openapi-maven-plugin/commit/8b491ebc343fbd82cd425acd29de6ee8e897c423))
* **security:** restrict scheme types to apiKey with explicit validation ([f16d5ef](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f16d5ef0f30815545cc0198eb3d411f241640c36))
* support Optional and JsonValue wrapper schemas ([5cfa33b](https://github.com/osodevops/akka-openapi-maven-plugin/commit/5cfa33b879a817813c322d2a55a840b2a2ab685c))
* support Optional and JsonValue wrapper schemas ([b81edd4](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b81edd4c7ec40c2db3467d9fc3415a3090a60059))


### Bug Fixes

* add Akka SDK repository for dependency resolution ([9f7ee53](https://github.com/osodevops/akka-openapi-maven-plugin/commit/9f7ee53a89af7714be0cdb10d3a86c01fa50316e))
* auto-publish releases end-to-end ([b9077f6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b9077f6eea01f8e084af6f44151c94ae71d1b7cd))
* auto-publish releases end-to-end, no manual click needed ([d2df6e3](https://github.com/osodevops/akka-openapi-maven-plugin/commit/d2df6e38caba3bd555c4117dc07f9299b2aa5a23))
* **ci:** skip invoker ITs in coverage job ([923d729](https://github.com/osodevops/akka-openapi-maven-plugin/commit/923d729e498db2b7843bbe36715d003cf9421bcc))
* **ci:** skip invoker ITs in coverage job (jacoco/argLine collision) ([f07e45a](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f07e45a8f9ae49e65d0695de5bd6e00f45455f58))
* close openAPISummaryShouldTargetMethodOnly test method ([b3c1a5c](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b3c1a5c1466d9be4dd52ac84110d8fbbad33e93d))
* correct release workflow trigger conditions ([50ea8e7](https://github.com/osodevops/akka-openapi-maven-plugin/commit/50ea8e788c667adb267417b1b6c709f12b27ad69))
* **docs:** update published usage examples ([9e0fff9](https://github.com/osodevops/akka-openapi-maven-plugin/commit/9e0fff9b52418eb353fe5892a2e3783cb5906ed5))
* **docs:** update published usage examples ([2f125a2](https://github.com/osodevops/akka-openapi-maven-plugin/commit/2f125a27df4ff0375886f4066cf0ad756dc41b34))
* document raw Akka HttpResponse schemas ([36433b3](https://github.com/osodevops/akka-openapi-maven-plugin/commit/36433b3076c01fd1d8d52ba59d85a26b2871f7e8))
* pin actions to commit SHAs, fix deprecated set-output ([1dd351e](https://github.com/osodevops/akka-openapi-maven-plugin/commit/1dd351e5f9227ebf05ebcad6c4a2c622fd381fe4))
* pin GitHub Actions to immutable commit SHAs ([601f705](https://github.com/osodevops/akka-openapi-maven-plugin/commit/601f705fab81f3f8d706e17b398cca04356fa221))
* pin SonarSource action to commit SHA ([aa6e6ae](https://github.com/osodevops/akka-openapi-maven-plugin/commit/aa6e6aea76550f3bb32de585465f94677aecde30))
* **release:** publish Maven Central releases from release-please ([f6a9953](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f6a995373b9afefbe79ea9bb210fbca8788b7957))
* **release:** publish Maven Central releases from release-please ([6556db6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/6556db6b3dc0ac7a1401a4bc30efeaf89d0f0edc))
* resolve merge-conflict markers in AnnotationRetentionTest ([52b62ce](https://github.com/osodevops/akka-openapi-maven-plugin/commit/52b62cec62f0b7939b570da53332537341469f13))
* rewrite verify-release-secrets expiry check in bash ([0e1aeb5](https://github.com/osodevops/akka-openapi-maven-plugin/commit/0e1aeb594c77ee3198b941e7b629f4c51faf395e))
* rewrite verify-release-secrets expiry check in bash ([2787ed6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/2787ed649815b136da2398ab8aca2e85953f2737))
* **schema:** retain Jakarta constraints on @JsonValue and title polymorphic subtypes ([c32885a](https://github.com/osodevops/akka-openapi-maven-plugin/commit/c32885a794d91fe8be66a1956762ff13af0687e7))
* **schema:** retain Jakarta constraints on @JsonValue, title polymorphic subtypes ([7ab9c83](https://github.com/osodevops/akka-openapi-maven-plugin/commit/7ab9c83a48bdec9ec1b9a0c2fffe210bcc68555d))
* switch to central-publishing-maven-plugin for Central Portal ([9f93488](https://github.com/osodevops/akka-openapi-maven-plugin/commit/9f9348879f64d4a23267b185c508422ceed75f94))
* update to use Sonatype Central Portal URLs ([61b1a99](https://github.com/osodevops/akka-openapi-maven-plugin/commit/61b1a99f6b6bdffd9f0bfa22604f4dda6037f39f))
* use GPG agent instead of Bouncy Castle for signing ([1db8350](https://github.com/osodevops/akka-openapi-maven-plugin/commit/1db8350f2d4f0382a9ce6d8bb16b46aaa82b2ac0))
* use mvn verify instead of compile for release validation ([b4eed25](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b4eed2544475526cb568a82ad1c6a095da979fd1))
* wire up OpenAPI annotation extraction ([53c08c6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/53c08c67a10b96ba2e868ab537fe66e2af19ba41))
* wire up OpenAPI annotation extraction ([#17](https://github.com/osodevops/akka-openapi-maven-plugin/issues/17)) ([9f702f9](https://github.com/osodevops/akka-openapi-maven-plugin/commit/9f702f9a2ddf40edfc5162d9952578df2565d218))


### Documentation

* document &lt;security&gt; block + @OpenAPISummary; expose includeSecuritySchemes flag ([dccdaee](https://github.com/osodevops/akka-openapi-maven-plugin/commit/dccdaee86fe659ce52f231aa6e93684aa171d42c))
* keep v1.4.0 references in README usage examples ([0012a12](https://github.com/osodevops/akka-openapi-maven-plugin/commit/0012a122faa911cc3d748e7c8e3bd2fe5180bdb0))
* prepare CHANGELOG for v1.0.1 release ([910ca5c](https://github.com/osodevops/akka-openapi-maven-plugin/commit/910ca5c44827f0c8808fea36f64df1d6ced349e8))
* update usage examples for v1.3.0 ([888d385](https://github.com/osodevops/akka-openapi-maven-plugin/commit/888d385695fa4d222da0bf7d1eb9ca26e295474a))
* update v1.3.0 usage examples ([745b46b](https://github.com/osodevops/akka-openapi-maven-plugin/commit/745b46b25833bf671667ff1ffee15e9b4105274e))
* update v1.4.0 usage examples ([cc7c983](https://github.com/osodevops/akka-openapi-maven-plugin/commit/cc7c983a9baa567809f36653ade400b6beb6ca33))
* update v1.4.0 usage examples ([c7a5084](https://github.com/osodevops/akka-openapi-maven-plugin/commit/c7a5084e0707a07facd9bd43d8a6badcf4e2ac4b))

## [Unreleased](https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.5.0...HEAD)

## [1.5.0](https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.4.0...v1.5.0) (2026-06-02)

### Features

* add `@OpenAPIQueryParam` for explicit dynamic query parameter documentation
* deduplicate nested component schemas when reused through response graphs

### Bug Fixes

* retain Jakarta constraints on `@JsonValue` wrapper schemas
* add titles to polymorphic subtype schemas
* mark nested non-optional fields as required
* keep README usage examples aligned with the published version

## [1.4.0](https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.3.0...v1.4.0) (2026-05-06)

### Features

* support `Optional` and `@JsonValue` wrapper schemas
* support optional deserialisation of records

### Documentation

* update usage examples for the published release

## [1.3.0](https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.2.0...v1.3.0) (2026-05-06)


### Features

* add Akka SDK shim for compile-time compatibility ([80377d6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/80377d67f9e7bb1ef89953b68df3d1f0a968b0df))
* initial release of Akka OpenAPI Maven Plugin ([77b75f2](https://github.com/osodevops/akka-openapi-maven-plugin/commit/77b75f2fbc9fee23bdd6e3ea8605ed5405e549fe))
* **security:** honour includeSecuritySchemes flag ([10455ee](https://github.com/osodevops/akka-openapi-maven-plugin/commit/10455eee8f6f663b5a925d5f2d1c21ac8720e924))
* **security:** Mojo-side validation for missing schemeName/name ([8b491eb](https://github.com/osodevops/akka-openapi-maven-plugin/commit/8b491ebc343fbd82cd425acd29de6ee8e897c423))
* **security:** restrict scheme types to apiKey with explicit validation ([f16d5ef](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f16d5ef0f30815545cc0198eb3d411f241640c36))


### Bug Fixes

* add Akka SDK repository for dependency resolution ([9f7ee53](https://github.com/osodevops/akka-openapi-maven-plugin/commit/9f7ee53a89af7714be0cdb10d3a86c01fa50316e))
* auto-publish releases end-to-end ([b9077f6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b9077f6eea01f8e084af6f44151c94ae71d1b7cd))
* auto-publish releases end-to-end, no manual click needed ([d2df6e3](https://github.com/osodevops/akka-openapi-maven-plugin/commit/d2df6e38caba3bd555c4117dc07f9299b2aa5a23))
* **ci:** skip invoker ITs in coverage job ([923d729](https://github.com/osodevops/akka-openapi-maven-plugin/commit/923d729e498db2b7843bbe36715d003cf9421bcc))
* **ci:** skip invoker ITs in coverage job (jacoco/argLine collision) ([f07e45a](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f07e45a8f9ae49e65d0695de5bd6e00f45455f58))
* close openAPISummaryShouldTargetMethodOnly test method ([b3c1a5c](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b3c1a5c1466d9be4dd52ac84110d8fbbad33e93d))
* correct release workflow trigger conditions ([50ea8e7](https://github.com/osodevops/akka-openapi-maven-plugin/commit/50ea8e788c667adb267417b1b6c709f12b27ad69))
* **docs:** update published usage examples ([9e0fff9](https://github.com/osodevops/akka-openapi-maven-plugin/commit/9e0fff9b52418eb353fe5892a2e3783cb5906ed5))
* **docs:** update published usage examples ([2f125a2](https://github.com/osodevops/akka-openapi-maven-plugin/commit/2f125a27df4ff0375886f4066cf0ad756dc41b34))
* document raw Akka HttpResponse schemas ([36433b3](https://github.com/osodevops/akka-openapi-maven-plugin/commit/36433b3076c01fd1d8d52ba59d85a26b2871f7e8))
* pin actions to commit SHAs, fix deprecated set-output ([1dd351e](https://github.com/osodevops/akka-openapi-maven-plugin/commit/1dd351e5f9227ebf05ebcad6c4a2c622fd381fe4))
* pin GitHub Actions to immutable commit SHAs ([601f705](https://github.com/osodevops/akka-openapi-maven-plugin/commit/601f705fab81f3f8d706e17b398cca04356fa221))
* pin SonarSource action to commit SHA ([aa6e6ae](https://github.com/osodevops/akka-openapi-maven-plugin/commit/aa6e6aea76550f3bb32de585465f94677aecde30))
* **release:** publish Maven Central releases from release-please ([f6a9953](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f6a995373b9afefbe79ea9bb210fbca8788b7957))
* **release:** publish Maven Central releases from release-please ([6556db6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/6556db6b3dc0ac7a1401a4bc30efeaf89d0f0edc))
* resolve merge-conflict markers in AnnotationRetentionTest ([52b62ce](https://github.com/osodevops/akka-openapi-maven-plugin/commit/52b62cec62f0b7939b570da53332537341469f13))
* rewrite verify-release-secrets expiry check in bash ([0e1aeb5](https://github.com/osodevops/akka-openapi-maven-plugin/commit/0e1aeb594c77ee3198b941e7b629f4c51faf395e))
* rewrite verify-release-secrets expiry check in bash ([2787ed6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/2787ed649815b136da2398ab8aca2e85953f2737))
* switch to central-publishing-maven-plugin for Central Portal ([9f93488](https://github.com/osodevops/akka-openapi-maven-plugin/commit/9f9348879f64d4a23267b185c508422ceed75f94))
* update to use Sonatype Central Portal URLs ([61b1a99](https://github.com/osodevops/akka-openapi-maven-plugin/commit/61b1a99f6b6bdffd9f0bfa22604f4dda6037f39f))
* use GPG agent instead of Bouncy Castle for signing ([1db8350](https://github.com/osodevops/akka-openapi-maven-plugin/commit/1db8350f2d4f0382a9ce6d8bb16b46aaa82b2ac0))
* use mvn verify instead of compile for release validation ([b4eed25](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b4eed2544475526cb568a82ad1c6a095da979fd1))
* wire up OpenAPI annotation extraction ([53c08c6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/53c08c67a10b96ba2e868ab537fe66e2af19ba41))
* wire up OpenAPI annotation extraction ([#17](https://github.com/osodevops/akka-openapi-maven-plugin/issues/17)) ([9f702f9](https://github.com/osodevops/akka-openapi-maven-plugin/commit/9f702f9a2ddf40edfc5162d9952578df2565d218))


### Documentation

* document &lt;security&gt; block + @OpenAPISummary; expose includeSecuritySchemes flag ([dccdaee](https://github.com/osodevops/akka-openapi-maven-plugin/commit/dccdaee86fe659ce52f231aa6e93684aa171d42c))
* prepare CHANGELOG for v1.0.1 release ([910ca5c](https://github.com/osodevops/akka-openapi-maven-plugin/commit/910ca5c44827f0c8808fea36f64df1d6ced349e8))

## [1.2.0](https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.1.0...v1.2.0) (2026-05-06)

### Features

* generate deterministic OpenAPI output by sorting paths and schemas
* generate Jackson polymorphic interface schemas with discriminator mappings
* add `stripServerPathPrefix` configuration for server-path de-duplication
* add notification example covering polymorphic request and response schemas

### Bug Fixes

* avoid duplicate numeric subtype components when polymorphic schemas are revisited through collections
* include discriminator properties on subtype schemas for client-generation compatibility
* apply server-path stripping to annotation-provided servers and prefer the longest matching prefix

## [1.1.0](https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.0.1...v1.1.0) (2026-05-05)


### Features

* **security:** honour includeSecuritySchemes flag ([10455ee](https://github.com/osodevops/akka-openapi-maven-plugin/commit/10455eee8f6f663b5a925d5f2d1c21ac8720e924))
* **security:** Mojo-side validation for missing schemeName/name ([8b491eb](https://github.com/osodevops/akka-openapi-maven-plugin/commit/8b491ebc343fbd82cd425acd29de6ee8e897c423))
* **security:** restrict scheme types to apiKey with explicit validation ([f16d5ef](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f16d5ef0f30815545cc0198eb3d411f241640c36))


### Bug Fixes

* auto-publish releases end-to-end ([b9077f6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b9077f6eea01f8e084af6f44151c94ae71d1b7cd))
* auto-publish releases end-to-end, no manual click needed ([d2df6e3](https://github.com/osodevops/akka-openapi-maven-plugin/commit/d2df6e38caba3bd555c4117dc07f9299b2aa5a23))
* **ci:** skip invoker ITs in coverage job ([923d729](https://github.com/osodevops/akka-openapi-maven-plugin/commit/923d729e498db2b7843bbe36715d003cf9421bcc))
* **ci:** skip invoker ITs in coverage job (jacoco/argLine collision) ([f07e45a](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f07e45a8f9ae49e65d0695de5bd6e00f45455f58))
* close openAPISummaryShouldTargetMethodOnly test method ([b3c1a5c](https://github.com/osodevops/akka-openapi-maven-plugin/commit/b3c1a5c1466d9be4dd52ac84110d8fbbad33e93d))
* document raw Akka HttpResponse schemas ([36433b3](https://github.com/osodevops/akka-openapi-maven-plugin/commit/36433b3076c01fd1d8d52ba59d85a26b2871f7e8))
* **release:** publish Maven Central releases from release-please ([f6a9953](https://github.com/osodevops/akka-openapi-maven-plugin/commit/f6a995373b9afefbe79ea9bb210fbca8788b7957))
* **release:** publish Maven Central releases from release-please ([6556db6](https://github.com/osodevops/akka-openapi-maven-plugin/commit/6556db6b3dc0ac7a1401a4bc30efeaf89d0f0edc))
* resolve merge-conflict markers in AnnotationRetentionTest ([52b62ce](https://github.com/osodevops/akka-openapi-maven-plugin/commit/52b62cec62f0b7939b570da53332537341469f13))


### Documentation

* document &lt;security&gt; block + @OpenAPISummary; expose includeSecuritySchemes flag ([dccdaee](https://github.com/osodevops/akka-openapi-maven-plugin/commit/dccdaee86fe659ce52f231aa6e93684aa171d42c))

## [1.0.1] - 2026-04-20

### Fixed
- OpenAPI annotation extraction is now wired up end-to-end. The `@OpenAPITag`,
  `@OpenAPIResponse`, `@OpenAPIInfo`, and `@OpenAPIExample` annotations
  shipped in 1.0.0 were not actually read during generation; they now
  populate the generated spec as documented ([#17], [#18]).

### Security
- Pinned all GitHub Actions to immutable commit SHAs to mitigate supply-chain
  risk from mutable tags ([#19]).

### CI
- Granted `checks: write` / `pull-requests: write` permissions to the CI
  workflow so that test-result publishing no longer fails with a 403 on push
  to `main`.

## [1.0.0] - 2026-01-25

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
- Akka SDK 3.5.17
- Swagger Core 2.2.25
- Swagger Parser 2.1.22
- ClassGraph 4.8.182
- jsonschema-generator 4.38.0
- Jackson 2.18.2
- Java 17+

[1.0.1]: https://github.com/osodevops/akka-openapi-maven-plugin/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/osodevops/akka-openapi-maven-plugin/releases/tag/v1.0.0
[#17]: https://github.com/osodevops/akka-openapi-maven-plugin/issues/17
[#18]: https://github.com/osodevops/akka-openapi-maven-plugin/pull/18
[#19]: https://github.com/osodevops/akka-openapi-maven-plugin/pull/19
