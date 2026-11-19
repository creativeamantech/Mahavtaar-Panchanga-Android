package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.AyanamsaMode
import com.mahavtaar.panchanga.core.calculateAyanamsa
import com.mahavtaar.panchanga.core.toSiderealLongitude
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.sunPosition

/**
 * Supplies the source engine's tropical and sidereal Panchanga fractions.
 * Tithi/Karana use tropical Moon-Sun elongation; Nakshatra/Yoga use the
 * selected source-compatible ayanamsa at each sampled instant.
 */
class PanchangaFractionAdapter {
    fun lunarPhaseAngle(epochMillis: Double): Double {
        val (sunLongitude, moonLongitude) = tropicalSunMoon(epochMillis)
        return normalizeDegrees(moonLongitude - sunLongitude)
    }

    fun tithiFraction(epochMillis: Double): Double =
        lunarPhaseAngle(epochMillis) / 12.0

    fun karanaFraction(epochMillis: Double): Double =
        lunarPhaseAngle(epochMillis) / 6.0

    fun nakshatraFraction(epochMillis: Double, mode: AyanamsaMode): Double {
        val time = timeAt(epochMillis)
        val moonLongitude = tropicalMoonLongitude(time)
        val ayanamsa = calculateAyanamsa(time.ut, mode)
        return toSiderealLongitude(moonLongitude, ayanamsa) / (360.0 / 27.0)
    }

    fun yogaFraction(epochMillis: Double, mode: AyanamsaMode): Double {
        val time = timeAt(epochMillis)
        val sunLongitude = sunPosition(time).elon
        val moonLongitude = tropicalMoonLongitude(time)
        val ayanamsa = calculateAyanamsa(time.ut, mode)
        val sunSidereal = toSiderealLongitude(sunLongitude, ayanamsa)
        val moonSidereal = toSiderealLongitude(moonLongitude, ayanamsa)
        return ((sunSidereal + moonSidereal) % 360.0) / (360.0 / 27.0)
    }

    private fun tropicalSunMoon(epochMillis: Double): Pair<Double, Double> {
        val time = timeAt(epochMillis)
        return sunPosition(time).elon to tropicalMoonLongitude(time)
    }

    private fun tropicalMoonLongitude(time: Time): Double =
        equatorialToEcliptic(geoVector(Body.Moon, time, Aberration.Corrected)).elon

    private fun timeAt(epochMillis: Double): Time {
        require(epochMillis.isFinite()) { "Epoch milliseconds must be finite." }
        return Time.fromMillisecondsSince1970(epochMillis.toLong())
    }

    private fun normalizeDegrees(value: Double): Double =
        ((value % 360.0) + 360.0) % 360.0
}
