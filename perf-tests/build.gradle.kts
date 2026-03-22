plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gatling)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // gatling-charts-highcharts is the all-in-one artifact: includes gatling-app,
    // the HTTP module, and the HTML report generator.
    gatling(libs.gatling.charts.highcharts)
}

// Forward relevant system properties from the Gradle JVM to the Gatling forked JVM.
// Uses systemProperties (not jvmArgs) so Gatling's default --add-opens flags are preserved.
// Pass overrides via: ./gradlew :perf-tests:gatlingRun -DconcurrentUsers=200 ...
val gatlingProps = listOf(
    "baseUrl", "concurrentUsers", "rampUpSeconds", "sustainedSeconds",
    "economyPct", "premiumPct", "businessPct", "invalidPct",
    "p90Ms", "p95Ms", "p99Ms", "maxErrorPct",
)

extensions.configure(io.gatling.gradle.GatlingPluginExtension::class.java) {
    systemProperties = gatlingProps
        .mapNotNull { key -> System.getProperty(key)?.let { value -> key to value } }
        .toMap()
}
