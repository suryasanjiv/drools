package com.example.drools.eligibility

/**
 * Drools output fact. Starts as false (deny-by-default).
 * Grant rules set eligible = true when conditions are met.
 */
class EligibilityResult(
    var eligible: Boolean = false,
)
