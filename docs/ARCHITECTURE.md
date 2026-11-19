# Architecture

## Goals
- Deliver a genuinely native Android application.
- Keep astronomical and Panchanga domain logic independent of Android UI frameworks.
- Establish one authoritative shared calculation core for Android and, after staged migration, the web application.
- Preserve calculation traceability and deterministic regression testing.

## High-level structure
- shared/domain: inputs, outputs, enums, domain invariants.
- shared/astronomy: ephemeris adapter and astronomical primitives.
- shared/panchanga: tithi, nakshatra, yoga, karana, masa, vara.
- shared/hora: day/night hora intervals and planetary rulers.
- shared/swara: swara event and transition calculations.
- shared/location: coordinate and timezone contracts.
- shared/validation: reference-vector comparison helpers.
- androidApp/presentation: Jetpack Compose screens and ViewModels.
- androidApp/platform: Android location, notifications, lifecycle.
- androidApp/data: Room cache and repositories.
- web-interop: Kotlin/JS or Kotlin/Wasm publishing boundary.
- reference-data: versioned test vectors and provenance.

Final Gradle module boundaries may be consolidated to reduce build complexity; package boundaries and dependency direction must remain clear.

## Dependency direction
- Presentation depends on domain use cases, never directly on astronomy implementation details.
- Domain models must not depend on Android, Compose, Room, or browser APIs.
- Astronomy adapters implement explicit domain-facing interfaces.
- Panchanga, Hora, and Swara use cases depend on shared astronomy contracts.
- Platform services (GPS, clock, timezone lookup, persistence) are injected through interfaces.

## Platform strategy

### Android
- Kotlin
- Jetpack Compose and Material 3
- MVVM / unidirectional data flow
- Room for scoped local persistence and cache
- WorkManager only for deferrable background work
- Android location APIs behind a permission-aware provider

### Web
The existing React/TanStack application remains operational during migration. Kotlin Multiplatform does not automatically make Kotlin code callable from TypeScript. A deliberate Kotlin/JS or Kotlin/Wasm build and versioned interop API are required. Browser compatibility, packaging, and performance must be proven before switching the web app.

## Calculation input contract
Every request should explicitly carry:
- Local civil date/time where applicable
- IANA timezone identifier
- Latitude and longitude in documented units and ranges
- Elevation in metres when required
- Ayanamsha identifier and version
- Calculation/ephemeris implementation and version
- Calendar convention (Amanta or Purnimanta) where relevant

Do not infer coordinates from a city label when precise coordinates are available. Store selected location and timezone separately.

## Accuracy and reproducibility
- Use deterministic inputs and stable output serialization.
- Preserve source engine outputs as golden vectors before porting each feature.
- Compare event instants as UTC instants and verify local formatted display values separately.
- Document tolerances per calculation; do not use one blanket tolerance.
- Every intentional discrepancy requires an architecture decision record and updated reference vectors.

## Initial navigation
1. Daily Panchanga
2. Vedic Hora
3. Swara Yoga

Each screen must expose selected location and date context and provide loading, permission-denied, unavailable-data, and calculation-error states.
