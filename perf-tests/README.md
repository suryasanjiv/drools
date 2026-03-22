# perf-tests

Gatling load tests for `POST /eligibility/check`.

> **Prerequisites** — The application must already be running. Start it with `./gradlew bootRun` before running any simulation.

## Quick start

```bash
# Terminal 1: start the app
./gradlew bootRun

# Terminal 2: run the load test
./gradlew :perf-tests:gatlingRun --no-configuration-cache
```

> `--no-configuration-cache` is required because the Gatling Gradle plugin (3.13.x) is not fully compatible with Gradle's configuration cache.

HTML report: `perf-tests/build/reports/gatling/<run-name>/index.html`

## Parameters

All parameters are passed as `-D` flags. Defaults shown.

| Property           | Default                 | Description                              |
| ------------------ | ----------------------- | ---------------------------------------- |
| `baseUrl`          | `http://localhost:8080` | Target application URL                   |
| `concurrentUsers`  | `100`                   | Peak concurrent users                    |
| `rampUpSeconds`    | `30`                    | Time to reach peak concurrency           |
| `sustainedSeconds` | `120`                   | Duration at peak concurrency             |
| `economyPct`       | `60`                    | % Economy requests (must sum to 100)     |
| `premiumPct`       | `25`                    | % Premium Economy requests               |
| `businessPct`      | `10`                    | % Business requests                      |
| `invalidPct`       | `5`                     | % Invalid cabin class (expects HTTP 400) |
| `p90Ms`            | `200`                   | SLE target: p90 response time (ms)       |
| `p95Ms`            | `300`                   | SLE target: p95 response time (ms)       |
| `p99Ms`            | `600`                   | SLE target: p99 response time (ms)       |
| `maxErrorPct`      | `1`                     | SLE target: max error rate (%)           |

SLE targets are printed at startup for reference. The simulation always exits 0 — compare the console summary and HTML report against the targets to assess results.

## Examples

```bash
# Run against a remote instance
./gradlew :perf-tests:gatlingRun --no-configuration-cache -DbaseUrl=https://staging.example.com

# Increase load
./gradlew :perf-tests:gatlingRun --no-configuration-cache -DconcurrentUsers=500 -DrampUpSeconds=60 -DsustainedSeconds=300

# Business-heavy traffic mix
./gradlew :perf-tests:gatlingRun --no-configuration-cache -DeconomyPct=20 -DpremiumPct=20 -DbusinessPct=55 -DinvalidPct=5
```
