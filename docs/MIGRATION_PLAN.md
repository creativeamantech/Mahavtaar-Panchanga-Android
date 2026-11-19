# Migration Plan

## Phase 0 — Repository and source audit
- [ ] Inspect Android repository baseline and establish build tooling.
- [ ] Audit creativeamantech/mahavtaarpanchanga and every repository/package it imports or documents as an upstream source.
- [ ] Record astronomy library version, ephemeris model, ayanamsha implementation, timezone handling, and dependencies.
- [ ] Trace call graphs for Daily Panchanga, Vedic Hora, and Swara Yoga.
- [ ] Identify server-only APIs, data files, generated assets, and licensing constraints.
- [ ] Record CI/test commands and known reference outputs.

Exit criteria: source inventory, dependency/licence inventory, and calculation specification.

## Phase 1 — Golden reference corpus
- [ ] Capture current web-engine outputs for agreed locations, dates, and edge cases.
- [ ] Include equinox/solstice periods, lunar boundaries, sunrise/sunset boundaries, timezone transitions, and masa edge cases where supported.
- [ ] Store exact inputs, source commit, engine version, output, and provenance.
- [ ] Define per-field tolerances and exact-match requirements.

Exit criteria: reproducible baseline tests run without the production web service.

## Phase 2 — Shared Kotlin core
- [ ] Create domain models and explicit calculation input contracts.
- [ ] Implement the astronomy adapter without changing specified models or conventions.
- [ ] Port one vertical at a time: Daily Panchanga, then Hora, then Swara.
- [ ] Add unit and property tests for boundary conditions.
- [ ] Run the complete golden corpus after each migration step.

Exit criteria: all agreed vectors pass, or exceptions are documented and approved.

## Phase 3 — Native Android foundation
- [ ] Generate a verified Kotlin/Gradle Android project.
- [ ] Add Jetpack Compose, Material 3, navigation, ViewModels, and dependency injection.
- [ ] Add permission-aware location and timezone providers.
- [ ] Add Room only for data requiring persistence or offline caching.
- [ ] Implement accessibility, dynamic type, themes, and responsive layouts.

Exit criteria: app installs on emulator and physical Android device; CI assembles and tests it.

## Phase 4 — Three launch screens
- [ ] Daily Panchanga
- [ ] Vedic Hora
- [ ] Swara Yoga

Each screen uses shared use cases and shows date, selected location, and timezone.

## Phase 5 — Web interoperability
- [ ] Build shared core for Kotlin/JS or Kotlin/Wasm.
- [ ] Define a typed, versioned JavaScript API.
- [ ] Run same vectors through Kotlin and TypeScript engines.
- [ ] Integrate behind a feature flag in React.
- [ ] Compare outputs before switching the web app.

Exit criteria: supported browsers pass parity tests and consume a versioned shared-core artifact.

## Phase 6 — Release readiness
- [ ] Profile representative low-, mid-, and high-range Android devices.
- [ ] Test offline behavior and location permissions.
- [ ] Review accessibility and localization.
- [ ] Test crash and error states.
- [ ] Prepare signed APK/AAB and Play Console checklist.

## Safety rails
- Do not delete or rewrite the existing TypeScript engine during migration.
- Do not change astronomical rules as part of UI work.
- Do not merge unvalidated algorithm changes into the release branch.
- Keep each vertical migration independently reviewable and reversible.
