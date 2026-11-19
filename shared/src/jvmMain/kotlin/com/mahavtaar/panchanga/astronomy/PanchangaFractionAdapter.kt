package com.mahavtaar.panchanga.astronomy

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.sunPosition

/**
 * Supplies the source engine's tropical lunar phase fractions for Tithi/Karana.
 * Ayanamsa is intentionally not applied: both source fractions use tropical
 * Moon-Sun elongation.
 */
class PanchangaFractionAdapter {
    fun lunarPhaseAngle(epochMillis: Double): Double {
        require(epochMillis.isFinite()) { "Epoch milliseconds must be finite." }
        val time = Time.fromMillisecondsSince1970(epochMillis.toLong())
        val sunLongitude = sunPosition(time).elon
        val moonLongitude = equatorialToEcliptic(
            geoVector(Body.Moon, time, io.github.cosinekitty.astronomy.Aberration.Corrected),
        ).elon
        return normalizeDegrees(moonLongitude - sunLongitude)
    }

    fun tithiFraction(epochMillis: Double): Double =
        lunarPhaseAngle(epochMillis) / 12.0

    fun karanaFraction(epochMillis: Double): Double =
        lunarPhaseAngle(epochMillis) / 6.0

    private fun normalizeDegrees(value: Double): Double =
        ((value % 360.0) + 360.0) % 360.0
}
