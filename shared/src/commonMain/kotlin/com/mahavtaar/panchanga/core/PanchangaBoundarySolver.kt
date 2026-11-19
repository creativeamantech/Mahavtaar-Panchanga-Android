package com.mahavtaar.panchanga.core

import kotlin.math.abs
import kotlin.math.floor

/**
 * One Panchanga item interval, with absolute epoch-millisecond boundaries.
 * A null end means the item remains active through the end of the sunrise day.
 */
data class PanchangaSegmentBoundary(
    val number: Int,
    val startTimeMs: Double?,
    val endTimeMs: Double?,
)

/**
 * Source-compatible bisection root finder from panchangaEngine.server.ts.
 *
 * The sampler returns an angular/fraction value. The 360-degree modular
 * difference, 30-minute bracket scan, 30 bisection iterations, and stopping
 * thresholds intentionally mirror the TypeScript implementation.
 */
fun findBoundaryCrossing(
    sample: (Double) -> Double,
    target: Double,
    startTimeMs: Double,
    endTimeMs: Double,
    maxIterations: Int = 30,
): Double? {
    require(startTimeMs.isFinite() && endTimeMs.isFinite())
    require(startTimeMs <= endTimeMs)
    require(target.isFinite())
    require(maxIterations > 0)

    val period = 360.0
    val stepMs = 30.0 * 60.0 * 1000.0

    fun difference(value: Double): Double {
        var result = (value - target) % period
        if (result > period / 2.0) result -= period
        if (result < -period / 2.0) result += period
        return result
    }

    var previousTime = startTimeMs
    var previousDifference = difference(sample(previousTime))
    var bracketStart = startTimeMs
    var bracketEnd = endTimeMs
    var foundBracket = false

    var currentTime = startTimeMs + stepMs
    while (currentTime <= endTimeMs + stepMs) {
        val currentDifference = difference(sample(currentTime))
        if (
            previousDifference * currentDifference <= 0.0 &&
            abs(currentDifference - previousDifference) < period / 2.0
        ) {
            bracketStart = previousTime
            bracketEnd = currentTime
            foundBracket = true
            break
        }
        previousTime = currentTime
        previousDifference = currentDifference
        currentTime += stepMs
    }

    if (!foundBracket) return null

    var low = bracketStart
    var high = bracketEnd
    repeat(maxIterations) {
        val middle = (low + high) / 2.0
        val middleDifference = difference(sample(middle))
        if (abs(middleDifference) < 1e-6 || abs(high - low) < 1_000.0) {
            return middle
        }

        val lowDifference = difference(sample(low))
        if (lowDifference * middleDifference <= 0.0) {
            high = middle
        } else {
            low = middle
        }
    }
    return (low + high) / 2.0
}

/**
 * Finds the current and next item between sunrise and next sunrise.
 * This mirrors the TypeScript findSegments search windows and boundary rules.
 */
fun findPanchangaSegments(
    sample: (Double) -> Double,
    sunriseMs: Double,
    nextSunriseMs: Double,
    totalItems: Int,
): List<PanchangaSegmentBoundary> {
    require(sunriseMs.isFinite() && nextSunriseMs.isFinite())
    require(sunriseMs < nextSunriseMs)
    require(totalItems > 0)

    val valueAtSunrise = sample(sunriseMs)
    require(valueAtSunrise.isFinite())

    val currentNumber = (floor(valueAtSunrise).toInt() % totalItems) + 1
    val target = floor(valueAtSunrise) + 1.0

    val crossing = findBoundaryCrossing(
        sample = sample,
        target = target,
        startTimeMs = sunriseMs,
        endTimeMs = nextSunriseMs,
    )

    val startCrossing = findBoundaryCrossing(
        sample = sample,
        target = floor(valueAtSunrise),
        startTimeMs = sunriseMs - 36.0 * HOUR_MILLIS,
        endTimeMs = sunriseMs + 12.0 * HOUR_MILLIS,
    )

    if (crossing != null && crossing <= nextSunriseMs) {
        val nextNumber = (currentNumber % totalItems) + 1
        val secondCrossing = findBoundaryCrossing(
            sample = sample,
            target = target + 1.0,
            startTimeMs = crossing,
            endTimeMs = nextSunriseMs,
        )
        return listOf(
            PanchangaSegmentBoundary(currentNumber, startCrossing, crossing),
            PanchangaSegmentBoundary(
                number = nextNumber,
                startTimeMs = crossing,
                endTimeMs = secondCrossing?.takeIf { it <= nextSunriseMs },
            ),
        )
    }

    return listOf(PanchangaSegmentBoundary(currentNumber, startCrossing, null))
}

private const val HOUR_MILLIS = 3_600_000.0
