package com.mahavtaar.panchanga.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PanchangaBoundarySolverTest {
    @Test
    fun bisectionFindsAContinuousBoundary() {
        val crossing = findBoundaryCrossing(
            sample = { timeMs -> timeMs / HOUR_MILLIS },
            target = 1.25,
            startTimeMs = 0.0,
            endTimeMs = 3.0 * HOUR_MILLIS,
        )
        assertNotNull(crossing)
        assertTrue(abs(crossing - 1.25 * HOUR_MILLIS) <= 1_000.0)
    }

    @Test
    fun bisectionFindsBoundaryAcrossAngularZero() {
        val crossing = findBoundaryCrossing(
            sample = { timeMs -> (359.0 + timeMs / HOUR_MILLIS) % 360.0 },
            target = 0.5,
            startTimeMs = 0.0,
            endTimeMs = 2.0 * HOUR_MILLIS,
        )
        assertNotNull(crossing)
        assertTrue(abs(crossing - 1.5 * HOUR_MILLIS) <= 1_000.0)
    }

    @Test
    fun createsCurrentAndNextSegmentsWithinSunriseDay() {
        val segments = findPanchangaSegments(
            sample = { timeMs -> timeMs / HOUR_MILLIS },
            sunriseMs = 2.5 * HOUR_MILLIS,
            nextSunriseMs = 5.5 * HOUR_MILLIS,
            totalItems = 30,
        )
        assertEquals(2, segments.size)
        assertEquals(3, segments[0].number)
        assertEquals(2.0 * HOUR_MILLIS, segments[0].startTimeMs)
        assertNear(3.0 * HOUR_MILLIS, segments[0].endTimeMs)
        assertEquals(4, segments[1].number)
        assertNear(3.0 * HOUR_MILLIS, segments[1].startTimeMs)
        assertNear(4.0 * HOUR_MILLIS, segments[1].endTimeMs)
    }

    @Test
    fun retainsOpenEndedSegmentWhenNoBoundaryOccursBeforeNextSunrise() {
        val segments = findPanchangaSegments(
            sample = { timeMs -> timeMs / HOUR_MILLIS },
            sunriseMs = 2.5 * HOUR_MILLIS,
            nextSunriseMs = 2.8 * HOUR_MILLIS,
            totalItems = 30,
        )
        assertEquals(1, segments.size)
        assertEquals(3, segments.single().number)
        assertEquals(2.0 * HOUR_MILLIS, segments.single().startTimeMs)
        assertNull(segments.single().endTimeMs)
    }

    @Test
    fun wrapsNextSegmentNumberAfterLastItem() {
        val segments = findPanchangaSegments(
            sample = { timeMs -> 29.5 + timeMs / HOUR_MILLIS },
            sunriseMs = 0.5 * HOUR_MILLIS,
            nextSunriseMs = 2.5 * HOUR_MILLIS,
            totalItems = 30,
        )
        assertEquals(listOf(30, 1), segments.map { it.number })
    }

    private fun assertNear(expected: Double, actual: Double?) {
        assertNotNull(actual)
        assertTrue(abs(expected - actual) <= 1_000.0, "Expected $expected but was $actual")
    }

    private companion object {
        const val HOUR_MILLIS = 3_600_000.0
    }
}
