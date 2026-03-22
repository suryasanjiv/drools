## Context

The application exposes a single stateless HTTP endpoint (`POST /eligibility/check`) backed by a Drools rules engine. There are no databases or external service calls — every request is a pure in-process rules evaluation. This makes the endpoint a good load test candidate: deterministic, isolated, and repeatable.

The project currently has no performance test coverage. The main risk to address is concurrency: can the JVM + Drools handle many simultaneous rule evaluations without throughput degradation or unacceptable tail latency?

The main application is built with Gradle Kotlin DSL. Any tooling added to the repo should be idiomatic in that context.

## Goals / Non-Goals

**Goals:**

- Introduce a Gatling (Kotlin DSL) load test that exercises `POST /eligibility/check` under sustained concurrent load.
- Keep the perf-tests project fully decoupled from the main app build — not included in the root `./gradlew build` lifecycle.
- All parameters (concurrency, duration, target URL, request mix, assertion thresholds) configurable via JVM system properties with sensible defaults.
- Produce a Gatling HTML report and console summary that clearly shows which SLE thresholds were met or breached. The process always exits 0 — there is no CI gate to trigger.

**Non-Goals:**

- Stress or soak testing (out of scope for this change; can be added later).
- Automatically starting the application as part of the test run.
- CI/CD pipeline integration (the test is run manually or in a separate pipeline step).
- Distributed load generation or Gatling Enterprise.

## Decisions

### Decision 1: Gradle subproject, not a standalone project

**Choice**: `perf-tests` as a Gradle subproject (`include(":perf-tests")` in `settings.gradle.kts`).

**Alternatives considered**:

- _Standalone project_ (its own `settings.gradle.kts`, separate `gradlew`) — would duplicate Gradle wrapper and version catalog; harder to keep in sync.
- _Directory with shell scripts_ — no build lifecycle, no dependency management, harder to run portably.

**Rationale**: A subproject shares the version catalog (`gradle/libs.versions.toml`) and the Gradle wrapper. Isolation from the root build lifecycle is achieved simply by not wiring `:perf-tests` tasks into any root lifecycle task (e.g., `build`, `check`). Explicit invocation (`./gradlew :perf-tests:gatlingRun`) is the only entry point.

---

### Decision 2: Gatling with Kotlin DSL (not JMeter, k6, Locust)

**Choice**: Gatling `3.x` with `gatling-gradle` plugin and Kotlin simulation source.

**Alternatives considered**:

- _JMeter_ — XML-based, hard to version-control, GUI-centric; poor fit for a code-first Kotlin project.
- _k6_ — excellent tool but requires Node.js/JavaScript; introduces a second ecosystem into a pure JVM project.
- _Locust_ — Python-based; same ecosystem mismatch concern.

**Rationale**: Gatling is JVM-native, integrates cleanly with Gradle, and its Kotlin DSL means simulations are first-class Kotlin source files alongside the rest of the project.

---

### Decision 3: System properties for runtime configuration

**Choice**: All configurable parameters read via `System.getProperty("key", "default")` in the simulation.

**Alternatives considered**:

- _External YAML/JSON config file_ — more flexible but adds a file-reading step and over-engineers a simple set of scalar parameters.
- _Hardcoded values_ — easy but requires a code change for every environment.

**Rationale**: System properties passed via `-D` flags on the Gradle command are the idiomatic Gatling + Gradle pattern. They are visible in CI logs, require zero extra files, and cover all current configuration needs.

---

### Decision 4: `randomSwitch` for weighted request mix

**Choice**: Use Gatling's `randomSwitch` (percentage-based) to distribute requests across Economy / Premium Economy / Business / Invalid payloads within a single scenario.

**Alternatives considered**:

- _Separate scenarios with separate injection profiles_ — more verbose, harder to control exact per-class percentages at a given concurrency level.

**Rationale**: `randomSwitch` is the canonical Gatling mechanism for weighted request distribution. It keeps a single virtual user definition and makes the percentages easy to read and configure.

---

### Decision 5: Warm-up ramp then sustained load

**Choice**: Load profile = ramp from 0 to `concurrentUsers` over `rampUpSeconds`, then hold for `sustainedSeconds`.

**Rationale**: The JVM JIT compiler optimizes hot paths during the ramp-up phase. Latency measurements taken only during the sustained phase (or across both, with the understanding that the ramp-up will be warmer) are representative of steady-state performance. Gatling's built-in assertions apply globally across the full run; results from the HTML report can be read to isolate the sustained phase manually if needed.

---

### Decision 6: Report SLE breaches via console and HTML — no hard assertion gate

**Choice**: Do **not** use Gatling's `.assertions()` block. Document the SLE targets (p90 < 200 ms, p95 < 300 ms, p99 < 600 ms, error rate < 1%) as logged reference values at simulation startup. The process always exits 0; developers read the Gatling console summary table and/or HTML report to determine whether targets were met.

**Alternatives considered**:

- _Gatling `.assertions()` block_ — causes a non-zero exit on breach, which is useful only as a CI hard gate. Since there is no CI pipeline for this test, a non-zero exit provides no value and would mislead any tooling that checks exit codes.

**Rationale**: Gatling's console output already prints a full percentile table (p50, p75, p95, p99) at the end of every run, and the HTML report highlights slow buckets visually. Breaches are immediately visible without needing a hard failure. This approach is simpler, requires no assertions DSL, and aligns with the project's current no-CI-gate requirement. The SLE values are still captured in code (as startup log lines) so they are not invisible — they just don't gate the run. If a CI gate is ever required in the future, switching to Gatling's `.assertions()` block is a one-line change; the SLE values are already documented and the simulation will be ready for it.

## Risks / Trade-offs

- **JVM warm-up skewing early results** → The ramp-up phase absorbs most JIT cold-start latency. For more precise measurements, a separate explicit warm-up scenario (not counted in assertions) can be added in a future iteration.
- **Machine-dependent results** → Load test numbers depend on the host machine. Thresholds should be treated as relative baselines, not absolute SLEs, until run in a consistent environment (e.g., a dedicated CI agent or staging server).
- **App must be running separately** → The test assumes `baseUrl` is reachable. If it is not, Gatling fails immediately with connection errors. This must be documented clearly in the README.
- **Drools `KieSession` per-request contention** → Each request creates a stateless session from a shared `KieContainer`. Under high concurrency this could become a bottleneck. The load test will surface this if it is an issue.
