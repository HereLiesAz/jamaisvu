package com.hereliesaz.lamplight

import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningHoursTest {

    @Test
    fun `no periods means unknown, not closed`() {
        assertNull(isOpenNow(emptyList(), DayOfWeek.MONDAY, LocalTime.of(12, 0)))
    }

    @Test
    fun `open during a same-day period`() {
        val periods = listOf(OpeningPeriod(openDay = 1, openTime = "09:00", closeDay = 1, closeTime = "17:00"))

        assertEquals(true, isOpenNow(periods, DayOfWeek.MONDAY, LocalTime.of(12, 0)))
    }

    @Test
    fun `closed before opening and after closing on the same day`() {
        val periods = listOf(OpeningPeriod(openDay = 1, openTime = "09:00", closeDay = 1, closeTime = "17:00"))

        assertEquals(false, isOpenNow(periods, DayOfWeek.MONDAY, LocalTime.of(8, 0)))
        assertEquals(false, isOpenNow(periods, DayOfWeek.MONDAY, LocalTime.of(18, 0)))
    }

    @Test
    fun `closed on a day with no matching period`() {
        val periods = listOf(OpeningPeriod(openDay = 1, openTime = "09:00", closeDay = 1, closeTime = "17:00"))

        assertEquals(false, isOpenNow(periods, DayOfWeek.TUESDAY, LocalTime.of(12, 0)))
    }

    @Test
    fun `a period spanning past midnight is open on both sides of the boundary`() {
        // Friday 6pm to Saturday 2am.
        val periods = listOf(OpeningPeriod(openDay = 5, openTime = "18:00", closeDay = 6, closeTime = "02:00"))

        assertEquals(true, isOpenNow(periods, DayOfWeek.FRIDAY, LocalTime.of(23, 0)))
        assertEquals(true, isOpenNow(periods, DayOfWeek.SATURDAY, LocalTime.of(1, 0)))
        assertEquals(false, isOpenNow(periods, DayOfWeek.SATURDAY, LocalTime.of(3, 0)))
        assertEquals(false, isOpenNow(periods, DayOfWeek.FRIDAY, LocalTime.of(17, 0)))
    }

    @Test
    fun `the 24-7 sentinel period is always open, any day`() {
        val periods = listOf(OpeningPeriod(openDay = 0, openTime = "00:00", closeDay = null, closeTime = null))

        DayOfWeek.entries.forEach { day ->
            assertEquals(true, isOpenNow(periods, day, LocalTime.of(3, 30)))
        }
    }
}
