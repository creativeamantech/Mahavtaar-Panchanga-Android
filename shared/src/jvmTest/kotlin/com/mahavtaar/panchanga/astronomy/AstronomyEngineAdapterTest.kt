package com.mahavtaar.panchanga.astronomy

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AstronomyEngineAdapterTest {
    @Test
    fun baselineDayEventsMatchTypeScriptGoldenReferencesAcrossTenCities() {
        val corpusDir = File(System.getProperty("referenceCorpusDir"))
        assertTrue(corpusDir.isDirectory, "Pinned TypeScript reference corpus is missing: $corpusDir")

        val cities = listOf(
            "bhopal", "indore", "ujjain", "varanasi", "delhi",
            "chennai", "london", "new-york", "singapore", "sydney",
        )
        val dates = listOf("2026-03-20", "2026-06-21", "2026-11-19")
        val fixtureFiles = cities.flatMap { city ->
            dates.map { date -> File(corpusDir, "$city-$date.json") }
        }

        assertEquals(30, fixtureFiles.size)
        fixtureFiles.forEach { fixture ->
            assertTrue(fixture.isFile, "Missing baseline fixture: " + fixture.name)
            assertFixtureMatches(fixture)
        }
    }


    @Test
    fun navagrahaPositionsMatchTypeScriptGoldenReferencesAcrossTenCities() {
        val corpusDir = File(System.getProperty("referenceCorpusDir"))
        val cities = listOf(
            "bhopal", "indore", "ujjain", "varanasi", "delhi",
            "chennai", "london", "new-york", "singapore", "sydney",
        )
        val dates = listOf("2026-03-20", "2026-06-21", "2026-11-19")
        val fixtureFiles = cities.flatMap { city ->
            dates.map { date -> File(corpusDir, "$city-$date.json") }
        }

        fixtureFiles.forEach { fixture ->
            assertTrue(fixture.isFile, "Missing planetary fixture: " + fixture.name)
            val json = fixture.readText()
            val expectedSunrise = jsonLong(json, "sunrise_ms")
            val ayanamsa = jsonDouble(json, "ayanamsa_degrees")
            val expectedPlanets = jsonObjects(json, "planets").associate { planet ->
                jsonString(planet, "id") to planet
            }
            val actualPlanets = PlanetaryPositionAdapter()
                .calculateAt(expectedSunrise, ayanamsa)
                .associateBy { it.id }

            assertEquals(
                setOf("sun", "moon", "mars", "mercury", "jupiter", "venus", "saturn", "rahu", "ketu"),
                expectedPlanets.keys,
                fixture.name + ": source Navagraha set changed",
            )
            assertEquals(expectedPlanets.keys, actualPlanets.keys, fixture.name + ": Kotlin Navagraha set differs")

            expectedPlanets.forEach { (id, expectedJson) ->
                val actual = actualPlanets.getValue(id)
                val expectedTropical = jsonDouble(expectedJson, "longitude")
                val expectedSidereal = jsonDouble(expectedJson, "siderealLongitude")
                val expectedRetrograde = jsonBoolean(expectedJson, "isRetrograde")

                assertDegreesNear(fixture.name + ": " + id + " tropical", expectedTropical, actual.tropicalLongitude)
                assertDegreesNear(fixture.name + ": " + id + " sidereal", expectedSidereal, actual.siderealLongitude)
                assertEquals(expectedRetrograde, actual.isRetrograde, fixture.name + ": " + id + " retrograde flag differs")
            }
        }
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

    private fun assertFixtureMatches(fixture: File) {
        val json = fixture.readText()
        val date = LocalDate.parse(jsonString(json, "date"))
        val zoneId = ZoneId.of(jsonString(json, "timezone"))
        val latitude = jsonDouble(json, "latitude")
        val longitude = jsonDouble(json, "longitude")

        val actual = AstronomyEngineAdapter().calculateDayEvents(
            date = date,
            zoneId = zoneId,
            latitude = latitude,
            longitude = longitude,
        )

        listOf(
            "previous_sunrise_ms" to actual.previousSunrise?.toEpochMilli(),
            "previous_sunset_ms" to actual.previousSunset?.toEpochMilli(),
            "sunrise_ms" to actual.sunrise?.toEpochMilli(),
            "sunset_ms" to actual.sunset?.toEpochMilli(),
            "next_sunrise_ms" to actual.nextSunrise?.toEpochMilli(),
        ).forEach { (key, value) ->
            assertEpochNear(fixture.name + ": " + key, jsonLong(json, key), value)
        }

        val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
        assertFixtureMoonEvent(json, fixture.name, "moonrise", actual.moonrise, zoneId, timeFormat)
        assertFixtureMoonEvent(json, fixture.name, "moonset", actual.moonset, zoneId, timeFormat)
    }

    private fun assertFixtureMoonEvent(
        json: String,
        fixtureName: String,
        key: String,
        actual: java.time.Instant?,
        zoneId: ZoneId,
        formatter: DateTimeFormatter,
    ) {
        val status = jsonString(json, key + "_status")
        val expected = jsonNullableString(json, key)
        if (status == "ok" && expected != null) {
            assertNotNull(actual, fixtureName + ": " + key + " was not found by Kotlin")
            assertEquals(expected, formatter.format(actual.atZone(zoneId)), fixtureName + ": " + key + " differs")
        } else {
            assertNull(actual, fixtureName + ": unexpected " + key + " when source status is " + status)
        }
    }

    private fun jsonString(json: String, key: String): String =
        Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
            .find(json)?.groupValues?.get(1) ?: error("Missing string fixture field: " + key)

    private fun jsonNullableString(json: String, key: String): String? {
        val match = Regex("\"$key\"\\s*:\\s*(null|\"([^\"]*)\")").find(json)
            ?: error("Missing nullable string fixture field: " + key)
        return match.groupValues[2].ifEmpty { null }
    }

    private fun jsonDouble(json: String, key: String): Double =
        Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
            .find(json)?.groupValues?.get(1)?.toDouble()
            ?: error("Missing numeric fixture field: " + key)

    private fun jsonLong(json: String, key: String): Long =
        Regex("\"$key\"\\s*:\\s*(-?\\d+)")
            .find(json)?.groupValues?.get(1)?.toLong()
            ?: error("Missing integer fixture field: " + key)


    private fun jsonObjects(json: String, key: String): List<String> {
        val array = Regex("\\\"$key\\\"\\s*:\\s*\\[(.*?)]", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(json)?.groupValues?.get(1) ?: error("Missing array fixture field: " + key)
        return Regex("\\{([^{}]*)\\}", setOf(RegexOption.DOT_MATCHES_ALL))
            .findAll(array).map { it.groupValues[1] }.toList()
    }

    private fun jsonBoolean(json: String, key: String): Boolean =
        Regex("\\\"$key\\\"\\s*:\\s*(true|false)")
            .find(json)?.groupValues?.get(1)?.toBoolean()
            ?: error("Missing boolean fixture field: " + key)

    private fun assertDegreesNear(label: String, expected: Double, actual: Double) {
        val difference = abs(actual - expected)
        assertTrue(
            difference <= MAX_LONGITUDE_DIFFERENCE_DEGREES,
            label + " differs by " + difference + " degrees (expected=" + expected + ", actual=" + actual + ")",
        )
    }

    private fun assertEpochNear(label: String, expected: Long, actual: Long?) {
        assertNotNull(actual, label + " was not found by Kotlin Astronomy Engine")
        val difference = abs(actual - expected)
        assertTrue(
            difference <= MAX_EVENT_DIFFERENCE_MILLIS,
            label + " differs by " + difference + " ms (expected=" + expected + ", actual=" + actual + ")",
        )
    }

    private companion object {
        const val MAX_EVENT_DIFFERENCE_MILLIS = 1_500L
        const val MAX_LONGITUDE_DIFFERENCE_DEGREES = 0.000001
    }
}
