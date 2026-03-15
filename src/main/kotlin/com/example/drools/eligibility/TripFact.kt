package com.example.drools.eligibility

/**
 * Drools input fact. Uses var to satisfy Java bean conventions required by
 * Drools for property access in rule conditions. Rules treat this as read-only.
 * Fields are nullable so missing request values flow through naturally;
 * grant rules simply won't match null values, preserving deny-by-default.
 */
class TripFact(
    var flightDurationHours: Double?,
    var chosenCabinClass: CabinClass?,
)
