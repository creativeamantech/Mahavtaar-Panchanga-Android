package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.AyanamsaMode
import com.mahavtaar.panchanga.core.MonthSystem
import com.mahavtaar.panchanga.core.PanchangaSegmentBoundary
import com.mahavtaar.panchanga.core.calculateAyanamsa
import com.mahavtaar.panchanga.core.findPanchangaSegments
import io.github.cosinekitty.astronomy.Time
import java.time.LocalDate
import java.time.ZoneId

/**
 * Astronomical portion of a Panchanga day, assembled from source-compatible
 * adapters. Masa, Vaara, muhurta labels, and calendar-convention formatting
 * are intentionally not claimed by this first calculation snapshot.
 */
data class AstronomicalPanchangaSnapshot(
    val date: LocalDate,
    val zoneId: ZoneId,
    val latitude: Double,
    val longitude: Double,
    val coordinateSelection: AyanamsaMode,
    val events: AstronomicalDayEvents,
    val calendar: PanchangaCalendarMetadata,
    val ayanamsaDegrees: Double,
    val planetaryPositions: List<PlanetaryPosition>,
    val tithi: List<PanchangaSegmentBoundary>,
    val nakshatra: List<PanchangaSegmentBoundary>,
    val yoga: List<PanchangaSegmentBoundary>,
    val karana: List<PanchangaSegmentBoundary>,
)

/**
 * Orchestrates the already independently validated event, longitude,
 * ayanamsa, and Anga-boundary adapters into one immutable day snapshot.
 */
class PanchangaCalculationAdapter(
    private val eventAdapter: AstronomyEngineAdapter = AstronomyEngineAdapter(),
    private val planetAdapter: PlanetaryPositionAdapter = PlanetaryPositionAdapter(),
    private val fractionAdapter: PanchangaFractionAdapter = PanchangaFractionAdapter(),
    private val calendarAdapter: PanchangaCalendarAdapter = PanchangaCalendarAdapter(),
) {
    fun calculate(
        date: LocalDate,
        zoneId: ZoneId,
        latitude: Double,
        longitude: Double,
        coordinateSelection: AyanamsaMode = AyanamsaMode.CITRA,
        monthSystem: MonthSystem = MonthSystem.AMANTA,
        elevationMeters: Double = 0.0,
    ): AstronomicalPanchangaSnapshot {
        val events = eventAdapter.calculateDayEvents(
            date = date,
            zoneId = zoneId,
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevationMeters,
        )
        val sunrise = requireNotNull(events.sunrise) {
            "The source-compatible astronomy engine did not return sunrise for $date at $latitude,$longitude."
        }
        val nextSunrise = requireNotNull(events.nextSunrise) {
            "The source-compatible astronomy engine did not return next sunrise for $date at $latitude,$longitude."
        }

        val sunriseMillis = sunrise.toEpochMilli()
        val nextSunriseMillis = nextSunrise.toEpochMilli()
        val astronomyTime = Time.fromMillisecondsSince1970(sunriseMillis)
        val ayanamsa = calculateAyanamsa(astronomyTime.ut, coordinateSelection)
        val tithiSegments = findPanchangaSegments(
            sample = fractionAdapter::tithiFraction,
            sunriseMs = sunriseMillis.toDouble(),
            nextSunriseMs = nextSunriseMillis.toDouble(),
            totalItems = 30,
        )
        val calendar = calendarAdapter.calculate(
            date = date,
            sunriseMillis = sunriseMillis,
            ayanamsaDegrees = ayanamsa,
            monthSystem = monthSystem,
            primaryTithiNumber = tithiSegments.firstOrNull()?.number ?: 1,
        )

        return AstronomicalPanchangaSnapshot(
            date = date,
            zoneId = zoneId,
            latitude = latitude,
            longitude = longitude,
            coordinateSelection = coordinateSelection,
            events = events,
            calendar = calendar,
            ayanamsaDegrees = ayanamsa,
            planetaryPositions = planetAdapter.calculateAt(sunriseMillis, ayanamsa),
            tithi = tithiSegments,
            nakshatra = findPanchangaSegments(
                sample = { timeMs -> fractionAdapter.nakshatraFraction(timeMs, coordinateSelection) },
                sunriseMs = sunriseMillis.toDouble(),
                nextSunriseMs = nextSunriseMillis.toDouble(),
                totalItems = 27,
            ),
            yoga = findPanchangaSegments(
                sample = { timeMs -> fractionAdapter.yogaFraction(timeMs, coordinateSelection) },
                sunriseMs = sunriseMillis.toDouble(),
                nextSunriseMs = nextSunriseMillis.toDouble(),
                totalItems = 27,
            ),
            karana = findPanchangaSegments(
                sample = fractionAdapter::karanaFraction,
                sunriseMs = sunriseMillis.toDouble(),
                nextSunriseMs = nextSunriseMillis.toDouble(),
                totalItems = 60,
            ),
        )
    }
}
