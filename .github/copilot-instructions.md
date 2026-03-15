# Kotlin Copilot Instructions

## Core Philosophy

**Prefer declarative over imperative.** Express _what_ you want, not _how_ to do it.

- **Idiomatic Kotlin first.** Use language features as intended — don't write Java in Kotlin.
- **Immutability by default.** Reach for `val`, `data class`, and immutable collections unless mutation is necessary.

---

## Declarative over Imperative

**Prefer collection operations over loops.**

```kotlin
// ❌
val result = mutableListOf<String>()
for (user in users) {
    if (user.isActive) result.add(user.name)
}

// ✅
val result = users.filter { it.isActive }.map { it.name }
```

**Use `when` as an expression, not a statement.**

```kotlin
// ❌
var label: String
if (score >= 90) label = "A"
else if (score >= 80) label = "B"
else label = "C"

// ✅
val label = when {
    score >= 90 -> "A"
    score >= 80 -> "B"
    else -> "C"
}
```

**Use `apply`, `let`, `run`, `also`, `with` to eliminate temporary variables and clarify intent.**

| Scope function | Returns       | `this` or `it` | Use for                          |
| -------------- | ------------- | -------------- | -------------------------------- |
| `apply`        | receiver      | `this`         | Object configuration             |
| `also`         | receiver      | `it`           | Side effects (logging, etc.)     |
| `let`          | lambda result | `it`           | Nullable checks, transformations |
| `run`          | lambda result | `this`         | Compute a value from an object   |
| `with`         | lambda result | `this`         | Multiple calls on same object    |

```kotlin
// ✅
val user = User().apply {
    name = "Alice"
    age = 30
}
```

---

## Verification Steps

- Run `./gradlew build` from the repository root:
  - Command: `./gradlew build --console=plain`
  - What to verify:
    - The build completes with `BUILD SUCCESSFUL`.
    - The unit tests run (look for the `> Task :test` task) and there are no test failures.

- Run `./gradlew bootRun` and confirm the application started in the logs:
  - Command (capture output): `./gradlew bootRun |& tee bootrun.log`
  - What to verify:
    - Logs contain a Spring Boot startup message such as `Started .* in .*` or `Tomcat started on port` or `Started Application`.
    - Example quick-check: `grep -E "Started .* in|Tomcat started on port|Started Application" bootrun.log || tail -n 200 bootrun.log`

````

## Tests

- Use backticks to name test methods, for example: `fun should return success`().
- Test method names should start with the word "should" (e.g., `should return foo`).
- Only write unit tests — do not write integration tests in this repository's unit test suites.
- Use BDD-style language and format: prefer descriptive `should` phrasing and Given/When/Then where helpful.
- Keep tests simple: one behavior per test, minimal setup, and clear assertions.
- Test controllers using `@WebMvcTest` for MVC slice testing (mock external dependencies).
- Use JUnit 5 and write tests in Kotlin. Prefer `org.junit.jupiter` annotations and idiomatic Kotlin test helpers.

## Immutability

```kotlin
// ❌
var count = 0
var items = mutableListOf<String>()

// ✅
val count = items.size
val items = listOf("a", "b", "c")
```

- Use `val` everywhere possible; use `var` only when reassignment is unavoidable.
- Prefer `listOf`, `mapOf`, `setOf` over their mutable counterparts unless you need mutation.
- Use `data class` for value objects — they give you `equals`, `hashCode`, `copy`, and `toString` for free.

---

## Null Safety

- **Don't use `!!`.** It's an assertion that will crash. Use `?.`, `?:`, or restructure.
- **Use `?:` (Elvis) for defaults**, `?.let` for conditional execution.

```kotlin
// ❌
val name = user!!.name

// ✅
val name = user?.name ?: "Unknown"
user?.let { sendWelcome(it) }
```

- Model optionality explicitly with nullable types (`String?`) rather than sentinel values (`""`, `-1`, `null` by convention).

---

## Functions

**Prefer expression bodies for simple functions.**

```kotlin
// ❌
fun double(x: Int): Int {
    return x * 2
}

// ✅
fun double(x: Int) = x * 2
```

**Use default and named parameters instead of overloads.**

```kotlin
// ❌
fun connect(host: String) = connect(host, 8080)
fun connect(host: String, port: Int) { ... }

// ✅
fun connect(host: String, port: Int = 8080) { ... }
```

**Use extension functions to extend behavior without inheritance.**

```kotlin
fun String.toSlug() = lowercase().replace(" ", "-")
```

**Use `inline` for higher-order functions on hot paths** to avoid lambda allocation overhead.

---

## Classes & Data Modeling

- Prefer `data class` for plain data holders.
- Use `sealed class` / `sealed interface` to model exhaustive state or result types.
- Prefer `object` for singletons; avoid companion object factories unless justified.

```kotlin
// ✅ Exhaustive result modeling
sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val error: Throwable) : Result<Nothing>
}
```

- Avoid unnecessary class hierarchies. Composition over inheritance.
- Use `internal` visibility to limit exposure within a module; don't default to `public`.

---

## Coroutines

- Use `suspend` functions for async work — not callbacks or `Future`.
- Scope coroutines to lifecycle: use `viewModelScope`, `lifecycleScope`, or a structured `CoroutineScope`.
- **Never use `GlobalScope`** in production.
- Prefer `Flow` over channels for data streams.
- Use `StateFlow` / `SharedFlow` for observable state.

```kotlin
// ✅
val uiState: StateFlow<UiState> = repository
    .observeData()
    .map { UiState(it) }
    .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)
```

---

## Error Handling

- Use `sealed` result types (`Result<T, E>`) for expected errors instead of exceptions.
- Reserve exceptions for truly exceptional, unrecoverable cases.
- Avoid bare `try/catch` around large blocks — catch specifically and close to the source.

---

## Collections & Sequences

- **Use `Sequence` for large or chained collection operations** to avoid intermediate allocations.

```kotlin
// ❌ Creates 3 intermediate lists
val result = list.filter { ... }.map { ... }.take(10)

// ✅ Lazy, single pass
val result = list.asSequence().filter { ... }.map { ... }.take(10).toList()
```

---

## Don'ts

| Avoid                              | Prefer instead                          |
|------------------------------------|-----------------------------------------|
| `!!`                               | `?.`, `?:`, safe restructuring          |
| `mutableListOf` by default         | `listOf` unless mutation is needed      |
| Manual loop counters               | `forEachIndexed`, `indices`, `zipWithIndex` |
| Utility classes with static methods | Top-level functions or extension functions |
| `@JvmStatic` / `@JvmField` unless interop | Idiomatic Kotlin declarations       |
````
