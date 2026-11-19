# Mahavtaar Panchanga — Android

A fully native Android application backed by a Kotlin Multiplatform calculation core.

## Product priorities

1. Daily Panchanga
2. Vedic Hora
3. Swara Yoga

Android UI is native Kotlin + Jetpack Compose. Shared deterministic calculation rules live in `shared/`. The app is not a WebView, React Native, or Capacitor wrapper.

## Current repository structure

- `app/`: native Android application and Jetpack Compose launch shell.
- `shared/`: Kotlin Multiplatform calculation core with JVM and JavaScript targets.
- `docs/`: architecture decisions, source audit, migration plan, reference corpus contract, and implementation notes.
- `.github/workflows/`: shared-core test/compile and Android debug-APK CI.

## Implemented so far

- Native Compose app shell with three launch destinations: Daily Panchanga, Vedic Hora, and Swara Yoga.
- Shared Kotlin Tithi Swara rules (all 30 Tithis).
- Shared Kotlin Tithi Swara event-window calculation.
- Shared Kotlin Nakshatra Swara rule helper.
- Shared Kotlin Hora schedule calculation (24 Horas, planetary sequence, day/night split, five Tattvas).
- Common tests for deterministic rule tables and Hora/Tattva boundaries.
- CI workflow for JVM tests, JS compilation, and Android debug APK assembly.

The Compose shell deliberately does not display fabricated Panchanga values. It clearly indicates that the astronomical calculation adapter and reference validation are still being integrated.

## Correctness rules

- Preserve the current TypeScript engine as the reference implementation during migration.
- Do not replace astronomical algorithms or ayanamsha conventions without documented evidence and approval.
- Compare every migrated calculation against a versioned reference corpus.
- Keep coordinates, elevation when available, local date, timezone, and daylight-saving behavior explicit.
- Never silently substitute approximate results.
- Do not claim parity until regression tests demonstrate it.

## Build

The CI workflow currently uses Gradle 9.1.0 and Java 17.

- Shared JVM tests: `gradle :shared:jvmTest`
- Shared JavaScript compilation: `gradle :shared:compileKotlinJs`
- Android debug APK: `gradle :app:assembleDebug`

Kotlin Multiplatform plugin: 2.4.21  
Android Gradle Plugin: 9.0.0  
Compose BOM: 2026.09.00  
Android compile/target SDK: 36

These versions are pinned in Gradle files and CI. CI results are the source of truth for build status.

## Next engineering milestones

1. Verify the current CI run and fix any build or test failures.
2. Capture versioned golden outputs from the active TypeScript engine.
3. Validate the official Kotlin Astronomy Engine implementation behind an Android/JVM adapter.
4. Port Daily Panchanga astronomy calculations incrementally.
5. Connect the real calculation state to the native Compose screens.
6. Produce signed release APK/AAB after numerical parity and device testing.

See `docs/SOURCE_AUDIT.md`, `docs/PANCHANGA_GOLDEN_CORPUS.md`, and `docs/SHARED_CORE_IMPLEMENTATION.md`.
