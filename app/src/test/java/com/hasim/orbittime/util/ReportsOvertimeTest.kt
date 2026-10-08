package com.hasim.orbittime.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Reports → Overtime card. Every figure must be the one [AttendanceStats.summarize] already
 * produces (Home's Overtime cell), for whichever month Reports has selected.
 *
 * Records are the real October 2026 history (worked 7h55m, 7h54m, 8h19m, 8h22m against 8h:
 * −5, −6, +19, +22 → +30 min net) plus a September with only short days. Fixed zone and dates.
 */
class ReportsOvertimeTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val shiftStart: LocalTime = LocalTime.of(11, 0)
    private val today: LocalDate = LocalDate.of(2026, 10, 8)
    private val now: Instant = at(today, 21, 0)
    private val octStart = LocalDate.of(2026, 10, 1)
    private val octEnd = LocalDate.of(2026, 10, 31)
    private val sepStart = LocalDate.of(2026, 9, 1)
    private val sepEnd = LocalDate.of(2026, 9, 30)

    private fun at(date: LocalDate, hour: Int, minute: Int): Instant =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun punch(date: LocalDate, inH: Int, inM: Int, outH: Int?, outM: Int = 0): Pair<LocalDate, DailyAttendance> =
        date to DailyAttendance(date, at(date, inH, inM), outH?.let { at(date, it, outM) })

    private val october = mapOf(
        punch(LocalDate.of(2026, 10, 1), 8, 42, 16, 37),
        punch(LocalDate.of(2026, 10, 6), 11, 11, 19, 5),
        punch(LocalDate.of(2026, 10, 7), 10, 42, 19, 1),
        punch(LocalDate.of(2026, 10, 8), 10, 45, 19, 7),
    )
    private val september = mapOf(
        punch(LocalDate.of(2026, 9, 1), 9, 0, 16, 30), // 7h30m
        punch(LocalDate.of(2026, 9, 2), 9, 0, 17, 0), // exactly 8h: not overtime
    )
    private val records = october + september
    private val holidays = setOf(LocalDate.of(2026, 10, 2))
    private val leaves = setOf(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 5))

    private fun overtime(
        start: LocalDate,
        end: LocalDate,
        recs: Map<LocalDate, DailyAttendance> = records,
        leaveDates: Set<LocalDate> = leaves,
        holidayDates: Set<LocalDate> = holidays,
    ) = ReportsStats.overtime(recs, start, end, today, shiftStart, leaveDates, holidayDates, now, zone)

    @Test
    fun `October 2026 shows the same +30 min as Home`() {
        val result = overtime(octStart, octEnd)
        assertEquals(Duration.ofMinutes(30), result.total)
        assertEquals(2, result.overtimeDays) // Oct 7 (+19) and Oct 8 (+22)
        assertEquals(Duration.ofMinutes(15), result.averagePerOvertimeDay)

        val home = AttendanceStats.summarize(
            records, octStart, octEnd, today, "", now = now, zone = zone,
            lateAfter = shiftStart, leaveDates = leaves, holidayDates = holidays,
        )
        assertEquals(home.overtimeBalance, result.total)
        assertEquals("+30 min", AttendanceTimeFormat.overtimeLabel(result.total))
    }

    @Test
    fun `switching to a month with no overtime shows zero overtime days`() {
        val result = overtime(sepStart, sepEnd)
        assertEquals(0, result.overtimeDays)
        assertEquals(Duration.ZERO, result.averagePerOvertimeDay)
        // September is 30 min short; the balance reports that rather than hiding it.
        assertEquals(Duration.ofMinutes(-30), result.total)

        val empty = overtime(sepStart, sepEnd, recs = emptyMap())
        assertEquals(Duration.ZERO, empty.total)
        assertEquals(0, empty.overtimeDays)
        assertEquals("0 min", AttendanceTimeFormat.overtimeLabel(empty.total))
        assertEquals("0 min", AttendanceTimeFormat.overtimeLabel(empty.averagePerOvertimeDay))
    }

    @Test
    fun `leave, holiday and week off with no attendance add no overtime`() {
        // Oct 2 holiday, Oct 3 + 5 leave, Oct 4 Sunday — none has a punch.
        val withoutCalendar = overtime(octStart, octEnd, leaveDates = emptySet(), holidayDates = emptySet())
        val withCalendar = overtime(octStart, octEnd)
        assertEquals(withoutCalendar.total, withCalendar.total)
        assertEquals(withoutCalendar.overtimeDays, withCalendar.overtimeDays)

        val onlyCalendarDays = overtime(
            LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5),
        )
        assertEquals(Duration.ZERO, onlyCalendarDays.total)
        assertEquals(0, onlyCalendarDays.overtimeDays)
    }

    @Test
    fun `a past day never checked out is not an overtime day`() {
        val sep3 = LocalDate.of(2026, 9, 3)
        val incomplete = mapOf(punch(sep3, 8, 0, outH = null))
        val result = overtime(sep3, sep3, recs = incomplete)
        assertEquals(Duration.ZERO, result.total)
        assertEquals(0, result.overtimeDays)
    }

    @Test
    fun `overtime label formats`() {
        assertEquals("0 min", AttendanceTimeFormat.overtimeLabel(Duration.ZERO))
        assertEquals("+30 min", AttendanceTimeFormat.overtimeLabel(Duration.ofMinutes(30)))
        assertEquals("−45 min", AttendanceTimeFormat.overtimeLabel(Duration.ofMinutes(-45)))
        assertEquals("+1h 15m", AttendanceTimeFormat.overtimeLabel(Duration.ofMinutes(75)))
        assertEquals("+2h", AttendanceTimeFormat.overtimeLabel(Duration.ofHours(2)))
        assertEquals("0 min", AttendanceTimeFormat.overtimeLabel(Duration.ofSeconds(59)))
    }
}
