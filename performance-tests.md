# Performance tests (Gatling, Kotlin DSL)
Add a Gatling-based performance test suite (Kotlin DSL) to exercise POST /eligibility/check under configurable load. The test harness will live in a Gradle subproject called perf-tests that is NOT executed by the root ./gradlew build (it runs only when explicitly invoked).

## Scope

- Load testing (fixed concurrency sustained for a duration).
- Configurable via system properties: concurrentUsers, rampUpSeconds, sustainedSeconds, baseUrl, and request-mix percentages.
- Produce Gatling HTML report.
- Latency thresholds are: p90 < 200ms, p95 < 300ms, p99 < 600ms, errorRate < 1%.

## Non-goal
- CI/CD compatibility at the moment. We will consider in the far future.

## Design

Project layout (under perf-tests):

perf-tests/
  build.gradle.kts
  src/gatling/kotlin/com/example/EligibilitySimulation.kt
  src/gatling/resources/gatling.conf

## Simulation behavior:

- Target: POST /eligibility/check
- Configurable load defaults: concurrentUsers=100, rampUpSeconds=30, sustainedSeconds=120
- Request mix defaults: economy 60%, premium 25%, business 10%, invalid 5%
- Required: warm-up period (default 20s, fixed scenario prefix) to let the JVM/JIT stabilize
- Request-mix percentages must sum to 100; the simulation will fail fast at startup if they do not
- SLE targets logged at simulation startup (no hard assertion gate — the process always exits 0 to avoid blocking local dev workflows until a real baseline is established):p90 < 200ms — majority of requests
  - p95 < 300ms — near-all requests
  - p99 < 600ms — worst realistic user experience
  - errorRate < 1% — global success rate
  - Breaches are visible in the Gatling console summary table and HTML report; tighten values after a real baseline run


Configuration (examples)
Run with defaults:
bash./gradlew :perf-tests:gatlingRun
Override parameters:
```
bash./gradlew :perf-tests:gatlingRun \
  -DbaseUrl=http://localhost:8080 \
  -DconcurrentUsers=200 -DrampUpSeconds=60 -DsustainedSeconds=300 \
  -DeconomyPct=50 -DpremiumPct=30 -DbusinessPct=15 -DinvalidPct=5
```

## Reporting
- Use Gatling's built-in HTML report (under build/reports/gatling/) for local analysis.
- The Gatling console prints a full percentile summary after every run — compare against the SLE targets above to judge the result.

## Success criteria
- perf-tests subproject runs via ./gradlew :perf-tests:gatlingRun ((explicit include; do not wire into root lifecycle))
- Simulations accept system properties above and honor request-mix percentages.
- Gatling run generates an HTML report and console summary; the process always exits 0 and SLE breaches are visible in the output.
- Add a concise and not verbose README.md with run instructions and example CLI overrides. It shouldn't be longer than a page — engineers are busy!


# Gatling Gradle setup
- Use the latest Gatling Gradle plugin version which is 3.15.0.1