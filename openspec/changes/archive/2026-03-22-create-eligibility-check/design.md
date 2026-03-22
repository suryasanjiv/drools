## Context

The eligibility-check capability introduces a Drools-based rules engine into a new Spring Boot + Kotlin application. There is no existing codebase to integrate with — this is a greenfield scaffold. The primary constraint is the tech stack: Kotlin, Gradle Kotlin DSL, Spring Boot (latest), and Drools 10 with MVEL rule expressions.

The API is a gatekeeper: given an employee's chosen cabin class and flight duration, it returns a single boolean indicating whether the choice is permitted under current travel policy.

## Goals / Non-Goals

**Goals:**

- Design a clean, testable layered structure for the eligibility-check capability.
- Establish how Drools is wired into Spring Boot and how each request maps to a rule evaluation cycle.
- Document all key technical decisions so implementation can proceed without ambiguity.

**Non-Goals:**

- Dynamic rule management (loading, updating, or deleting rules at runtime via API) — deferred.
- Authentication and authorisation — out of scope for the scaffold.
- Persistent storage of trip requests or evaluation results.

## Decisions

### 1. Package structure: feature/domain, not layer

**Decision:** Organise code by domain capability, not by technical layer.

```
com.example.drools
├── eligibility/         ← all eligibility-check code
│   ├── EligibilityController.kt
│   ├── EligibilityService.kt
│   ├── TripRequest.kt          (request DTO)
│   ├── EligibilityResponse.kt  (response DTO)
│   ├── TripFact.kt             (Drools input fact)
│   ├── EligibilityResult.kt    (Drools output fact)
│   ├── CabinClass.kt           (enum: ECONOMY, PREMIUM_ECONOMY, BUSINESS)
│   └── DroolsConfig.kt         (KieContainer Spring bean)
└── DroolsApplication.kt
```

**Rationale:** Keeps all eligibility concerns co-located — including Drools wiring, which is an internal implementation detail of this capability, not shared infrastructure. A `config/` package would only be warranted if a second capability also needed Drools; that generalisation is deferred until it's actually needed. A future `rules/` package for rule management would be a peer of `eligibility/`, not a sibling inside it.

---

### 2. Two-object fact model

**Decision:** Use two separate objects inserted into the Drools session — one immutable input, one mutable output.

- `TripFact(var flightDurationHours: Double?, var chosenCabinClass: CabinClass?)` — inserted as-is; rules only read it. Fields use `var` and nullable types to satisfy Drools Java bean conventions and to allow null inputs to flow through from the DTO without being rejected at the HTTP boundary.
- `EligibilityResult(var eligible: Boolean = false)` — inserted with `eligible = false`; rules set it to `true` if conditions are met.

**Rationale:** Keeping input and output separate avoids mutating request data and makes the rule contract explicit. Rules only ever write to `EligibilityResult`, which is easy to reason about and test.

**Alternative considered:** A single merged object (`TripFact` with `var eligible`). Rejected because it conflates input data with output state.

---

### 3. Stateless KieSession per request

**Decision:** Use a `StatelessKieSession` created fresh for each API call and discarded immediately after.

**Rationale:** Each eligibility check is a self-contained, independent evaluation. Stateless sessions avoid the need for `dispose()` calls, prevent cross-request state bleed, and are thread-safe when created from a shared `KieContainer`. The `KieContainer` itself is a singleton Spring bean, loaded once at startup.

**Alternative considered:** Stateful `KieSession` pooled per request. Rejected — adds lifecycle complexity with no benefit for single-shot evaluations.

---

### 4. Deny-by-default rule strategy

**Decision:** `EligibilityResult.eligible` starts as `false`. Only "grant" rules exist in the `.drl` file. There are no explicit "deny" rules.

```
# Pseudocode — only allow rules:
ECONOMY         + any duration    → eligible = true
PREMIUM_ECONOMY + duration >= 6h  → eligible = true
BUSINESS        + duration > 10h  → eligible = true
```

**Rationale:** If no rule fires (e.g., unknown cabin class, edge-case input), the safe default is ineligible. Simpler to add grant rules for new classes than to manage deny-then-override logic.

---

### 5. Rules stored as classpath `.drl` files with MVEL expressions

**Decision:** Rules live in `src/main/resources/rules/cabin-class-eligibility.drl`, loaded by `KieContainer` at startup. Rule body expressions use MVEL.

**Rationale:** Classpath loading is the simplest integration path with Spring Boot — no external repo or hot-reload infrastructure needed. MVEL is the natural Drools expression language and avoids verbose Java RHS code for readable, concise rule bodies.

**Alternative considered:** Programmatic rule building (Executable Model / Kogito-style). Rejected — significantly more setup, less readable, and Drools 10 `.drl` files remain well-supported.

---

### 6. Drools 10 with Spring Boot wiring via KieServices API

**Decision:** Use Drools 10's standard `KieServices` / `KieFileSystem` / `KieBuilder` API to initialise a `KieContainer` as a singleton Spring `@Bean` in `DroolsConfig`.

```
KieServices
  └── newKieFileSystem()
        └── write(classpath rules)
              ↓
        KieBuilder.buildAll()
              ↓
        KieContainer  (@Bean, singleton)
              ↓
        per-request: container.newStatelessKieSession()
```

**Rationale:** Standard, well-documented approach. Isolates Drools wiring in one config class. No dependency on Kogito, Quarkus, or KIE Workbench.

---

### 7. Dependency management via Gradle version catalog

**Decision:** Add Drools 10 and MVEL to `gradle/libs.versions.toml`. Group related Drools dependencies into a bundle.

```toml
[versions]
drools = "10.x.x"   # pin to latest stable Drools 10

[libraries]
drools-core     = { module = "org.drools:drools-core",     version.ref = "drools" }
drools-compiler = { module = "org.drools:drools-compiler", version.ref = "drools" }
drools-mvel     = { module = "org.drools:drools-mvel",     version.ref = "drools" }

[bundles]
drools = ["drools-core", "drools-compiler", "drools-mvel"]
```

**Rationale:** Keeps versions centralised and consistent with the project's existing Gradle conventions.

---

### 8. Input handling strategy

**Decision:** Accept nullable fields at the HTTP boundary and treat missing or null inputs as "no grant" (i.e., not eligible). Do not fail requests automatically for missing fields; let rules (and deny-by-default) determine eligibility. However, invalid enum string values will still be rejected by Jackson with `400 Bad Request`.

- `TripRequest` DTO fields are nullable (`flightDurationHours: Double?`, `chosenCabinClass: CabinClass?`). Missing or null values are allowed and map through to Drools facts.
- No Jakarta validation annotations are applied that would reject missing/null values. (We intentionally avoid `@NotNull`.)
- Jackson enum deserialisation behavior remains: an unknown string value for `chosenCabinClass` will produce a 400 during deserialisation; `null` or an absent field will result in a `null` value on the DTO.

**Rationale:** This approach keeps the HTTP API forgiving for callers (they get a clear `eligible: false` result when inputs are insufficient) and centralises policy decision-making in the rules. It also simplifies client integration during early adoption while preserving a safe default.

## Data Structures

### API — Request & Response

**`TripRequest`** (HTTP request body, deserialised from JSON)
| Field | Type | Notes |
|---|---|---|
| `flightDurationHours` | `Double?` | Nullable; if null or missing, treated as "no grant" by rules |
| `chosenCabinClass` | `CabinClass?` | Nullable; if null or missing, treated as "no grant"; unknown strings are rejected by Jackson (400) |

**`EligibilityResponse`** (HTTP response body, serialised to JSON)
| Field | Type | Notes |
|---|---|---|
| `eligible` | `Boolean` | `true` if permitted, `false` otherwise |

---

### Drools Facts — Session Objects

**`TripFact`** (Drools input fact, inserted into session; rules treat fields as read-only)
| Field | Type | Notes |
|---|---|---|
| `flightDurationHours` | `Double?` | Nullable; `var` for Java bean convention; rules never write to it |
| `chosenCabinClass` | `CabinClass?` | Nullable; `var` for Java bean convention; rules never write to it |

**`EligibilityResult`** (mutable output fact, inserted into session)
| Field | Type | Default | Notes |
|---|---|---|---|
| `eligible` | `Boolean` | `false` | Rules set to `true` if a grant condition matches |

Both facts are inserted into the same `StatelessKieSession` per request. Rules read `TripFact` and write only to `EligibilityResult`.

---

### Enum

**`CabinClass`**

```
ECONOMY
PREMIUM_ECONOMY
BUSINESS
```

---

### Data flow per request

```
HTTP POST /eligibility/check
        │
        ▼
  TripRequest (DTO)
        │  map
        ▼
  TripFact + EligibilityResult(eligible=false)
        │  insert both into StatelessKieSession
        ▼
  Drools fires rules
        │  rules write to EligibilityResult only
        ▼
  read EligibilityResult.eligible
        │  map
        ▼
  EligibilityResponse { eligible: true/false }
        │
        ▼
  HTTP 200 OK
```

---

## Risks / Trade-offs

- **Drools 10 + Spring Boot compatibility** → Drools 10 has fewer Spring Boot integration examples than 8.x. Mitigation: use the standard `KieServices` API which is stable across major versions; avoid framework-specific integrations.
- **Classpath rules → no runtime reload** → Rules bundled in the JAR require a redeploy to change. Mitigation: acceptable for now; dynamic rule management is explicitly deferred. Document this constraint clearly.
- **MVEL dependency** → MVEL adds a runtime dependency. Mitigation: already accounted for as an explicit build dependency (`drools-mvel`).
- **Kotlin data classes as Drools facts** → Drools uses Java bean conventions (getters/setters). Kotlin `data class` generates getters but not setters for `val` properties. Mitigation: use `var` for any property rules need to mutate (only `EligibilityResult.eligible`); keep `TripFact` properties as `val`.

## Open Questions

- Confirm the exact latest stable Drools 10 version before pinning in the version catalog.
- Confirm whether `application.yml` should expose a configurable rules path or whether hardcoding the classpath location is sufficient for the scaffold.
