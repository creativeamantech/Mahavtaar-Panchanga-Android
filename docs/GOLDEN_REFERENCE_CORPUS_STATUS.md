# Golden Reference Corpus Status

Updated: 2026-11-19

## Immutable source corpus

- Source repository: `creativeamantech/mahavtaarpanchanga`
- Exact source commit: `e65f81caa7fedad0bcbfaf59335d70d8729d39a4`
- Engine: `src/lib/panchangaEngine.server.ts`
- Astronomy dependency: `astronomy-engine 2.1.19`
- Captured fixtures: **41**
- Capture workflow: https://github.com/creativeamantech/mahavtaarpanchanga/actions/runs/44486833059
- Permanent fixtures: `reference/panchanga/v1/` at source commit `088035ddea6c9757f09e77fb4f30bd2657ef2d32`

Fixtures are generated exclusively by the TypeScript reference engine. Kotlin output is never used to populate expected values.

## Coverage

- Ten cities: Bhopal, Indore, Ujjain, Varanasi, Delhi, Chennai, London, New York, Singapore, Sydney.
- Three baseline dates per city: 2026-03-20, 2026-06-21, 2026-11-19.
- Bhopal Amanta/Purnimanta comparisons.
- All eight coordinate-selection modes.
- One historical lunar-month probe (2023-07-18); its Adhika Masa status is recorded, not assumed.

## Kotlin parity now verified

Latest successful CI run: https://github.com/creativeamantech/Mahavtaar-Panchanga-Android/actions/runs/44496472737

- **Day events — 30 baseline scenarios:** previous sunrise/sunset, sunrise, sunset, next sunrise, moonrise and moonset. Solar event instants are compared within 1,500 ms; lunar events are compared as local `HH:mm:ss`.
- **Navagraha — 30 baseline scenarios:** Sun, Moon, Mars, Mercury, Jupiter, Venus, Saturn, Rahu and Ketu. Tropical and sidereal longitudes use a (0.000001^\circ) tolerance; retrograde flags are exact.
- **Tithi and Karana — 30 baseline scenarios:** current/next segment numbers and start/end instants compared within 1,000 ms.
- **Nakshatra and Yoga — all 41 fixtures:** segment boundaries validated across all eight coordinate-selection modes within 1,000 ms.
- **Ayanamsa — all eight modes:** source formulas ported to commonMain and unit-tested.
- **Calendar metadata — all 41 fixtures:** Amanta/Purnimanta masa number, Adhika flag, weekday, Julian day, Kali day, Saka/Kali/Vikrama years, Paksha, Ayana, Drik Ayana and Sun/Moon Rashi.
- **Integrated snapshot:** Bhopal 2026-11-19 end-to-end adapter output compared against the source fixture.
- Shared JVM tests, Kotlin/JS compilation, and Android debug APK assembly all pass in the latest run.

Debug APK artifact: `mahavtaar-panchanga-debug-apk` in the latest CI run. Artifact expires on 2026-12-03.

## Implemented adapters and shared rules

- `AstronomyEngineAdapter.kt`: JVM sunrise, sunset, moonrise and moonset adapter.
- `PlanetaryPositionAdapter.kt`: nine planetary longitudes and retrograde state.
- `PanchangaFractionAdapter.kt`: tropical and sidereal fractions for all four Angas.
- `PanchangaCalendarAdapter.kt`: source-compatible lunar month and calendar metadata.
- `PanchangaCalculationAdapter.kt`: composes these into an immutable astronomical day snapshot.
- `PanchangaBoundarySolver.kt` in commonMain: source-compatible 30-minute scan and bisection segment solver.
- `AyanamsaRules.kt` and `MonthSystem.kt` in commonMain.

The snapshot is not yet the complete user-facing Panchanga response. Masa/season labels, Samvatsara names, muhurta intervals, transit events, and the remaining source response fields still need explicit parity work and localization.

## Remaining validation gates

The 41-fixture baseline is strong regression coverage, but it is not exhaustive. Before claiming complete engine parity or enabling all Daily Panchanga fields in production, add dedicated source-generated vectors for:

- Exact before/at/after Tithi, Nakshatra, Yoga and Karana transitions.
- Sunrise and local-midnight boundaries.
- DST/timezone offset transition dates in London and New York.
- Polar and high-latitude no-rise/no-set cases.
- Confirmed Adhika Masa and Kshaya Masa cases.
- Planetary transit, station and retrograde boundary cases.
- Calendar labels, muhurta intervals and all remaining fields in the source response.

Do not hand-edit expected values or regenerate them from Kotlin. A failed vector blocks parity claims. Any accepted difference requires source evidence and a documented decision.
