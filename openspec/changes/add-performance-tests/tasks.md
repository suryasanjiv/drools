## 1. Gradle Build Setup

- [x] 1.1 Add `gatling` version, library aliases (`gatling-app`, `gatling-charts-highcharts`), and plugin alias (`gatling`) to `gradle/libs.versions.toml`
- [x] 1.2 Add `include(":perf-tests")` to `settings.gradle.kts` (do NOT add to root build lifecycle)
- [x] 1.3 Create `perf-tests/build.gradle.kts` — apply the `io.gatling.gradle` plugin, configure Kotlin toolchain (Java 21), add Gatling dependencies, and wire system property pass-through via `jvmArgs`
- [x] 1.4 Create `perf-tests/src/gatling/kotlin/` source directory structure (package `com.example`)

## 2. Gatling Simulation

- [x] 2.1 Create `EligibilitySimulation.kt` — read all system properties (`baseUrl`, `concurrentUsers`, `rampUpSeconds`, `sustainedSeconds`, `economyPct`, `premiumPct`, `businessPct`, `invalidPct`) with documented defaults
- [x] 2.2 Add startup validation that the four mix percentages sum to 100; throw `IllegalArgumentException` with a clear message if not
- [x] 2.3 Define the HTTP protocol with `baseUrl` and `Content-Type: application/json` header
- [x] 2.4 Define four request builders (Economy, Premium Economy, Business, Invalid) with appropriate JSON bodies targeting `POST /eligibility/check`
- [x] 2.5 Define a single mixed scenario per cabin class (closed injection; proportional user allocation per design.md Decision 4 — separate scenarios chosen over `randomSwitch` for per-type stats in the HTML report)
- [x] 2.6 Define the injection profile: ramp-up from 0 to `concurrentUsers` over `rampUpSeconds`, then hold for `sustainedSeconds`
- [x] 2.7 Log SLE targets (p90/p95/p99/errorRate) at simulation startup as reference values — no `.assertions()` block per design.md Decision 6 (process always exits 0)

## 3. Configuration Pass-Through

- [x] 3.1 Ensure `perf-tests/build.gradle.kts` forwards all relevant `-D` system properties from the Gradle JVM to the Gatling forked JVM (via `gatlingRun { jvmArgs(...) }` or equivalent)

## 4. Documentation

- [x] 4.1 Create `perf-tests/README.md` with: prerequisites (app must be running), default run command, override examples for URL / concurrency / mix / thresholds, and where to find the HTML report
- [x] 4.2 Ensure `perf-tests/build/` is covered by `.gitignore` (root `build/` pattern already matches nested dirs — no change needed)

## 5. Verification

- [x] 5.1 Run `./gradlew build` from the project root and confirm no Gatling tasks execute and the build remains green
- [x] 5.2 Start the application with `./gradlew bootRun` and run `./gradlew :perf-tests:gatlingRun --no-configuration-cache` with defaults; confirm the simulation completes and an HTML report is generated (35k requests, p99=10ms, 0 errors, 1843 req/s)
- [x] 5.3 Run with `-DconcurrentUsers=10 -DrampUpSeconds=5 -DsustainedSeconds=15` and confirm the overrides are respected in the Gatling console output (verified: banner shows `Users: 10 | Ramp: 5s | Sustained: 15s`)
- [x] 5.4 Verify no hard assertion gate: set `-Dp99Ms=1` (unrealistically tight) and confirm the process still exits 0 and the SLE target is shown in the banner as a reference value (per design.md Decision 6)
