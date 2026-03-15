## Why

We need a single, reliable service that answers: "Is the employee's chosen cabin class permitted for this trip?" Centralising this decision reduces duplicated policy logic across booking systems and makes audits and tests straightforward.

## What Changes

- Add an `eligibility-check` capability that exposes a simple REST API to determine trip eligibility.
- Add a single endpoint `POST /eligibility/check` which accepts flight duration and the chosen cabin class and returns a boolean `eligible`.
- Implement rules as classpath `.drl` files (MVEL) loaded at application startup; each API call evaluates rules in a short-lived session.

## Capabilities

### New Capabilities

- `eligibility-check`: Given `{ flightDurationHours, chosenCabinClass }`, return `{ eligible: boolean }` indicating whether the chosen cabin is allowed.

### Modified Capabilities

- None

## Impact

- Code: introduce an `eligibility/` package containing the API controller, service, request/response models, and the facts used by the rules.
- Configuration: add a small Drools wiring component and a couple of application configuration entries (rules path, optional reload toggle).
- Rules: add a focused rules file for cabin-class eligibility to `src/main/resources/rules/`.
- API: new non-breaking endpoint `POST /eligibility/check` (keeps existing callers unaffected unless they adopt the new API).
- Dependencies: add Drools 10 and the MVEL runtime to the build.

## Non-goals

- Dynamic rule-management UI or API (deferred to a future change).
- Human approval workflows or multi-step processes.
