package com.mahavtaar.panchanga.core

enum class Nadi { IDA, PINGALA }

data class TithiSwaraRule(
    val paksha: Paksha,
    val tithiNumber: Int,
    val tithiCycleNumber: Int,
    val startNadi: Nadi,
    val endNadi: Nadi,
)

enum class Paksha { SHUKLA, KRISHNA }

fun Nadi.opposite(): Nadi = if (this == Nadi.IDA) Nadi.PINGALA else Nadi.IDA

/**
 * Direct port of src/lib/tithiSwaraEngine.ts.
 * The explicit 30-row table is deliberately retained as data to prevent
 * accidental changes caused by compressing the source rules into a formula.
 */
val TITHI_SWARA_RULES: List<TithiSwaraRule> = listOf(
    TithiSwaraRule(Paksha.SHUKLA, 1, 1, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.SHUKLA, 2, 2, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.SHUKLA, 3, 3, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.SHUKLA, 4, 4, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.SHUKLA, 5, 5, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.SHUKLA, 6, 6, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.SHUKLA, 7, 7, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.SHUKLA, 8, 8, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.SHUKLA, 9, 9, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.SHUKLA, 10, 10, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.SHUKLA, 11, 11, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.SHUKLA, 12, 12, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.SHUKLA, 13, 13, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.SHUKLA, 14, 14, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.SHUKLA, 15, 15, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.KRISHNA, 1, 16, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.KRISHNA, 2, 17, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.KRISHNA, 3, 18, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.KRISHNA, 4, 19, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.KRISHNA, 5, 20, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.KRISHNA, 6, 21, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.KRISHNA, 7, 22, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.KRISHNA, 8, 23, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.KRISHNA, 9, 24, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.KRISHNA, 10, 25, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.KRISHNA, 11, 26, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.KRISHNA, 12, 27, Nadi.IDA, Nadi.PINGALA),
    TithiSwaraRule(Paksha.KRISHNA, 13, 28, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.KRISHNA, 14, 29, Nadi.PINGALA, Nadi.IDA),
    TithiSwaraRule(Paksha.KRISHNA, 15, 30, Nadi.PINGALA, Nadi.IDA),
)

fun getTithiSwaraRule(cycleNumber: Int): TithiSwaraRule {
    require(cycleNumber in 1..30) { "Tithi cycle number must be between 1 and 30." }
    return TITHI_SWARA_RULES[cycleNumber - 1]
}

/**
 * Canonical parent-Rashi mapping copied from src/lib/nakshatraSwaraEngine.ts.
 * Abhijit (28) is assigned to Capricorn (10), matching the active TypeScript table.
 */
private val NAKSHATRA_PARENT_RASHI = listOf(
    1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5, 6, 6,
    7, 7, 8, 8, 9, 9, 9, 10, 10, 11, 11, 12, 12, 10,
)

data class NakshatraSwaraRule(
    val nakshatraNumber: Int,
    val rashiNumber: Int,
    val startNadi: Nadi,
    val endNadi: Nadi,
)

fun getNakshatraSwaraRule(nakshatraNumber: Int): NakshatraSwaraRule {
    require(nakshatraNumber in 1..28) { "Nakshatra number must be between 1 and 28." }
    val rashiNumber = NAKSHATRA_PARENT_RASHI[nakshatraNumber - 1]
    val start = if (rashiNumber % 2 == 1) Nadi.PINGALA else Nadi.IDA
    return NakshatraSwaraRule(nakshatraNumber, rashiNumber, start, start.opposite())
}

data class SwaraEvent(
    val type: SwaraEventType,
    val nadi: Nadi,
    val startTimeMs: Double,
    val endTimeMs: Double,
    val durationMs: Double,
    val hasOverlap: Boolean,
    val isActive: Boolean,
)

enum class SwaraEventType { START, END }

/** One-hour windows anchored to exact astronomical event boundaries. */
fun createTithiSwaraEvents(
    tithiCycleNumber: Int,
    tithiStartMs: Double,
    tithiEndMs: Double,
    nowMs: Double,
): List<SwaraEvent> {
    require(tithiStartMs.isFinite() && tithiEndMs.isFinite() && nowMs.isFinite())
    require(tithiStartMs < tithiEndMs) { "Tithi end must be after tithi start." }
    val rule = getTithiSwaraRule(tithiCycleNumber)
    val hourMs = 3_600_000.0
    val startEnd = tithiStartMs + hourMs
    val endStart = tithiEndMs - hourMs
    val hasOverlap = tithiEndMs - tithiStartMs < 2.0 * hourMs
    return listOf(
        SwaraEvent(
            SwaraEventType.START, rule.startNadi, tithiStartMs, startEnd, hourMs, hasOverlap,
            nowMs >= tithiStartMs && nowMs < startEnd,
        ),
        SwaraEvent(
            SwaraEventType.END, rule.endNadi, endStart, tithiEndMs, hourMs, hasOverlap,
            nowMs >= endStart && nowMs < tithiEndMs,
        ),
    )
}

