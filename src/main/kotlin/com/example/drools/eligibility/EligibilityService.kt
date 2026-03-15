package com.example.drools.eligibility

import org.kie.api.runtime.KieContainer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Orchestrates a single eligibility check. Maps the request DTO to Drools
 * facts, fires a stateless session (one per request, discarded after use),
 * and returns the verdict.
 */
@Service
class EligibilityService(private val kieContainer: KieContainer) {

    companion object {
        private val logger = LoggerFactory.getLogger(EligibilityService::class.java)
    }

    fun check(request: TripRequest): EligibilityResponse {
        val fact = TripFact(
            flightDurationHours = request.flightDurationHours,
            chosenCabinClass = request.chosenCabinClass,
        )
        val result = EligibilityResult()

        val session = kieContainer.newStatelessKieSession()
        session.execute(listOf(fact, result))

        logger.info(
            "Eligibility check: cabinClass={}, duration={}h → eligible={}",
            request.chosenCabinClass,
            request.flightDurationHours,
            result.eligible,
        )

        return EligibilityResponse(eligible = result.eligible)
    }
}
