package com.mahavtaar.panchanga.astronomy

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.sunPosition

/**
 * Tropical and sidereal longitude values produced by the source-compatible
 * planetary calculation at one instant.
 */
data class PlanetaryPosition(
    val id: String,
    val tropicalLongitude: Double,
    val siderealLongitude: Double,
    val isRetrograde: Boolean,
)

/**
 * JVM adapter for the Navagraha longitude calculations in the TypeScript engine.
 *
 * The ayanamsa is injected from the same sunrise instant and coordinate mode
 * calculation used by the reference engine; this adapter does not select or
 * approximate an ayanamsa on its own.
 */
class PlanetaryPositionAdapter {
    fun calculateAt(
        epochMillis: Long,
        ayanamsaDegrees: Double,
    ): List<PlanetaryPosition> {
        require(ayanamsaDegrees.isFinite()) { "Ayanamsa must be finite." }

        val time = Time.fromMillisecondsSince1970(epochMillis)
        val nextHour = Time.fromMillisecondsSince1970(epochMillis + ONE_HOUR_MILLIS)

        val sunLongitude = sunPosition(time).elon
        val moonLongitude = eclipticLongitude(Body.Moon, time)

        val bodies = listOf(
            "sun" to Body.Sun,
            "moon" to Body.Moon,
            "mars" to Body.Mars,
            "mercury" to Body.Mercury,
            "jupiter" to Body.Jupiter,
            "venus" to Body.Venus,
            "saturn" to Body.Saturn,
        )

        val positions = bodies.map { (id, body) ->
            val longitude = when (body) {
                Body.Sun -> sunLongitude
                Body.Moon -> moonLongitude
                else -> eclipticLongitude(body, time)
            }
            val retrograde = if (body == Body.Sun || body == Body.Moon) {
                false
            } else {
                val nextLongitude = eclipticLongitude(body, nextHour)
                angularDifference(nextLongitude, longitude) < 0.0
            }
            PlanetaryPosition(
                id = id,
                tropicalLongitude = longitude,
                siderealLongitude = normalizeDegrees(longitude - ayanamsaDegrees),
                isRetrograde = retrograde,
            )
        }

        val centuriesSinceJ2000 = time.ut / 36525.0
        var rahuLongitude =
            (125.04452 - 1934.136261 * centuriesSinceJ2000 +
                0.0020708 * centuriesSinceJ2000 * centuriesSinceJ2000) % 360.0
        if (rahuLongitude < 0.0) rahuLongitude += 360.0
        val ketuLongitude = (rahuLongitude + 180.0) % 360.0

        return positions + listOf(
            PlanetaryPosition(
                id = "rahu",
                tropicalLongitude = rahuLongitude,
                siderealLongitude = normalizeDegrees(rahuLongitude - ayanamsaDegrees),
                isRetrograde = true,
            ),
            PlanetaryPosition(
                id = "ketu",
                tropicalLongitude = ketuLongitude,
                siderealLongitude = normalizeDegrees(ketuLongitude - ayanamsaDegrees),
                isRetrograde = true,
            ),
        )
    }

    private fun eclipticLongitude(body: Body, time: Time): Double =
        equatorialToEcliptic(geoVector(body, time, Aberration.Corrected)).elon

    private fun angularDifference(next: Double, current: Double): Double {
        var difference = (next - current) % 360.0
        if (difference > 180.0) difference -= 360.0
        if (difference < -180.0) difference += 360.0
        return difference
    }

    private fun normalizeDegrees(value: Double): Double =
        ((value % 360.0) + 360.0) % 360.0

    private companion object {
        const val ONE_HOUR_MILLIS = 3_600_000L
    }
}
