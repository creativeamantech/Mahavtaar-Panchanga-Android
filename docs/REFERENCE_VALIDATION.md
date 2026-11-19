# Reference Validation Policy

## Purpose
The current Mahavtaar Panchanga web engine is the temporary reference implementation while the shared Kotlin core is developed. Reference results are regression evidence, not proof that the reference implementation is infallible.

## Required metadata per vector
- Stable vector ID
- Source repository and commit SHA
- Engine and dependency versions
- Input local date/time (ISO 8601)
- IANA timezone
- Latitude and longitude
- Elevation, if used
- Ayanamsha and calendar convention
- Expected output payload
- Capture timestamp and provenance
- Per-field comparison policy

## Comparison rules
- Compare absolute event instants, not formatted strings alone.
- Compare local display strings separately using the same IANA timezone.
- Require exact equality for categorical values unless a documented boundary rule applies.
- Define numerical tolerances from the source algorithm precision; never invent a tolerance to make a failing test pass.
- Test just before, at, and just after transitions.
- Preserve raw reference output alongside normalized fixtures.

## Required test dimensions
- Multiple latitudes and longitudes, including equatorial and high-latitude locations.
- Multiple timezones and dates around timezone offset changes.
- Sunrise/sunset and moonrise/moonset edge cases, including no-event conditions.
- Tithi, nakshatra, yoga, and karana transition boundaries.
- Amanta and Purnimanta conventions where applicable.
- Adhika masa and kshaya masa cases where supported.
- Hora sequence across sunrise, sunset, midnight, and next sunrise.
- Swara event ordering and transition boundaries.

## Approval
A failed vector blocks parity claims. Changing an expected value requires a documented reason, source evidence, reviewer approval, and a history-preserving fixture update.
