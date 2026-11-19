# Shared Kotlin Core — First Implementation Slice

Date: 2026-11-19

## Implemented

- Kotlin Multiplatform module with JVM and JavaScript targets.
- Common Kotlin models for Tithi Swara and Hora.
- Explicit 30-entry Tithi-to-Nadi table ported from the active TypeScript engine's `src/lib/tithiSwaraEngine.ts`.
- Tithi Swara event-window helper using absolute millisecond instants, exact one-hour windows, explicit overlap metadata for Tithis shorter than two hours, and half-open active intervals.
- Nakshatra Swara start/end helper based on parent Rashi parity, matching the inspected source rules.
- Hora schedule generator ported from the active app's `src/horaEngine.ts`:
  - 24 Horas from sunrise to next sunrise.
  - 12 day and 12 night Horas.
  - Source weekday start indices and planetary sequence.
  - Source five Tattva ratios and order.
  - Alternating Nadi from the sunrise Tithi Swara.
  - Injected time formatter so timezone conversion remains outside commonMain.
- Common tests for the complete Tithi table, Nadi opposition, Tithi event windows, Hora boundaries, weekday validation, ruler sequence, and Tattva continuity.
- GitHub Actions workflow for JVM tests and JS compilation.

## Important implementation notes

1. Astronomical Tithi and Nakshatra boundaries are still supplied by the astronomical engine; this module does not approximate them.
2. The common core uses absolute timestamp values as `Double` to preserve the source engine's numeric representation and avoid silently rounding fractional milliseconds.
3. Time formatting is injected through a callback. Android must format using the selected IANA timezone; common code must not use the device default timezone implicitly.
4. Tithi Swara windows intentionally remain exactly one hour even when the Tithi is shorter; the source marks overlap instead of clamping either window. This behavior is covered by a regression test.\n5. The active source Hora engine calls `computeSwaraYoga(tithiNum)`; this port uses the exact canonical 30-Tithi table that the source Swara engine documents and uses. Before integration, the fixture capture must confirm whether the input `tithiNum` is always the 1..30 cycle number and detect any edge mismatch.
6. Nakshatra helper accepts both nakshatra number and its parent Rashi number. The astronomical engine remains responsible for deriving the Rashi at the actual boundary; no assumed mapping is used here.
7. No astronomy dependency has yet been added to commonMain. The Kotlin Astronomy Engine candidate is JVM-only and remains behind a future Android/JVM adapter decision.

## Build verification

The source and CI workflow have been committed, but the GitHub Actions run has not yet executed in this task. Do not interpret committed tests as passing tests until the workflow reports success.
