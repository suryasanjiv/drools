---
description: "Instructions for editing Gradle Kotlin DSL build files, version catalogs, and settings files"
applyTo: "**/*.gradle.kts,**/settings.gradle.kts,**/gradle/libs.versions.toml,**/gradle.properties"
---

# Gradle Kotlin DSL Instructions

## Role

You are editing a Gradle build that uses Kotlin DSL and a centralized version catalog.

When changing build logic:
- Prefer small, safe, idiomatic Gradle changes.
- Preserve existing project conventions unless the task requires a change.
- Do not introduce new build patterns unless they clearly improve consistency, maintainability, or performance.
- Only use the Gradle wrapper.

## Core Rules

- Use Kotlin DSL only. Do not generate Groovy DSL.
- Use the `plugins {}` block for plugin application whenever possible.
- Prefer strongly typed Kotlin DSL accessors over string-based lookups.
- Prefer lazy, configuration-avoidance APIs such as `registering`, `named`, and `configureEach`.
- Do not eagerly realize tasks or configurations unless required.
- Do not use Gradle internal APIs.

## Version and Dependency Management

- Centralize dependency coordinates in `gradle/libs.versions.toml`.
- Never hardcode dependency or plugin versions in `build.gradle.kts` if they belong in the version catalog.
- Prefer catalog aliases such as `libs.xxx` and `libs.plugins.xxx`.
- Use kebab-case keys in `[versions]`, `[libraries]`, `[bundles]`, and `[plugins]`.
- Group related dependencies into bundles only when that improves readability and reuse.
- Do not specify versions for dependencies already managed by a platform or BOM unless there is a documented reason to override them.
- If overriding a managed version, add a short comment explaining why.

## settings.gradle.kts

- Set `rootProject.name`.
- Centralize repository and plugin resolution in `settings.gradle.kts`.
- Prefer `dependencyResolutionManagement` for repositories.
- Prefer `RepositoriesMode.FAIL_ON_PROJECT_REPOS` unless the build intentionally allows project-level repositories.
- Include subprojects explicitly with one `include(":module")` entry per line.
- Do not add custom version catalog setup when the standard `gradle/libs.versions.toml` arrangement is sufficient.
- Put plugin repositories in `pluginManagement { repositories { ... } }` when plugin resolution needs configuration.

## Repositories

- Prefer as few repositories as possible.
- Prefer `mavenCentral()` unless another repository is required.
- Do not add `repositories {}` blocks to subprojects when repositories are centralized in settings.
- Do not add snapshot, milestone, or ad-hoc repositories unless the task explicitly requires them.

## Java and Kotlin Toolchains

- Prefer Gradle toolchains over sourceCompatibility/targetCompatibility when possible.
- Keep JVM and Kotlin toolchain configuration aligned.
- Reuse the existing project convention for Java/Kotlin version management.
- If the project stores the Java version in the catalog or gradle.properties, continue using that source of truth.

## Dependency Scopes

Choose the narrowest correct scope:
- `implementation` for internal production dependencies.
- `api` only for dependencies intentionally exposed by library modules.
- `compileOnly` for compile-time only dependencies.
- `runtimeOnly` for runtime-only dependencies.
- `testImplementation` and `testRuntimeOnly` for tests.

Prefer `implementation` over `api` unless the module is a library and the dependency is part of its public surface.

## Spring Boot and BOMs

- If the project uses Spring Boot, follow the existing project convention for dependency management.
- Prefer BOM/platform-managed versions over repeating versions on individual dependencies.
- Do not add explicit versions to Spring Boot managed dependencies unless there is a strong compatibility or security reason.
- Avoid mixing competing dependency-management approaches in the same module.

## Tasks

- Configure existing tasks with `tasks.named<T>("name")`.
- Use `withType<T>().configureEach { ... }` only when the same configuration should apply to all tasks of that type.
- Prefer declarative task inputs/outputs and typed task properties.
- Avoid `doFirst` and `doLast` for normal configuration when a proper task property or task type can be used.
- When creating custom tasks, make them cache-friendly and incremental where practical.

## gradle.properties

- Use `gradle.properties` for build flags and Gradle/JVM tuning.
- Do not put secrets, tokens, passwords, or environment-specific credentials in `gradle.properties`.
- Use environment variables or the project’s approved secret mechanism for secrets.
- Keep performance flags only if they are compatible with the build.

## Performance and Reliability

- Prefer configuration-cache-compatible patterns.
- Prefer build-cache-friendly task design.
- Avoid patterns that force early configuration or dependency resolution during configuration time.
- Do not add unnecessary work to all projects or all tasks.

## Validation After Changes

After changing build files:
- Verify the change is minimal and consistent with existing conventions.
- Ensure added aliases exist in `gradle/libs.versions.toml`.
- Ensure plugin aliases and library aliases use the correct catalog names.
- Check whether the affected module should compile, test, or run with the new configuration.

When suggesting validation commands, prefer the smallest relevant command first, for example:
- `./gradlew :module:compileKotlin`
- `./gradlew :module:test`
- `./gradlew build`

## What to Avoid

- Do not migrate unrelated parts of the build.
- Do not reorder large sections of TOML or Gradle files without reason.
- Do not introduce `allprojects {}` or `subprojects {}` cross-configuration unless the repository already relies on that pattern.
- Do not add duplicate aliases, duplicate repositories, or duplicate plugin declarations.
- Do not hardcode file paths or environment-specific machine settings.
- Do not assume every project is an application; library modules and convention plugins should stay minimal.

## Editing Style

When producing changes:
- Modify the fewest files necessary.
- Keep naming consistent with existing aliases and modules.
- Add short comments only for non-obvious decisions.
- Prefer readability over clever Gradle constructs.