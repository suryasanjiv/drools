## ADDED Requirements

### Requirement: Load test simulation exists and is executable

The `perf-tests` Gradle subproject SHALL contain a Gatling simulation written in Kotlin DSL that can be executed via `./gradlew :perf-tests:gatlingRun` against a running instance of the application.

#### Scenario: Run with default parameters

- **WHEN** the developer runs `./gradlew :perf-tests:gatlingRun` with the application running on `http://localhost:8080`
- **THEN** the simulation completes, an HTML report is generated under `perf-tests/build/reports/gatling/`, and the process exits with code 0 if all assertions pass

#### Scenario: Application not reachable

- **WHEN** the developer runs `./gradlew :perf-tests:gatlingRun` and the target URL is not reachable
- **THEN** Gatling fails immediately with connection errors and exits non-zero

---

### Requirement: Perf-tests subproject is isolated from the root build lifecycle

The `:perf-tests` subproject SHALL be included in `settings.gradle.kts` but MUST NOT be wired into any root lifecycle task (e.g., `build`, `check`, `test`).

#### Scenario: Root build does not trigger perf tests

- **WHEN** the developer runs `./gradlew build` from the project root
- **THEN** no Gatling tasks are executed

#### Scenario: Explicit invocation runs the simulation

- **WHEN** the developer runs `./gradlew :perf-tests:gatlingRun`
- **THEN** the Gatling simulation executes

---

### Requirement: Load parameters are configurable via system properties

The simulation SHALL read all load parameters from JVM system properties with the following defaults.

| Property           | Default                 | Description                     |
| ------------------ | ----------------------- | ------------------------------- |
| `baseUrl`          | `http://localhost:8080` | Target application URL          |
| `concurrentUsers`  | `100`                   | Number of virtual users at peak |
| `rampUpSeconds`    | `30`                    | Time to reach peak concurrency  |
| `sustainedSeconds` | `120`                   | Duration at peak concurrency    |

#### Scenario: Override concurrent users at the command line

- **WHEN** the developer runs `./gradlew :perf-tests:gatlingRun -DconcurrentUsers=200`
- **THEN** the simulation ramps to 200 virtual users and sustains that load

#### Scenario: Override target URL

- **WHEN** the developer runs `./gradlew :perf-tests:gatlingRun -DbaseUrl=https://staging.example.com`
- **THEN** all requests are sent to `https://staging.example.com/eligibility/check`

---

### Requirement: Request mix is configurable and defaults to a realistic distribution

The simulation SHALL distribute requests across cabin-class types using configurable percentage weights, defaulting to: Economy 60%, Premium Economy 25%, Business 10%, Invalid 5%.

| Property      | Default |
| ------------- | ------- |
| `economyPct`  | `60`    |
| `premiumPct`  | `25`    |
| `businessPct` | `10`    |
| `invalidPct`  | `5`     |

The percentages SHALL sum to 100; the simulation MUST fail at startup if they do not.

#### Scenario: Default mix distributes requests proportionally

- **WHEN** the simulation runs with default mix properties
- **THEN** approximately 60% of requests use Economy, 25% Premium Economy, 10% Business, and 5% use an invalid cabin class

#### Scenario: Override mix for a business-heavy run

- **WHEN** the developer runs with `-DeconomyPct=10 -DpremiumPct=10 -DbusinessPct=75 -DinvalidPct=5`
- **THEN** approximately 75% of requests use Business cabin class

---

### Requirement: Assertions enforce latency and error-rate thresholds

The simulation SHALL define built-in Gatling assertions with the following configurable defaults. A breach of any assertion MUST cause the Gatling process to exit with a non-zero status code.

| Property      | Default | Assertion                           |
| ------------- | ------- | ----------------------------------- |
| `p90Ms`       | `100`   | Global p90 response time < value ms |
| `p95Ms`       | `150`   | Global p95 response time < value ms |
| `p99Ms`       | `200`   | Global p99 response time < value ms |
| `maxErrorPct` | `1`     | Global error rate < value %         |

#### Scenario: All assertions pass

- **WHEN** the application responds within the configured thresholds during the simulation
- **THEN** Gatling exits with code 0

#### Scenario: p99 assertion breach

- **WHEN** the p99 response time exceeds the configured threshold
- **THEN** Gatling prints the failed assertion and exits with a non-zero code

#### Scenario: Error rate assertion breach

- **WHEN** more than `maxErrorPct`% of requests return a non-2xx response
- **THEN** Gatling prints the failed assertion and exits with a non-zero code

---

### Requirement: Gatling HTML report is generated after every run

After each simulation run, Gatling SHALL produce an HTML report in `perf-tests/build/reports/gatling/<simulation-name>-<timestamp>/index.html` regardless of whether assertions pass or fail.

#### Scenario: Report generated on passing run

- **WHEN** the simulation completes and all assertions pass
- **THEN** an HTML report directory is present under `build/reports/gatling/`

#### Scenario: Report generated on failing run

- **WHEN** the simulation completes and one or more assertions fail
- **THEN** an HTML report directory is still present under `build/reports/gatling/`
