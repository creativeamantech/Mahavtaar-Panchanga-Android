package com.mahavtaar.panchanga.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SwaraRulesTest {
    @Test
    fun allThirtyTithiRulesAreExplicitAndOpposite() {
        assertEquals(30, TITHI_SWARA_RULES.size)
        TITHI_SWARA_RULES.forEachIndexed { index, rule ->
            assertEquals(index + 1, rule.tithiCycleNumber)
            assertEquals(rule.startNadi.opposite(), rule.endNadi)
        }
        assertEquals(Nadi.IDA, getTithiSwaraRule(1).startNadi)
        assertEquals(Nadi.PINGALA, getTithiSwaraRule(16).startNadi)
        assertEquals(Nadi.PINGALA, getTithiSwaraRule(30).startNadi)
    }

    @Test
    fun rejectsInvalidTithiCycle() {
        assertFailsWith<IllegalArgumentException> { getTithiSwaraRule(0) }
        assertFailsWith<IllegalArgumentException> { getTithiSwaraRule(31) }
    }

    @Test
    fun nakshatraRulesUseCanonicalParentRashiMapping() {
        assertEquals(1, getNakshatraSwaraRule(1).rashiNumber)
        assertEquals(Nadi.PINGALA, getNakshatraSwaraRule(1).startNadi)
        assertEquals(2, getNakshatraSwaraRule(4).rashiNumber)
        assertEquals(Nadi.IDA, getNakshatraSwaraRule(4).startNadi)
        assertEquals(10, getNakshatraSwaraRule(28).rashiNumber)
        assertEquals(Nadi.IDA, getNakshatraSwaraRule(28).startNadi)
        for (number in 1..28) {
            val rule = getNakshatraSwaraRule(number)
            assertEquals(rule.startNadi.opposite(), rule.endNadi)
        }
    }

    @Test
    fun rejectsInvalidNakshatraNumber() {
        assertFailsWith<IllegalArgumentException> { getNakshatraSwaraRule(0) }
        assertFailsWith<IllegalArgumentException> { getNakshatraSwaraRule(29) }
    }

    @Test
    fun tithiEventsUseAbsoluteOneHourWindowsAndHalfOpenIntervals() {
        val start = 1_000_000.0
        val end = start + 10_000_000.0
        val atStart = createTithiSwaraEvents(1, start, end, start)
        assertTrue(atStart[0].isActive)
        assertEquals(start + 3_600_000.0, atStart[0].endTimeMs)
        assertEquals(Nadi.IDA, atStart[0].nadi)

        val atStartEnd = createTithiSwaraEvents(1, start, end, start + 3_600_000.0)
        assertTrue(!atStartEnd[0].isActive)

        val atEnd = createTithiSwaraEvents(1, start, end, end)
        assertTrue(!atEnd[1].isActive)
    }

    @Test
    fun shortTithiWindowsKeepFullHourAndReportOverlap() {
        val events = createTithiSwaraEvents(1, 0.0, 1_800_000.0, 900_000.0)
        assertEquals(3_600_000.0, events[0].endTimeMs)
        assertEquals(-1_800_000.0, events[1].startTimeMs)
        assertEquals(3_600_000.0, events[0].durationMs)
        assertEquals(3_600_000.0, events[1].durationMs)
        assertTrue(events.all { it.hasOverlap })
        assertTrue(events.all { it.isActive })
    }
}
