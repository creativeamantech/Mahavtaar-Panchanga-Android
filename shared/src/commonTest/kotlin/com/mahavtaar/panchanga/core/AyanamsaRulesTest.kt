package com.mahavtaar.panchanga.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AyanamsaRulesTest {
    @Test
    fun allCoordinateModesMatchTheSourceOffsets() {
        val utDays = 9_500.25
        val centuries = utDays / 36_525.0
        val lahiri = 23.857092 + 1.3969713 * centuries + 0.0003086 * centuries * centuries

        assertNear(lahiri, calculateAyanamsa(utDays, AyanamsaMode.CITRA))
        assertNear(lahiri - 0.25, calculateAyanamsa(utDays, AyanamsaMode.REVATI))
        assertNear(lahiri + 1.25, calculateAyanamsa(utDays, AyanamsaMode.ROHINI))
        assertNear(lahiri - 0.5, calculateAyanamsa(utDays, AyanamsaMode.PUSHYA))
        assertNear(lahiri + 0.75, calculateAyanamsa(utDays, AyanamsaMode.MULA))
        assertNear(lahiri - 0.0980556, calculateAyanamsa(utDays, AyanamsaMode.KRISHNAMURTI))
        assertNear(lahiri - 1.4666667, calculateAyanamsa(utDays, AyanamsaMode.RAMAN))
        assertEquals(0.0, calculateAyanamsa(utDays, AyanamsaMode.TROPICAL))
    }

    @Test
    fun siderealLongitudeWrapsIntoZeroTo360Range() {
        assertNear(359.0, toSiderealLongitude(1.0, 2.0))
        assertNear(1.0, toSiderealLongitude(359.0, 358.0))
        assertTrue(toSiderealLongitude(0.0, 24.0) in 0.0..360.0)
    }

    private fun assertNear(expected: Double, actual: Double) {
        assertTrue(kotlin.math.abs(expected - actual) < 1e-10)
    }
}
