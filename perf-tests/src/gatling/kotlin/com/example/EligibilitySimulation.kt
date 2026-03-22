package com.example

import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.Duration

/**
 * Gatling load test for POST /eligibility/check.
 *
 * The application MUST be running before this simulation is executed.
 * All parameters are configurable via JVM system properties — see README.md.
 *
 * SLE targets are printed at startup for reference.
 * No hard assertion gate: the process always exits 0.
 * Compare the Gatling console summary and HTML report against the SLE targets manually.
 */
class EligibilitySimulation : Simulation() {

    // ── Load parameters ───────────────────────────────────────────────────────

    private val baseUrl          = System.getProperty("baseUrl",          "http://localhost:8080")
    private val concurrentUsers  = System.getProperty("concurrentUsers",  "100").toInt()
    private val rampUpSeconds    = System.getProperty("rampUpSeconds",    "30").toLong()
    private val sustainedSeconds = System.getProperty("sustainedSeconds", "120").toLong()

    // ── Request mix percentages (must sum to 100) ─────────────────────────────

    private val economyPct  = System.getProperty("economyPct",  "60").toDouble()
    private val premiumPct  = System.getProperty("premiumPct",  "25").toDouble()
    private val businessPct = System.getProperty("businessPct", "10").toDouble()
    private val invalidPct  = System.getProperty("invalidPct",  "5").toDouble()

    // ── SLE targets — reference only, no hard gate ────────────────────────────

    private val p90Ms       = System.getProperty("p90Ms",       "200").toLong()
    private val p95Ms       = System.getProperty("p95Ms",       "300").toLong()
    private val p99Ms       = System.getProperty("p99Ms",       "600").toLong()
    private val maxErrorPct = System.getProperty("maxErrorPct", "1").toDouble()

    // ── Startup validation + config banner ────────────────────────────────────

    init {
        val total = economyPct + premiumPct + businessPct + invalidPct
        require(total == 100.0) {
            "Request mix percentages must sum to 100, got $total " +
                "(economyPct=$economyPct premiumPct=$premiumPct " +
                "businessPct=$businessPct invalidPct=$invalidPct)"
        }

        println(
            """
            ┌───────────────────────────────────────────────────────────────┐
            │               Eligibility Load Test — Config                  │
            ├───────────────────────────────────────────────────────────────┤
            │  Target      : $baseUrl
            │  Users       : $concurrentUsers  |  Ramp: ${rampUpSeconds}s  |  Sustained: ${sustainedSeconds}s
            │  Mix         : Economy ${economyPct}%  Premium ${premiumPct}%  Business ${businessPct}%  Invalid ${invalidPct}%
            ├───────────────────────────────────────────────────────────────┤
            │  SLE targets (reference — no hard gate, process always exits 0)
            │    p90 < ${p90Ms}ms   p95 < ${p95Ms}ms   p99 < ${p99Ms}ms   errorRate < ${maxErrorPct}%
            └───────────────────────────────────────────────────────────────┘
            """.trimIndent(),
        )
    }

    // ── HTTP protocol ─────────────────────────────────────────────────────────

    private val httpProtocol = http
        .baseUrl(baseUrl)
        .contentTypeHeader("application/json")
        .acceptHeader("application/json")

    // ── Requests ──────────────────────────────────────────────────────────────
    //
    // Economy / Premium Economy / Business: valid requests, expect 200.
    // Invalid cabin class (FIRST_CLASS): app returns 400 by design;
    //   checked explicitly so Gatling counts it as a success rather than an error.

    private val economyReq = http("economy")
        .post("/eligibility/check")
        .body(io.gatling.javaapi.core.CoreDsl.StringBody(
            """{"flightDurationHours": 3.0, "chosenCabinClass": "ECONOMY"}""",
        ))
        .check(status().`is`(200))

    private val premiumReq = http("premium_economy")
        .post("/eligibility/check")
        .body(io.gatling.javaapi.core.CoreDsl.StringBody(
            """{"flightDurationHours": 7.0, "chosenCabinClass": "PREMIUM_ECONOMY"}""",
        ))
        .check(status().`is`(200))

    private val businessReq = http("business")
        .post("/eligibility/check")
        .body(io.gatling.javaapi.core.CoreDsl.StringBody(
            """{"flightDurationHours": 11.0, "chosenCabinClass": "BUSINESS"}""",
        ))
        .check(status().`is`(200))

    private val invalidReq = http("invalid_cabin_class")
        .post("/eligibility/check")
        .body(io.gatling.javaapi.core.CoreDsl.StringBody(
            """{"flightDurationHours": 5.0, "chosenCabinClass": "FIRST_CLASS"}""",
        ))
        .check(status().`is`(400)) // 400 is the expected response for unknown cabin class

    // ── Scenarios ─────────────────────────────────────────────────────────────
    //
    // One scenario per cabin class so the HTML report shows per-type latency
    // breakdowns. User counts are distributed proportionally from concurrentUsers.

    private val economyScenario  = scenario("Economy").exec(economyReq)
    private val premiumScenario  = scenario("Premium Economy").exec(premiumReq)
    private val businessScenario = scenario("Business").exec(businessReq)
    private val invalidScenario  = scenario("Invalid Cabin Class").exec(invalidReq)

    /** Proportional share of [concurrentUsers] for a given percentage, minimum 1. */
    private fun usersFor(pct: Double) = (concurrentUsers * pct / 100).toInt().coerceAtLeast(1)

    // ── Simulation setup ──────────────────────────────────────────────────────
    //
    // Closed injection: controls the number of concurrent users at any instant
    // (as opposed to open injection which controls the arrival rate).
    // Profile: ramp from 0 → peak over rampUpSeconds, then hold for sustainedSeconds.

    init {
        setUp(
            economyScenario.injectClosed(
                rampConcurrentUsers(0).to(usersFor(economyPct)).during(Duration.ofSeconds(rampUpSeconds)),
                constantConcurrentUsers(usersFor(economyPct)).during(Duration.ofSeconds(sustainedSeconds)),
            ),
            premiumScenario.injectClosed(
                rampConcurrentUsers(0).to(usersFor(premiumPct)).during(Duration.ofSeconds(rampUpSeconds)),
                constantConcurrentUsers(usersFor(premiumPct)).during(Duration.ofSeconds(sustainedSeconds)),
            ),
            businessScenario.injectClosed(
                rampConcurrentUsers(0).to(usersFor(businessPct)).during(Duration.ofSeconds(rampUpSeconds)),
                constantConcurrentUsers(usersFor(businessPct)).during(Duration.ofSeconds(sustainedSeconds)),
            ),
            invalidScenario.injectClosed(
                rampConcurrentUsers(0).to(usersFor(invalidPct)).during(Duration.ofSeconds(rampUpSeconds)),
                constantConcurrentUsers(usersFor(invalidPct)).during(Duration.ofSeconds(sustainedSeconds)),
            ),
        ).protocols(httpProtocol)
    }
}
