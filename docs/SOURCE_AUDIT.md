# Source Repository Audit — Mahavtaar Panchanga Android

Audit date: 2026-11-19

## Repositories inspected

- Active TypeScript application: https://github.com/creativeamantech/mahavtaarpanchanga
- Referenced upstream/original project: https://github.com/creativeamantech/Mahavtaar-Panchangaa
- Android target: https://github.com/creativeamantech/Mahavtaar-Panchanga-Android

Both source repositories are public. The active TypeScript repository's README explicitly points to the upstream/original repository, so both must remain part of the migration audit.

## Confirmed source architecture

### Active TypeScript application

- React 19 / TanStack Start / TypeScript.
- `astronomy-engine` dependency: `^2.1.19`.
- Main Panchanga calculation module: `src/lib/panchangaEngine.server.ts` (2,945 lines; approximately 91 KB).
- Hora engine: `src/horaEngine.ts`.
- Swara rules and presentation data: `src/swaraYoga.ts`.
- Tithi-boundary Swara integration: `src/lib/tithiSwaraEngine.ts`.
- Nakshatra-boundary Swara integration: `src/lib/nakshatraSwaraEngine.ts`.
- Astronomical core also exists under `src/kundali/astronomy/`, including `AstronomicalCore.ts` and `AyanamshaProvider.ts`.
- Panchanga engine imports city and Sanskrit-name JSON datasets, plus Tithi/Nakshatra Swara engines.

The Panchanga engine exposes city lookup, custom-coordinate Panchanga calculation, city-based Panchanga calculation, planetary transitions, iCalendar generation, and Karana Tattva boundary helpers.

### Referenced upstream/original project

The active app README points to `creativeamantech/Mahavtaar-Panchangaa`. Its `ORIGINAL_README.md` describes the original Drik Panchanga implementation as a Swiss Ephemeris-based observational Indian lunisolar calendar. It documents Tithi, Nakshatra, Yoga, Karana, Vaara, sunrise/sunset, moonrise/moonset, Amanta/Purnimanta month systems, and other calendar calculations.

The upstream repository also contains a TypeScript server engine using `astronomy-engine`. Its package dependency is also `astronomy-engine ^2.1.19`. The upstream Hora implementation differs in its input model from the current active app implementation, so these versions must not be merged mechanically.

## First three Android launch modules

### 1. Daily Panchanga

Primary source: `src/lib/panchangaEngine.server.ts`.

Calculation dependency: `astronomy-engine`, city coordinate/timezone data, Sanskrit naming data, and the Tithi/Nakshatra Swara integrations.

Migration must preserve the current engine's actual conventions and output fields, including sunrise-based Panchanga day boundaries, month-system selection, ayanamsha selection, exact event timestamps, and location/timezone handling. The source file is server-marked; Android must use a platform-neutral shared API rather than importing server-only TypeScript.

### 2. Vedic Hora

Primary source: `src/horaEngine.ts`.

Confirmed rules in the inspected implementation:
- 24 Horas per sunrise-to-next-sunrise day.
- 12 day Horas divide sunrise-to-sunset; 12 night Horas divide sunset-to-next-sunrise.
- Planetary ruler sequence: Sun, Venus, Mercury, Moon, Saturn, Jupiter, Mars.
- Weekday start-index table: Sunday through Saturday = 0, 3, 6, 2, 5, 1, 4.
- Hora Nadi alternates from the sunrise Swara.
- Five Tattva periods use ratios 1/15, 2/15, 3/15, 5/15, 4/15 in the source order Space, Air, Fire, Earth, Water.
- The current implementation uses absolute millisecond timestamps and explicit timezone formatting.

The upstream Hora module accepts hour-of-day inputs while the active app module accepts absolute timestamps and a timezone. The Android contract should be based on the active app's behavior, with upstream used as historical/reference evidence only.

### 3. Swara Yoga

Primary source: `src/swaraYoga.ts`.

Supporting event engines:
- `src/lib/tithiSwaraEngine.ts`: exact 30-Tithi mapping; one-hour start and end windows; end Nadi is opposite of start Nadi; absolute Unix milliseconds.
- `src/lib/nakshatraSwaraEngine.ts`: 28-Nakshatra alignment rules based on the parent Rashi; one-hour start/end windows; opposite Nadi at the end.

The 30-Tithi Swara table is explicit and should be ported as data, not re-created from a compressed formula unless exhaustive equivalence tests prove the same output. The 28-Nakshatra table likewise needs direct parity tests.

## Critical migration constraints

1. Do not replace the astronomical library or change algorithms just to make a Kotlin port easier.
2. Do not assume `astronomy-engine` can be called directly from Kotlin Multiplatform common code. Select and validate a KMP-compatible implementation or a carefully isolated platform adapter after an API/accuracy audit.
3. Treat the original Swiss Ephemeris documentation and current Astronomy Engine implementation as separate reference lineages. Record which lineage each output currently follows.
4. Preserve Julian-day/time-scale handling, observer coordinates, sunrise/sunset search behavior, root-finding tolerances, ayanamsha formulae, and local-time formatting semantics.
5. Keep all event boundaries as absolute instants; format them only at the presentation boundary using the requested IANA timezone.
6. Never use approximate placeholder astronomical results in production UI.
7. Port one calculation slice at a time and compare against the TypeScript implementation using fixed golden vectors before enabling it in Android.
8. Existing tests found include Hora resolver tests, Tithi/Nakshatra boundary tests, and Tithi/Nakshatra Swara tests. The available test suite must be run and expanded; test filenames alone are not evidence of passing tests.

## Recommended implementation sequence

1. Freeze a versioned reference corpus from the active app for a matrix of dates, locations, timezones, ayanamsha choices, month systems, and boundary cases.
2. Audit the complete transitive dependency graph of `panchangaEngine.server.ts`, including city/Sanskrit JSON datasets and helper functions.
3. Define stable Kotlin data contracts for location, civil date, calculation options, Panchanga segments, and event timestamps.
4. Port deterministic rule engines first: Swara mapping, Tithi/Nakshatra Swara windows, then Hora.
5. Select the astronomy implementation only after verifying KMP compatibility, licensing, supported bodies/events, time scales, and numerical parity.
6. Port Daily Panchanga astronomical calculations in small slices and validate each slice against the frozen corpus.
7. Build the native Jetpack Compose screens after the shared calculation contracts are stable.
8. Add CI checks for Kotlin compilation, unit tests, reference parity, lint, and Android packaging.

## Audit status

This is a source inspection, not a completed numerical validation. No Kotlin calculation code has been ported and no Android build or APK has been produced by this audit. The next engineering task is to build the versioned reference corpus and complete the astronomy dependency/API audit before choosing the Kotlin ephemeris implementation.
