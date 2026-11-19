package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.findPanchangaSegments

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
    fun tithiAndKaranaSegmentsMatchTypeScriptGoldenReferencesAcrossTenCities() {
        val corpusDir = File(System.getProperty("referenceCorpusDir"))
        val cities = listOf(
            "bhopal", "indore", "ujjain", "varanasi", "delhi",
            "chennai", "london", "new-york", "singapore", "sydney",
        )
        val dates = listOf("2026-03-20", "2026-06-21", "2026-11-19")
        val fixtureFiles = cities.flatMap { city ->
            dates.map { date -> File(corpusDir, "$city-$date.json") }
        }
        val fractions = PanchangaFractionAdapter()

        fixtureFiles.forEach { fixture ->
            val json = fixture.readText()
            val sunrise = jsonDouble(json, "sunrise_ms")
            val nextSunrise = jsonDouble(json, "next_sunrise_ms")

            val actualTithi = findPanchangaSegments(
                sample = fractions::tithiFraction,
                sunriseMs = sunrise,
                nextSunriseMs = nextSunrise,
                totalItems = 30,
            )
            val actualKarana = findPanchangaSegments(
                sample = fractions::karanaFraction,
                sunriseMs = sunrise,
                nextSunriseMs = nextSunrise,
                totalItems = 60,
            )

            assertSegmentsMatch(json, fixture.name, "tithi", actualTithi)
            assertSegmentsMatch(json, fixture.name, "karana", actualKarana)
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



    private fun assertSegmentsMatch(
        fixtureJson: String,
        fixtureName: String,
        key: String,
        actual: List<com.mahavtaar.panchanga.core.PanchangaSegmentBoundary>,
    ) {
        val expected = jsonObjects(fixtureJson, key)
        assertEquals(expected.size, actual.size, fixtureName + ": " + key + " segment count differs")
        expected.forEachIndexed { index, expectedJson ->
            val actualSegment = actual[index]
            assertEquals(
                jsonLong(expectedJson, "number").toInt(),
                actualSegment.number,
                fixtureName + ": " + key + " segment " + index + " number differs",
            )
            assertEpochNullableNear(
                fixtureName + ": " + key + " segment " + index + " start",
                jsonNullableLong(expectedJson, "startTimeMs"),
                actualSegment.startTimeMs,
            )
            assertEpochNullableNear(
                fixtureName + ": " + key + " segment " + index + " end",
                jsonNullableLong(expectedJson, "endTimeMs"),
                actualSegment.endTimeMs,
            )
        }
    }

    private fun jsonNullableLong(json: String, key: String): Long? {
        val value = jsonTopLevelValue(json, key) ?: return null
        return value.takeUnless { it == "null" }?.toDouble()?.toLong()
    }

    private fun jsonTopLevelValue(json: String, key: String): String? {
        var depth = 0
        var index = 0
        while (index < json.length) {
            val character = json[index]
            if (character == '"') {
                val keyStart = index + 1
                var end = keyStart
                var escaped = false
                while (end < json.length) {
                    val current = json[end]
                    if (escaped) escaped = false
                    else if (current == '\\\\') escaped = true
                    else if (current == '"') break
                    end++
                }
                if (depth == 0 && end < json.length && json.substring(keyStart, end) == key) {
                    var valueStart = end + 1
                    while (valueStart < json.length && json[valueStart].isWhitespace()) valueStart++
                    if (valueStart < json.length && json[valueStart] == ':') {
                        valueStart++
                        while (valueStart < json.length && json[valueStart].isWhitespace()) valueStart++
                        var valueEnd = valueStart
                        while (valueEnd < json.length && json[valueEnd] != ',' && json[valueEnd] != '}') valueEnd++
                        return json.substring(valueStart, valueEnd).trim()
                    }
                }
                index = end + 1
                continue
            }
            when (character) {
                '{', '[' -> depth++
                '}', ']' -> depth--
            }
            index++
        }
        return null
    }

    private fun assertEpochNullableNear(label: String, expected: Long?, actual: Double?) {
        if (expected == null) {
            assertNull(actual, label + " should be null")
        } else {
            assertNotNull(actual, label + " is missing")
            assertTrue(
                abs(actual - expected.toDouble()) <= MAX_BOUNDARY_DIFFERENCE_MILLIS,
                label + " differs by " + abs(actual - expected.toDouble()) + " ms",
            )
        }
    }

    private fun jsonObjects(json: String, key: String): List<String> {
        val arrayStart = Regex("\\\"$key\\\"\\s*:\\s*\\[")
            .find(json)?.range?.last?.plus(1) ?: error("Missing array fixture field: " + key)
        val objects = mutableListOf<String>()
        var depth = 0
        var objectStart = -1
        var inString = false
        var escaped = false

        for (index in arrayStart until json.length) {
            val character = json[index]
            if (inString) {
                if (escaped) escaped = false
                else if (character == '\\') escaped = true
                else if (character == '"') inString = false
                continue
            }
            when (character) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) objectStart = index + 1
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && objectStart >= 0) {
                        objects += json.substring(objectStart, index)
                        objectStart = -1
                    }
                }
                ']' -> if (depth == 0) return objects
            }
        }
        return objects
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
        const val MAX_BOUNDARY_DIFFERENCE_MILLIS = 1_000.0
    }
}
