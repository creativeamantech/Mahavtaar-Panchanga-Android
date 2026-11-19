package com.mahavtaar.panchanga.astronomy

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AstronomyEngineAdapterTest {
    @Test
    fun bhopalDayEventsMatchTypeScriptGoldenReference() {
        val fixture = File(System.getProperty("referenceCorpusDir"), "bhopal-2026-11-19.json")
        assertTrue(fixture.isFile, "Pinned TypeScript reference fixture is missing: $fixture")
        val json = fixture.readText()

        val expected = mapOf(
            "previous_sunrise_ms" to jsonLong(json, "previous_sunrise_ms"),
            "previous_sunset_ms" to jsonLong(json, "previous_sunset_ms"),
            "sunrise_ms" to jsonLong(json, "sunrise_ms"),
            "sunset_ms" to jsonLong(json, "sunset_ms"),
            "next_sunrise_ms" to jsonLong(json, "next_sunrise_ms"),
        )

        val actual = AstronomyEngineAdapter().calculateDayEvents(
            date = LocalDate.parse("2026-11-19"),
            zoneId = ZoneId.of("Asia/Kolkata"),
            latitude = 23.2599,
            longitude = 77.4126,
        )

        assertEpochNear("previous sunrise", expected.getValue("previous_sunrise_ms"), actual.previousSunrise?.toEpochMilli())
        assertEpochNear("previous sunset", expected.getValue("previous_sunset_ms"), actual.previousSunset?.toEpochMilli())
        assertEpochNear("sunrise", expected.getValue("sunrise_ms"), actual.sunrise?.toEpochMilli())
        assertEpochNear("sunset", expected.getValue("sunset_ms"), actual.sunset?.toEpochMilli())
        assertEpochNear("next sunrise", expected.getValue("next_sunrise_ms"), actual.nextSunrise?.toEpochMilli())
        assertEpochNear("moonrise", jsonLong(json, "moonrise_ms"), actual.moonrise?.toEpochMilli())
        assertEpochNear("moonset", jsonLong(json, "moonset_ms"), actual.moonset?.toEpochMilli())
    }

    @Test
    fun invalidGeographicCoordinatesAreRejected() {
        val error = runCatching {
            AstronomyEngineAdapter().calculateDayEvents(
                LocalDate.parse("2026-11-19"),
                ZoneId.of("Asia/Kolkata"),
                latitude = 91.0,
                longitude = 77.4126,
            )
        }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is IllegalArgumentException)
    }

    private fun jsonLong(json: String, key: String): Long {
        val match = Regex("\\\"$key\\\"\\s*:\\s*(-?\\d+)").find(json)
        return match?.groupValues?.get(1)?.toLong() ?: error("Missing numeric fixture field: $key")
    }

    private fun assertEpochNear(label: String, expected: Long, actual: Long?) {
        assertNotNull(actual, "$label was not found by the Kotlin Astronomy Engine")
        val difference = abs(actual - expected)
        assertTrue(difference <= MAX_EVENT_DIFFERENCE_MILLIS, "$label differs by $difference ms (expected=$expected, actual=$actual)")
    }

    private companion object {
        const val MAX_EVENT_DIFFERENCE_MILLIS = 1_500L
    }
}
