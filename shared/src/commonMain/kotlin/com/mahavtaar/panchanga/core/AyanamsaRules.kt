package com.mahavtaar.panchanga.core

/** Coordinate/ayanamsa modes supported by the TypeScript reference engine. */
enum class AyanamsaMode {
    CITRA,
    REVATI,
    ROHINI,
    PUSHYA,
    MULA,
    KRISHNAMURTI,
    RAMAN,
    TROPICAL,
}

/**
 * Direct port of calculateAyanamsa from panchangaEngine.server.ts.
 * utDaysSinceJ2000 is Astronomy Engine's UT day count from JD 2451545.0.
 */
fun calculateAyanamsa(
    utDaysSinceJ2000: Double,
    mode: AyanamsaMode,
): Double {
    require(utDaysSinceJ2000.isFinite()) { "UT day count must be finite." }
    if (mode == AyanamsaMode.TROPICAL) return 0.0

    val centuries = utDaysSinceJ2000 / 36_525.0
    val lahiri = 23.857092 + 1.3969713 * centuries + 0.0003086 * centuries * centuries
    return when (mode) {
        AyanamsaMode.CITRA -> lahiri
        AyanamsaMode.REVATI -> lahiri - 0.25
        AyanamsaMode.ROHINI -> lahiri + 1.25
        AyanamsaMode.PUSHYA -> lahiri - 0.5
        AyanamsaMode.MULA -> lahiri + 0.75
        AyanamsaMode.KRISHNAMURTI -> lahiri - 0.0980556
        AyanamsaMode.RAMAN -> lahiri - 1.4666667
        AyanamsaMode.TROPICAL -> 0.0
    }
}

fun toSiderealLongitude(tropicalLongitude: Double, ayanamsaDegrees: Double): Double {
    require(tropicalLongitude.isFinite() && ayanamsaDegrees.isFinite())
    return ((tropicalLongitude - ayanamsaDegrees) % 360.0 + 360.0) % 360.0
}
