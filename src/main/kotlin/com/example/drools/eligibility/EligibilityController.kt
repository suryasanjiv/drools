package com.example.drools.eligibility

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/eligibility")
class EligibilityController(private val eligibilityService: EligibilityService) {

    @PostMapping("/check", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun check(@RequestBody request: TripRequest): EligibilityResponse =
        eligibilityService.check(request)
}
