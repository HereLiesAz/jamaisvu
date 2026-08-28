package com.hereliesaz.lamplight

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

// Google's day convention is 0=Sunday..6=Saturday; kotlinx.datetime.DayOfWeek is 1=Monday..7=Sunday.
private fun googleDayToDayOfWeek(googleDay: Int): DayOfWeek = DayOfWeek(if (googleDay == 0) 7 else googleDay)

/**
 * Whether a place is open at [nowTime] on [now], given its opening periods. Returns null when
 * there's no opening-hours data at all -- distinct from "closed", since the UI shouldn't claim
 * a place is closed when it simply doesn't know.
 */
fun isOpenNow(periods: List<OpeningPeriod>, now: DayOfWeek, nowTime: LocalTime): Boolean? {
    if (periods.isEmpty()) return null

    return periods.any { period ->
        // Google's documented sentinel for "open 24 hours, every day": a single period with
        // open = Sunday 00:00 and no close at all. Not day-specific despite naming Sunday.
        val isAlwaysOpen = period.openDay == 0 && period.openTime == "00:00" &&
            period.closeDay == null && period.closeTime == null
        if (isAlwaysOpen) return@any true

        val openDay = googleDayToDayOfWeek(period.openDay)
        val openTime = runCatching { LocalTime.parse(period.openTime) }.getOrNull() ?: return@any false

        if (period.closeDay == null || period.closeTime == null) {
            // An unusual, non-sentinel open-ended period -- treat as open from that time
            // through the rest of the same day, the safest reading of an unexpected shape.
            return@any now == openDay && nowTime >= openTime
        }

        val closeDay = googleDayToDayOfWeek(period.closeDay)
        val closeTime = runCatching { LocalTime.parse(period.closeTime) }.getOrNull() ?: return@any false

        if (openDay == closeDay) {
            now == openDay && nowTime >= openTime && nowTime < closeTime
        } else {
            // Spans past midnight (e.g. open Friday 6pm, close Saturday 2am).
            (now == openDay && nowTime >= openTime) || (now == closeDay && nowTime < closeTime)
        }
    }
}
