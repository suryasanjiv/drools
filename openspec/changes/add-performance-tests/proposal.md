## Why

The application has no performance test coverage. Without load testing, we cannot validate that the Drools rules evaluation endpoint (`POST /eligibility/check`) can handle concurrent traffic at the volumes expected in production. Adding a reproducible, configurable load test suite closes this gap and gives the team a baseline to reason about capacity before deployment.

## What Changes

- New `perf-tests` Gradle subproject containing a Gatling Kotlin DSL load test suite.
- The subproject is explicitly included in `settings.gradle.kts` but is **not wired into the root build lifecycle** — it only executes when explicitly invoked via `./gradlew :perf-tests:gatlingRun`.
- A single Gatling simulation (`EligibilitySimulation`) targeting `POST /eligibility/check` with a configurable mixed-request scenario and built-in assertions.
- All load parameters (concurrency, ramp-up, duration, target URL, request-mix percentages, and assertion thresholds) are configurable via JVM system properties with sensible defaults.

## Capabilities

### New Capabilities

- `eligibility-load-test`: A Gatling-based load test simulation that exercises `POST /eligibility/check` with a configurable mix of Economy, Premium Economy, Business, and invalid requests under sustained concurrent load. Produces an HTML report and exits non-zero when configurable p90/p95/p99 latency or error-rate assertions fail.

### Modified Capabilities

_(none — no existing spec requirements are changing)_

## Impact

- **New directory**: `perf-tests/` at project root (Gradle subproject).
- **`settings.gradle.kts`**: One new `include(":perf-tests")` line.
- **`gradle/libs.versions.toml`**: New `gatling` version entry and library/plugin aliases.
- **No changes** to main application source (`src/`), existing tests, or root build lifecycle.
- **Dependencies added** (perf-tests only): Gatling app, Gatling charts, `io.gatling.gradle` plugin.
