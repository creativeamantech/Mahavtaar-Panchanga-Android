package com.mahavtaar.panchanga.astronomy

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchRiseSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Astronomical event times for one local civil date.
 *
 * Missing events remain null. This adapter deliberately does not invent fallback
 * sunrise, sunset, moonrise, or moonset values for polar/no-event conditions.
 */
data class AstronomicalDayEvents(
    val localDate: LocalDate,
    val zoneId: ZoneId,
    val previousSunrise: Instant?,
    val previousSunset: Instant?,
    val sunrise: Instant?,
    val sunset: Instant?,
    val nextSunrise: Instant?,
    val moonrise: Instant?,
    val moonset: Instant?,
)

class AstronomyEngineAdapter {
    fun calculateDayEvents(
        date: LocalDate,
        zoneId: ZoneId,
        latitude: Double,
        longitude: Double,
        elevationMeters: Double = 0.0,
    ): AstronomicalDayEvents {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90 degrees." }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180 degrees." }
        require(elevationMeters.isFinite()) { "Elevation must be finite." }

        val midnight = date.atStartOfDay(zoneId).toInstant()
        val midnightMs = midnight.toEpochMilli()
        // Match the TypeScript reference engine exactly: its local-midnight
        // timestamp is shifted back by a fixed 24 hours for the previous search.
        val previousSearchStartMs = midnightMs - DAY_MILLIS
        val observer = Observer(latitude, longitude, elevationMeters)

        // Keep the TypeScript reference engine's search windows and target altitude:
        // SearchAltitude(Sun, observer, +/-1, start, 1.0 day, 0.0 degrees).
        val sunrise = searchAltitude(
            Body.Sun, observer, Direction.Rise,
            Time.fromMillisecondsSince1970(midnightMs), 1.0, 0.0,
        )
        val previousSunrise = searchAltitude(
            Body.Sun, observer, Direction.Rise,
            Time.fromMillisecondsSince1970(previousSearchStartMs), 1.0, 0.0,
        )
        val previousSunset = previousSunrise?.let {
            searchAltitude(Body.Sun, observer, Direction.Set, it, 1.0, 0.0)
        }
        val sunset = sunrise?.let {
            searchAltitude(Body.Sun, observer, Direction.Set, it, 1.0, 0.0)
        }
        val nextSunrise = sunrise?.let {
            searchAltitude(
                Body.Sun, observer, Direction.Rise,
                it.addDays(0.5), 1.0, 0.0,
            )
        }

        // The reference engine searches one local civil day for lunar horizon events.
        val moonriseCandidate = searchRiseSet(
            Body.Moon, observer, Direction.Rise,
            Time.fromMillisecondsSince1970(midnightMs), 1.0, 0.0,
        )
        val moonsetCandidate = searchRiseSet(
            Body.Moon, observer, Direction.Set,
            Time.fromMillisecondsSince1970(midnightMs), 1.0, 0.0,
        )
        // The source engine filters lunar events to [localMidnight, localMidnight + 24h).
        val endOfReferenceDayMs = midnightMs + DAY_MILLIS
        val moonrise = moonriseCandidate?.toInstantOrNull()?.takeIf {
            it.toEpochMilli() in midnightMs until endOfReferenceDayMs
        }
        val moonset = moonsetCandidate?.toInstantOrNull()?.takeIf {
            it.toEpochMilli() in midnightMs until nextMidnightMs
        }

        return AstronomicalDayEvents(
            localDate = date,
            zoneId = zoneId,
            previousSunrise = previousSunrise.toInstantOrNull(),
            previousSunset = previousSunset.toInstantOrNull(),
            sunrise = sunrise.toInstantOrNull(),
            sunset = sunset.toInstantOrNull(),
            nextSunrise = nextSunrise.toInstantOrNull(),
            moonrise = moonrise,
            moonset = moonset,
        )
    }

    private fun Time?.toInstantOrNull(): Instant? =
        this?.toMillisecondsSince1970()?.let(Instant::ofEpochMilli)

    private companion object {
        const val DAY_MILLIS = 86_400_000L
    }
}
