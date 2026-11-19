# Shared Kotlin Core and Astronomy Implementation

Date: 2026-11-19

## Shared commonMain

- Kotlin Multiplatform module targets JVM and JavaScript.
- `SwaraRules.kt` ports the explicit 30-entry Tithi-to-Nadi table and canonical 28-Nakshatra parent-Rashi mapping.
- Tithi Swara event windows preserve exact one-hour duration, overlap metadata for short Tithis, and half-open active intervals.
- `HoraEngine.kt` ports the 24-Hora day/night schedule, weekday ruler sequence, five Tattva ratios, alternating Nadi and injected time formatter.
- `AyanamsaRules.kt` ports Citra, Revati, Rohini, Pushya, Mula, Krishnamurti, Raman and Tropical modes.
- `MonthSystem.kt` defines Amanta and Purnimanta conventions.
- `PanchangaBoundarySolver.kt` ports the source 30-minute bracket scan, 360-degree modular difference, bisection refinement, current/next segment rules and open-ended intervals.

## JVM astronomy adapters

Astronomy Engine Kotlin remains JVM-only and is isolated behind adapters; no astronomy dependency is placed in commonMain.

- `AstronomyEngineAdapter.kt`: source-compatible sunrise, sunset, previous/next sunrise, moonrise and moonset search windows.
- `PlanetaryPositionAdapter.kt`: Sun, Moon, Mars, Mercury, Jupiter, Venus, Saturn, Rahu and Ketu positions, including retrograde flags.
- `PanchangaFractionAdapter.kt`: tropical Tithi/Karana and selected-ayanamsa Nakshatra/Yoga fractions.
- `PanchangaCalendarAdapter.kt`: new-moon search, Amanta/Purnimanta masa number, Adhika flag, weekday, era values, Rashi and Ayana metadata.
- `PanchangaCalculationAdapter.kt`: immutable day snapshot that composes the validated event, planet, Anga and calendar adapters.

The day snapshot deliberately throws when sunrise or next sunrise is unavailable; it never substitutes fabricated times.

## Validation

Latest full successful run: https://github.com/creativeamantech/Mahavtaar-Panchanga-Android/actions/runs/44496472737

- 30 baseline city/date fixtures: astronomical day events and nine Navagraha positions.
- 30 baseline fixtures: Tithi and Karana segments.
- All 41 fixtures: Nakshatra and Yoga segments, eight coordinate modes, and calendar metadata.
- Bhopal 2026-11-19: combined snapshot regression.
- Common tests cover the 30-Tithi Swara table, Hora schedule, Ayanamsa modes, angular wrap-around, boundary refinement, open-ended segments and item-number wrap.
- CI passes JVM tests, Kotlin/JS compilation and Android debug APK assembly.

Comparison policies are per-field: solar event instants within 1,500 ms, lunar display times exact to the second, planetary longitude within (0.000001^\circ), and Anga boundaries within 1,000 ms.

## Important constraints

1. The TypeScript engine at source commit `e65f81caa7fedad0bcbfaf59335d70d8729d39a4` remains the reference.
2. The fixed 24-hour previous-day and lunar-event search windows intentionally mirror the current source engine, including its DST semantics. Do not silently replace them with local-civil-day windows.
3. The sunrise ayanamsa is reused for new-moon solar Rashi exactly as the source currently does.
4. Time formatting and timezone presentation remain platform concerns; commonMain must not infer the device timezone.
5. The 41 fixtures do not yet prove transition-adjacent, DST, polar, confirmed Adhika/Kshaya, or full-response parity.
6. The snapshot is an astronomical calculation slice, not the complete Daily Panchanga response. Remaining fields and localized labels must be ported and validated before production UI integration.
7. Do not hand-edit golden expected values. Only source-engine-generated fixtures may be used as expected output.
