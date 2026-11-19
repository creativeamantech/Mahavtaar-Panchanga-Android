package com.mahavtaar.panchanga.core

data class TattvaPeriod(
    val name: String,
    val sanskrit: String,
    val durationMs: Double,
    val durationMins: Int,
    val startTime: String,
    val endTime: String,
    val startTimeMs: Double,
    val endTimeMs: Double,
)

data class Hora(
    val index: Int,
    val isDay: Boolean,
    val startTime: String,
    val endTime: String,
    val startTimeMs: Double,
    val endTimeMs: Double,
    val durationMs: Double,
    val ruler: String,
    val nadi: Nadi,
    val tattvas: List<TattvaPeriod>,
)

data class DailyHoras(
    val date: String,
    val horas: List<Hora>,
    val activeHora: Hora?,
    val activeTattva: TattvaPeriod?,
)

private val HORA_LORDS = listOf("Sun", "Venus", "Mercury", "Moon", "Saturn", "Jupiter", "Mars")
private val WEEKDAY_START_INDEX = listOf(0, 3, 6, 2, 5, 1, 4)

private data class TattvaDefinition(
    val name: String,
    val sanskrit: String,
    val ratio: Double,
)

private val TATTVA_DEFINITIONS = listOf(
    TattvaDefinition("Space", "Akasha", 1.0 / 15.0),
    TattvaDefinition("Air", "Vayu", 2.0 / 15.0),
    TattvaDefinition("Fire", "Agni", 3.0 / 15.0),
    TattvaDefinition("Earth", "Prithvi", 5.0 / 15.0),
    TattvaDefinition("Water", "Jala", 4.0 / 15.0),
)

/**
 * Direct port of the active app's computeDailyHoras.
 * formatTime is injected so timezone conversion remains a platform concern;
 * Android should pass a java.time formatter using the selected IANA timezone.
 */
fun computeDailyHoras(
    date: String,
    sunriseMs: Double,
    sunsetMs: Double,
    nextSunriseMs: Double,
    weekday: Int,
    tithiCycleNumber: Int,
    currentTimeMs: Double,
    formatTime: (Double) -> String,
): DailyHoras {
    require(sunriseMs.isFinite() && sunsetMs.isFinite() && nextSunriseMs.isFinite()) {
        "Invalid astronomical timestamps provided for Hora calculation."
    }
    require(sunriseMs < sunsetMs && sunsetMs < nextSunriseMs) {
        "Invalid chronological ordering of sunrise and sunset."
    }
    require(weekday in 0..6) { "Weekday must be between 0 and 6." }
    require(currentTimeMs.isFinite()) { "Current time must be finite." }

    val initialNadi = getTithiSwaraRule(tithiCycleNumber).startNadi
    val dayHoraDuration = (sunsetMs - sunriseMs) / 12.0
    val nightHoraDuration = (nextSunriseMs - sunsetMs) / 12.0
    val horas = mutableListOf<Hora>()
    var activeHora: Hora? = null
    var activeTattva: TattvaPeriod? = null

    for (i in 0 until 24) {
        val isDay = i < 12
        val duration = if (isDay) dayHoraDuration else nightHoraDuration
        val start = if (i == 0) sunriseMs else horas[i - 1].endTimeMs
        val end = when (i) {
            11 -> sunsetMs
            23 -> nextSunriseMs
            else -> start + duration
        }

        val tattvas = mutableListOf<TattvaPeriod>()
        var tattvaStart = start
        TATTVA_DEFINITIONS.forEachIndexed { index, definition ->
            val tattvaEnd = if (index == TATTVA_DEFINITIONS.lastIndex) end
                else tattvaStart + duration * definition.ratio
            val period = TattvaPeriod(
                name = definition.name,
                sanskrit = definition.sanskrit,
                durationMs = tattvaEnd - tattvaStart,
                durationMins = ((tattvaEnd - tattvaStart) / 60_000.0 + 0.5).toInt(),
                startTime = formatTime(tattvaStart),
                endTime = formatTime(tattvaEnd),
                startTimeMs = tattvaStart,
                endTimeMs = tattvaEnd,
            )
            tattvas += period
            if (currentTimeMs >= tattvaStart && currentTimeMs < tattvaEnd) activeTattva = period
            tattvaStart = tattvaEnd
        }

        val hora = Hora(
            index = i + 1,
            isDay = isDay,
            startTime = formatTime(start),
            endTime = formatTime(end),
            startTimeMs = start,
            endTimeMs = end,
            durationMs = end - start,
            ruler = HORA_LORDS[(WEEKDAY_START_INDEX[weekday] + i) % 7],
            nadi = if (i % 2 == 0) initialNadi else initialNadi.opposite(),
            tattvas = tattvas,
        )
        horas += hora
        if (currentTimeMs >= start && currentTimeMs < end) activeHora = hora
    }

    return DailyHoras(date, horas, activeHora, activeTattva)
}
