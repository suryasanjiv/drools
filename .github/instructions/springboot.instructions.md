---
description: "Guidelines for building Spring Boot base applications (Kotlin)"
applyTo: "**/*.kt"
---

# Spring Boot Development

## General Instructions

- Make only high confidence suggestions when reviewing code changes.
- Write code with good maintainability practices, including brief rationale for notable design decisions.
- Handle edge cases and write clear exception handling.
- For libraries or external dependencies, mention their usage and purpose in comments.

## Spring Boot (Kotlin) Instructions

### Dependency Injection

- Use constructor injection for all required dependencies.
- Declare dependency properties as `private val` on the primary constructor or in the class body.

### Configuration

- Use YAML files (`application.yml`) for externalized configuration.
- Do not use environment-specific profiles in this project.
- Configuration Properties: Use `@ConfigurationProperties` (with a Kotlin data class) for type-safe configuration binding.
- Secrets should be injected via environment variables.

### Code Organization

- Package Structure: Organize by feature/domain rather than by layer.
- Separation of Concerns: Keep controllers thin, services focused, and repositories simple.
- Utility Objects: Prefer `object` singletons for stateless utilities, or use `class` with a private constructor when instantiation control is needed.

### Service Layer

- Place business logic in `@Service`-annotated classes written in Kotlin.
- Services should be stateless and testable.
- Inject repositories via constructor parameters (use `private val repository: MyRepository`).
- Service method signatures should accept domain IDs or DTOs and avoid exposing persistence entities unless necessary.

### Logging

- Use SLF4J for all logging. In Kotlin prefer a `companion object` logger, e.g.:

  ```kotlin
  companion object {
  	private val logger = LoggerFactory.getLogger(MyClass::class.java)
  }
  ```

- Do not use `System.out.println()` directly.
- Use parameterized logging: `logger.info("User {} logged in", userId)`.

### Security & Input Handling

- Use parameterized queries and Spring Data repositories to prevent SQL injection.
- Validate request bodies and parameters using JSR-380 annotations. In Kotlin annotate fields with `@field:NotNull`, `@field:Size`, etc., to ensure annotations apply to generated fields.
- Favor Kotlin nullability to express required vs optional values in DTOs and request models.

## Build and Verification

- After adding or modifying code, verify the project continues to build successfully.
- Run `./gradlew build` (or `gradlew.bat build` on Windows).
- Ensure all tests pass as part of the build.

## Useful Commands

- `./gradlew bootRun` : Run the application.
- `./gradlew build` : Build the application.
- `./gradlew test` : Run tests.
- `./gradlew bootJar` : Package the application as a JAR.
- `./gradlew bootBuildImage` : Package the application as a container image.
