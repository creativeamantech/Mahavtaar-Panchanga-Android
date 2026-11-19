package com.mahavtaar.panchanga.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class HoraEngineTest {
    private val formatter: (Double) -> String = { it.toLong().toString() }

    @Test
    fun generatesTwentyFourHorasWithExactSunriseAndSunsetEndpoints() {
        val sunrise = 1_700_000_000_000.0
        val sunset = sunrise + 43_200_000.0
        val nextSunrise = sunrise + 86_400_000.0
        val result = computeDailyHoras(
            date = "14/09/2026",
            sunriseMs = sunrise,
            sunsetMs = sunset,
            nextSunriseMs = nextSunrise,
            weekday = 1,
            tithiCycleNumber = 1,
            currentTimeMs = sunrise,
            formatTime = formatter,
        )

        assertEquals(24, result.horas.size)
        assertEquals(sunrise, result.horas.first().startTimeMs)
        assertEquals(sunset, result.horas[11].endTimeMs)
        assertEquals(nextSunrise, result.horas.last().endTimeMs)
        assertEquals("Moon", result.horas.first().ruler)
        assertEquals("Saturn", result.horas[1].ruler)
        assertEquals("Jupiter", result.horas[2].ruler)
        assertEquals(Nadi.IDA, result.horas.first().nadi)
        assertEquals(Nadi.PINGALA, result.horas[1].nadi)
        assertNotNull(result.activeHora)
        assertNotNull(result.activeTattva)
    }

    @Test
    fun exactHoraBoundaryBelongsToNextHora() {
        val sunrise = 1_700_000_000_000.0
        val sunset = sunrise + 43_200_000.0
        val nextSunrise = sunrise + 86_400_000.0
        val result = computeDailyHoras(
            "14/09/2026", sunrise, sunset, nextSunrise, 1, 1,
            sunrise + 3_600_000.0, formatter,
        )
        assertEquals(2, result.activeHora?.index)
    }

    @Test
    fun rejectsInvalidChronologyAndWeekday() {
        assertFailsWith<IllegalArgumentException> {
            computeDailyHoras("x", 10.0, 10.0, 20.0, 1, 1, 10.0, formatter)
        }
        assertFailsWith<IllegalArgumentException> {
            computeDailyHoras("x", 10.0, 20.0, 30.0, 7, 1, 10.0, formatter)
        }
    }

    @Test
    fun tattvaSegmentsAreContiguousAndEndAtHoraBoundary() {
        val sunrise = 1_700_000_000_000.0
        val result = computeDailyHoras(
            "x", sunrise, sunrise + 43_200_000.0, sunrise + 86_400_000.0,
            0, 16, sunrise + 1.0, formatter,
        )
        val firstHora = result.horas.first()
        assertEquals(5, firstHora.tattvas.size)
        firstHora.tattvas.zipWithNext().forEach { (a, b) ->
            assertEquals(a.endTimeMs, b.startTimeMs)
        }
        assertEquals(firstHora.endTimeMs, firstHora.tattvas.last().endTimeMs)
        assertEquals(Nadi.PINGALA, firstHora.nadi)
    }
}
