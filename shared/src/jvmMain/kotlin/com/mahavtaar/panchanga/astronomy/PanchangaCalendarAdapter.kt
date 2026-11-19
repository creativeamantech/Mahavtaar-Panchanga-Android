package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.AyanamsaMode
import com.mahavtaar.panchanga.core.MonthSystem
import com.mahavtaar.panchanga.core.toSiderealLongitude
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.sunPosition
import java.time.LocalDate
import kotlin.math.floor

/**
 * Calendar metadata calculated using the exact formulas and new-moon search
 * windows from panchangaEngine.server.ts.
 */
data class PanchangaCalendarMetadata(
    val monthSystem: MonthSystem,
    val amantaMasaNumber: Int,
    val displayMasaNumber: Int,
    val isAdhika: Boolean,
    val weekdaySundayZero: Int,
    val paksha: String,
    val sunRashiNumber: Int,
    val moonRashiNumber: Int,
    val rtuIndex: Int,
    val drikRtuIndex: Int,
    val ayanaNorth: Boolean,
    val drikAyanaNorth: Boolean,
    val julianDay: Double,
    val kaliDay: Long,
    val sakaYear: Int,
    val kaliYear: Int,
    val vikramaYear: Int,
    val samvatsaraIndex: Int,
)

/**
 * JVM-only adapter because the reference lunar-month calculation depends on
 * Astronomy Engine's new-moon phase search. It uses the sunrise ayanamsa for
 * the new-moon solar longitude, exactly as the current TypeScript source does.
 */
class PanchangaCalendarAdapter {
    fun calculate(
        date: LocalDate,
        sunriseMillis: Long,
        ayanamsaDegrees: Double,
        monthSystem: MonthSystem,
        primaryTithiNumber: Int,
    ): PanchangaCalendarMetadata {
        require(ayanamsaDegrees.isFinite())
        require(primaryTithiNumber in 1..30)

        val sunriseTime = Time.fromMillisecondsSince1970(sunriseMillis)
        val sunTropicalAtSunrise = sunPosition(sunriseTime).elon
        val moonTropicalAtSunrise = equatorialToEcliptic(
            geoVector(Body.Moon, sunriseTime, io.github.cosinekitty.astronomy.Aberration.Corrected),
        ).elon
        val sunSiderealAtSunrise = toSiderealLongitude(sunTropicalAtSunrise, ayanamsaDegrees)
        val moonSiderealAtSunrise = toSiderealLongitude(moonTropicalAtSunrise, ayanamsaDegrees)
        val sunRashiIndex = floor(sunSiderealAtSunrise / 30.0).toInt()
        val moonRashiIndex = floor(moonSiderealAtSunrise / 30.0).toInt()

        val lastNewMoon = runCatching {
            searchMoonPhase(0.0, sunriseTime.addDays(-32.0), 33.0)
        }.getOrNull() ?: sunriseTime
        val sunSiderealAtNewMoon = toSiderealLongitude(sunPosition(lastNewMoon).elon, ayanamsaDegrees)
        val solarRashiAtNewMoon = floor(sunSiderealAtNewMoon / 30.0).toInt()
        val amantaMasa = ((solarRashiAtNewMoon + 2) % 12).let { if (it == 0) 12 else it }

        val nextNewMoon = runCatching {
            searchMoonPhase(0.0, lastNewMoon.addDays(15.0), 20.0)
        }.getOrNull()
        val isAdhika = nextNewMoon?.let { next ->
            val nextSunSidereal = toSiderealLongitude(sunPosition(next).elon, ayanamsaDegrees)
            floor(nextSunSidereal / 30.0).toInt() == solarRashiAtNewMoon
        } ?: false

        val displayMasa = if (
            monthSystem == MonthSystem.PURNIMANTA &&
            primaryTithiNumber > 15 &&
            !isAdhika
        ) {
            (amantaMasa % 12) + 1
        } else {
            amantaMasa
        }

        val civilWeekday = date.dayOfWeek.value % 7
        val sakaYear = date.year - 78 - if (
            date.monthValue < 3 || (date.monthValue == 3 && date.dayOfMonth < 22)
        ) 1 else 0
        val julianDay = sunriseTime.ut + 2_451_545.0
        val kaliDay = floor(julianDay - 588_465.5).toLong()
        val tropicalRtuIndex = floor(((sunTropicalAtSunrise + 30.0) % 360.0) / 60.0).toInt()

        return PanchangaCalendarMetadata(
            monthSystem = monthSystem,
            amantaMasaNumber = amantaMasa,
            displayMasaNumber = displayMasa,
            isAdhika = isAdhika,
            weekdaySundayZero = civilWeekday,
            paksha = if (primaryTithiNumber <= 15) "Shukla" else "Krishna",
            sunRashiNumber = sunRashiIndex + 1,
            moonRashiNumber = moonRashiIndex + 1,
            rtuIndex = floor((amantaMasa - 1) / 2.0).toInt(),
            drikRtuIndex = tropicalRtuIndex,
            ayanaNorth = sunRashiIndex >= 9 || sunRashiIndex <= 2,
            drikAyanaNorth = sunTropicalAtSunrise >= 270.0 || sunTropicalAtSunrise < 90.0,
            julianDay = julianDay,
            kaliDay = kaliDay,
            sakaYear = sakaYear,
            kaliYear = sakaYear + 3_179,
            vikramaYear = sakaYear + 135,
            samvatsaraIndex = (sakaYear + 12) % 60,
        )
    }
}
