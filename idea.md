## Drools Rule Engine

## Overall goal

Create a Drools rules engine REST API that determines whether an employee is eligible for a trip based on their chosen cabin class and flight duration. The API acts as a **gatekeeper** — given what the employee chose, it returns a single eligibility verdict.

The main goal for this change is:

- Create a scaffold project with the basic running app
- Support executing rules (`POST /eligibility/check`) returning a single `eligible` boolean

## API Contract

**Request**

```json
{
  "flightDurationHours": 7.5,
  "chosenCabinClass": "PREMIUM_ECONOMY"
}
```

**Response**

```json
{
  "eligible": true
}
```

`chosenCabinClass` values: `ECONOMY`, `PREMIUM_ECONOMY`, `BUSINESS`

## Rule: Flight Class Eligibility by Trip Duration

The engine uses **deny-by-default** — `EligibilityResult.eligible` starts as `false`. Only "grant" rules exist in the `.drl`; if no rule matches, the result stays `false`.

| Chosen Class    | Duration Condition | Eligible |
| --------------- | ------------------ | -------- |
| ECONOMY         | any                | ✅ true  |
| PREMIUM_ECONOMY | >= 6 hours         | ✅ true  |
| PREMIUM_ECONOMY | < 6 hours          | ❌ false |
| BUSINESS        | > 10 hours         | ✅ true  |
| BUSINESS        | <= 10 hours        | ❌ false |

## Rule Engine Behaviour

- **Stateless session per request** — a fresh `StatelessKieSession` is created for each call and discarded after firing.
- **Two-object fact model** — input and output are kept separate:
  - `TripFact(flightDurationHours, chosenCabinClass)` — immutable input, inserted into the session.
  - `EligibilityResult(var eligible = false)` — mutable result object, also inserted; rules mutate only this.
- **Rules stored as `.drl` files on the classpath** — loaded at startup via `KieContainer`.
- **MVEL** used for rule body expressions.

## Package Structure

Organised by domain capability, not layer:

```
eligibility/    ← controller, service, TripFact, EligibilityResult, DTOs, CabinClass enum
config/         ← DroolsConfig (KieContainer bean)
```

`eligibility/` owns all facts and DTOs — nothing is shared across packages at this stage.

## Technical constraints

- Kotlin latest
- Gradle with Kotlin DSL
- Spring Boot - latest version
- Drools 10
- MVEL for Drools rule expressions
- JUnit tests
