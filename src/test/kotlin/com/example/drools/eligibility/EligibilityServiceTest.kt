package com.example.drools.eligibility

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kie.api.KieServices
import org.kie.api.runtime.KieContainer

/**
 * Unit tests for EligibilityService covering all spec scenarios.
 * A real KieContainer is built from the classpath rules so Drools
 * logic is exercised without starting the full Spring context.
 */
class EligibilityServiceTest {

    private val kieContainer: KieContainer = buildKieContainer()

    private val service = EligibilityService(kieContainer)

    // ── economy ──────────────────────────────────────────────────────────────

    @Test
    fun `should return eligible for economy with positive duration`() {
        val result = service.check(TripRequest(flightDurationHours = 3.0, chosenCabinClass = CabinClass.ECONOMY))
        assertThat(result.eligible).isTrue()
    }

    @Test
    fun `should return eligible for economy with long duration`() {
        val result = service.check(TripRequest(flightDurationHours = 15.0, chosenCabinClass = CabinClass.ECONOMY))
        assertThat(result.eligible).isTrue()
    }

    @Test
    fun `should return not eligible for economy with zero duration`() {
        val result = service.check(TripRequest(flightDurationHours = 0.0, chosenCabinClass = CabinClass.ECONOMY))
        assertThat(result.eligible).isFalse()
    }

    @Test
    fun `should return not eligible for economy with negative duration`() {
        val result = service.check(TripRequest(flightDurationHours = -1.0, chosenCabinClass = CabinClass.ECONOMY))
        assertThat(result.eligible).isFalse()
    }

    // ── premium economy ───────────────────────────────────────────────────────

    @Test
    fun `should return eligible for premium economy with duration 6 hours or more`() {
        val result = service.check(TripRequest(flightDurationHours = 6.0, chosenCabinClass = CabinClass.PREMIUM_ECONOMY))
        assertThat(result.eligible).isTrue()
    }

    @Test
    fun `should return eligible for premium economy with duration above 6 hours`() {
        val result = service.check(TripRequest(flightDurationHours = 9.0, chosenCabinClass = CabinClass.PREMIUM_ECONOMY))
        assertThat(result.eligible).isTrue()
    }

    @Test
    fun `should return not eligible for premium economy with duration under 6 hours`() {
        val result = service.check(TripRequest(flightDurationHours = 5.9, chosenCabinClass = CabinClass.PREMIUM_ECONOMY))
        assertThat(result.eligible).isFalse()
    }

    // ── business ──────────────────────────────────────────────────────────────

    @Test
    fun `should return eligible for business with duration over 10 hours`() {
        val result = service.check(TripRequest(flightDurationHours = 10.1, chosenCabinClass = CabinClass.BUSINESS))
        assertThat(result.eligible).isTrue()
    }

    @Test
    fun `should return not eligible for business with duration exactly 10 hours`() {
        val result = service.check(TripRequest(flightDurationHours = 10.0, chosenCabinClass = CabinClass.BUSINESS))
        assertThat(result.eligible).isFalse()
    }

    @Test
    fun `should return not eligible for business with duration under 10 hours`() {
        val result = service.check(TripRequest(flightDurationHours = 8.0, chosenCabinClass = CabinClass.BUSINESS))
        assertThat(result.eligible).isFalse()
    }

    // ── null inputs ───────────────────────────────────────────────────────────

    @Test
    fun `should return not eligible when cabin class is null`() {
        val result = service.check(TripRequest(flightDurationHours = 12.0, chosenCabinClass = null))
        assertThat(result.eligible).isFalse()
    }

    @Test
    fun `should return not eligible when flight duration is null`() {
        val result = service.check(TripRequest(flightDurationHours = null, chosenCabinClass = CabinClass.ECONOMY))
        assertThat(result.eligible).isFalse()
    }

    @Test
    fun `should return not eligible when both fields are null`() {
        val result = service.check(TripRequest(flightDurationHours = null, chosenCabinClass = null))
        assertThat(result.eligible).isFalse()
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    companion object {
        private fun buildKieContainer(): KieContainer {
            val kieServices = KieServices.Factory.get()
            val kieFileSystem = kieServices.newKieFileSystem()
            kieFileSystem.write(
                "src/main/resources/rules/cabin-class-eligibility.drl",
                kieServices.resources.newClassPathResource("rules/cabin-class-eligibility.drl"),
            )
            kieServices.newKieBuilder(kieFileSystem).buildAll()
            return kieServices.newKieContainer(kieServices.repository.defaultReleaseId)
        }
    }
}
