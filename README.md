# Mahavtaar Panchanga — Android

A fully native Android application backed by a Kotlin Multiplatform calculation core.

## Product priorities
1. Daily Panchanga
2. Vedic Hora
3. Swara Yoga

Android uses Kotlin + Jetpack Compose. The calculation core is designed for reuse by the existing React/TanStack web application through a Kotlin/JS or Kotlin/Wasm integration boundary. Android UI remains native.

## Correctness rules
- Preserve the current web engine as the reference implementation during migration.
- Do not replace astronomical algorithms or ayanamsha conventions without documented evidence and approval.
- Compare every migrated calculation against a versioned reference corpus.
- Keep coordinates, elevation when available, local date, timezone, and daylight-saving behavior explicit.
- Never silently substitute approximate results.
- Do not claim parity until regression tests demonstrate it.

## Planned structure
- shared/: platform-independent domain models, calculation contracts, and shared algorithms.
- androidApp/: native Android app, Jetpack Compose UI, location and notification integrations.
- web-interop/ (planned): Kotlin/JS or Kotlin/Wasm packaging and typed integration contract.
- reference-data/: curated, versioned golden test vectors and provenance.
- docs/: architecture decisions, audit findings, migration plan, and validation policy.

## Repository status
This initial commit establishes documentation and migration guardrails. It does not yet contain a buildable Android application or migrated astronomical engine.

See Architecture, Migration Plan, and Reference Validation in docs/.

Exact Gradle, Kotlin, Android Gradle Plugin, Compose, and library versions will be pinned together when the build scaffold is generated and verified in CI.
