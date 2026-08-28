package com.hereliesaz.lamplight

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpeningHoursTest {

    @Test
    fun `no periods means unknown, not closed`() {
        assertNull(isOpenNow(emptyList(), DayOfWeek.MONDAY, LocalTime(12, 0)))
    }

    @Test
    fun `open during a same-day period`() {
        val periods = listOf(OpeningPeriod(openDay = 1, openTime = "09:00", closeDay = 1, closeTime = "17:00"))

        assertEquals(true, isOpenNow(periods, DayOfWeek.MONDAY, LocalTime(12, 0)))
    }

    @Test
    fun `closed before opening and after closing on the same day`() {
        val periods = listOf(OpeningPeriod(openDay = 1, openTime = "09:00", closeDay = 1, closeTime = "17:00"))

        assertEquals(false, isOpenNow(periods, DayOfWeek.MONDAY, LocalTime(8, 0)))
        assertEquals(false, isOpenNow(periods, DayOfWeek.MONDAY, LocalTime(18, 0)))
    }

    @Test
    fun `closed on a day with no matching period`() {
        val periods = listOf(OpeningPeriod(openDay = 1, openTime = "09:00", closeDay = 1, closeTime = "17:00"))

        assertEquals(false, isOpenNow(periods, DayOfWeek.TUESDAY, LocalTime(12, 0)))
    }

    @Test
    fun `a period spanning past midnight is open on both sides of the boundary`() {
        // Friday 6pm to Saturday 2am.
        val periods = listOf(OpeningPeriod(openDay = 5, openTime = "18:00", closeDay = 6, closeTime = "02:00"))

        assertEquals(true, isOpenNow(periods, DayOfWeek.FRIDAY, LocalTime(23, 0)))
        assertEquals(true, isOpenNow(periods, DayOfWeek.SATURDAY, LocalTime(1, 0)))
        assertEquals(false, isOpenNow(periods, DayOfWeek.SATURDAY, LocalTime(3, 0)))
        assertEquals(false, isOpenNow(periods, DayOfWeek.FRIDAY, LocalTime(17, 0)))
    }

    @Test
    fun `the 24-7 sentinel period is always open, any day`() {
        val periods = listOf(OpeningPeriod(openDay = 0, openTime = "00:00", closeDay = null, closeTime = null))

        DayOfWeek.entries.forEach { day ->
            assertEquals(true, isOpenNow(periods, day, LocalTime(3, 30)))
        }
    }
}
