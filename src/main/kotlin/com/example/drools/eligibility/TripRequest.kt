package com.example.drools.eligibility

data class TripRequest(
    val flightDurationHours: Double?,
    val chosenCabinClass: CabinClass?,
)
