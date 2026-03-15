package com.example.drools.eligibility

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(EligibilityController::class)
class EligibilityControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var eligibilityService: EligibilityService

    @Test
    fun `should return 200 with eligible true for valid request`() {
        val request = TripRequest(flightDurationHours = 7.5, chosenCabinClass = CabinClass.PREMIUM_ECONOMY)
        whenever(eligibilityService.check(request)).thenReturn(EligibilityResponse(eligible = true))

        mockMvc.post("/eligibility/check") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.eligible") { value(true) }
        }
    }

    @Test
    fun `should return 400 for unknown cabin class string`() {
        mockMvc.post("/eligibility/check") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"flightDurationHours": 7.5, "chosenCabinClass": "FIRST_CLASS"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
