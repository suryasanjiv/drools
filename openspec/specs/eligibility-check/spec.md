## Requirements

### Requirement: Employee trip eligibility check

The system SHALL accept a trip request containing a flight duration and a chosen cabin class, and SHALL return a single boolean indicating whether the chosen cabin class is permitted for that trip.

#### Scenario: Economy class is eligible for any positive, non-null duration

- **WHEN** a request is submitted with `chosenCabinClass` of `ECONOMY` and a positive, non-null `flightDurationHours`
- **THEN** the response SHALL contain `eligible: true`

#### Scenario: Premium economy eligible for flights of 6 hours or more

- **WHEN** a request is submitted with `chosenCabinClass` of `PREMIUM_ECONOMY` and `flightDurationHours` >= 6
- **THEN** the response SHALL contain `eligible: true`

#### Scenario: Premium economy not eligible for flights under 6 hours

- **WHEN** a request is submitted with `chosenCabinClass` of `PREMIUM_ECONOMY` and `flightDurationHours` < 6
- **THEN** the response SHALL contain `eligible: false`

#### Scenario: Business class eligible for flights over 10 hours

- **WHEN** a request is submitted with `chosenCabinClass` of `BUSINESS` and `flightDurationHours` > 10
- **THEN** the response SHALL contain `eligible: true`

#### Scenario: Business class not eligible for flights of 10 hours or under

- **WHEN** a request is submitted with `chosenCabinClass` of `BUSINESS` and `flightDurationHours` <= 10
- **THEN** the response SHALL contain `eligible: false`

### Requirement: Deny-by-default eligibility

The system SHALL default to `eligible: false` for any request where no rule explicitly grants eligibility (e.g. a new `CabinClass` value is added to the enum but no corresponding grant rule exists in the `.drl`).

#### Scenario: Cabin class with no matching grant rule

- **WHEN** a valid `chosenCabinClass` is submitted but no Drools rule grants eligibility for it
- **THEN** the response SHALL contain `eligible: false`

> **Note:** This scenario is a design guarantee enforced by `EligibilityResult` initialising with `eligible = false` (deny-by-default). It cannot be exercised directly without adding a new `CabinClass` enum value with no corresponding DRL grant rule. It is verified implicitly by the deny-by-default unit tests (null cabin class, null duration) and the structure of `EligibilityResult`.

### Requirement: Valid cabin class input

The system SHALL accept `chosenCabinClass` values of `ECONOMY`, `PREMIUM_ECONOMY`, or `BUSINESS`. An unrecognised string value SHALL be rejected with `400 Bad Request`. A `null` or absent `chosenCabinClass` SHALL be accepted and SHALL result in `eligible: false` via deny-by-default.

#### Scenario: Invalid cabin class string rejected

- **WHEN** a request is submitted with a `chosenCabinClass` string that does not match a known enum value
- **THEN** the system SHALL respond with HTTP `400 Bad Request`

#### Scenario: Null cabin class returns not eligible

- **WHEN** a request is submitted with `chosenCabinClass` absent or null
- **THEN** the response SHALL contain `eligible: false`

### Requirement: Flight duration input handling

The system SHALL accept any numeric value or null for `flightDurationHours`. Values that are null, zero, or negative SHALL not be rejected; instead they SHALL result in `eligible: false` as no grant rule will match them.

#### Scenario: Null flight duration returns not eligible

- **WHEN** a request is submitted with `flightDurationHours` absent or null
- **THEN** the response SHALL contain `eligible: false`

#### Scenario: Zero or negative flight duration returns not eligible

- **WHEN** a request is submitted with `flightDurationHours` <= 0
- **THEN** the response SHALL contain `eligible: false`
