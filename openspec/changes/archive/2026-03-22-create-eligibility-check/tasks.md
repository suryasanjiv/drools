## 1. Project Scaffold

- [x] 1.1 Initialise Spring Boot project with Kotlin and Gradle Kotlin DSL
- [x] 1.2 Add Drools 10 and MVEL dependencies to `gradle/libs.versions.toml` (versions, libraries, bundle)
- [x] 1.3 Wire the Drools bundle into `build.gradle.kts` using the catalog bundle alias
- [x] 1.4 Verify the project builds successfully with `./gradlew build`

## 2. Domain Model

- [x] 2.1 Create `CabinClass` enum with values `ECONOMY`, `PREMIUM_ECONOMY`, `BUSINESS`
- [x] 2.2 Create `TripRequest` data class with nullable fields `flightDurationHours: Double?` and `chosenCabinClass: CabinClass?`
- [x] 2.3 Create `EligibilityResponse` data class with `eligible: Boolean`
- [x] 2.4 Create `TripFact` class with nullable fields `flightDurationHours: Double?` and `chosenCabinClass: CabinClass?` (Drools input fact; use `var` to satisfy Java bean conventions if needed)
- [x] 2.5 Create `EligibilityResult` class with `var eligible: Boolean = false` (Drools output fact)

## 3. Drools Configuration

- [x] 3.1 Create `DroolsConfig` in the `eligibility` package as a Spring `@Configuration` class
- [x] 3.2 Implement `KieContainer` `@Bean` using `KieServices` / `KieFileSystem` / `KieBuilder` API, loading rules from classpath
- [x] 3.3 Verify `KieContainer` bean initialises at startup without errors

## 4. Rules

- [x] 4.1 Create `src/main/resources/rules/cabin-class-eligibility.drl` with MVEL expressions
- [x] 4.2 Add grant rule: `ECONOMY` with any non-null duration → `eligible = true`
- [x] 4.3 Add grant rule: `PREMIUM_ECONOMY` with `flightDurationHours >= 6` → `eligible = true`
- [x] 4.4 Add grant rule: `BUSINESS` with `flightDurationHours > 10` → `eligible = true`
- [x] 4.5 Confirm no deny rules exist (deny-by-default via `EligibilityResult` initial state)

## 5. Service & Controller

- [x] 5.1 Create `EligibilityService` that maps `TripRequest` → `TripFact` + `EligibilityResult`, creates a `StatelessKieSession`, inserts both facts, fires all rules, and returns `EligibilityResponse`
- [x] 5.2 Create `EligibilityController` with `POST /eligibility/check` endpoint, injecting `EligibilityService` via constructor
- [x] 5.3 Confirm null/missing inputs flow through to Drools and return `eligible: false`
- [x] 5.4 Confirm unknown `chosenCabinClass` string values produce HTTP `400` (Jackson deserialisation)

## 6. Tests

- [x] 6.1 Unit test `EligibilityService`: `ECONOMY` + any duration → `eligible: true`
- [x] 6.2 Unit test `EligibilityService`: `PREMIUM_ECONOMY` + duration >= 6 → `eligible: true`
- [x] 6.3 Unit test `EligibilityService`: `PREMIUM_ECONOMY` + duration < 6 → `eligible: false`
- [x] 6.4 Unit test `EligibilityService`: `BUSINESS` + duration > 10 → `eligible: true`
- [x] 6.5 Unit test `EligibilityService`: `BUSINESS` + duration <= 10 → `eligible: false`
- [x] 6.6 Unit test `EligibilityService`: null `chosenCabinClass` → `eligible: false`
- [x] 6.7 Unit test `EligibilityService`: null `flightDurationHours` → `eligible: false`
- [x] 6.8 Unit test `EligibilityService`: both fields null → `eligible: false`
- [x] 6.9 `@WebMvcTest` for `EligibilityController`: valid request returns `200` with `eligible` field
- [x] 6.10 `@WebMvcTest` for `EligibilityController`: unknown cabin class string returns `400`

## 7. Verification

- [x] 7.1 Run `./gradlew build` — build succeeds, all tests pass
- [x] 7.2 Run `./gradlew bootRun` — application starts without errors, Drools `KieContainer` loads cleanly
