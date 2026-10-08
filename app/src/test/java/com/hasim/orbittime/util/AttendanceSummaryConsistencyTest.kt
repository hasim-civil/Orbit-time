package com.hasim.orbittime.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The Attendance Summary, the attendance rate and every Reports figure must agree with Daily
 * History day for day, because they all come from [AttendanceStats.classifyDay].
 *
 * The month under test is the real October 2026 that exposed the bug: a holiday on Fri 2 Oct
 * was listed as "Holiday" in Daily History but counted as Absent by the summary (4 present,
 * 1 absent, 80%), because [AttendanceStats.summarize] used its own rules and ignored holidays.
 */
class AttendanceSummaryConsistencyTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val shiftStart: LocalTime = LocalTime.of(11, 0)
    private val monthStart: LocalDate = LocalDate.of(2026, 10, 1)
    private val monthEnd: LocalDate = LocalDate.of(2026, 10, 31)
    private val today: LocalDate = LocalDate.of(2026, 10, 8)
    private val now: Instant = at(today, 21, 0)

    private fun day(dayOfMonth: Int): LocalDate = monthStart.withDayOfMonth(dayOfMonth)

    private fun at(date: LocalDate, hour: Int, minute: Int): Instant =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun punch(dayOfMonth: Int, inH: Int, inM: Int, outH: Int, outM: Int): Pair<LocalDate, DailyAttendance> {
        val date = day(dayOfMonth)
        return date to DailyAttendance(date, at(date, inH, inM), at(date, outH, outM))
    }

    private val records = mapOf(
        punch(1, 8, 42, 16, 37), // Present, 7h 55m
        punch(6, 11, 11, 19, 5), // Late, 7h 54m
        punch(7, 10, 42, 19, 1), // Present, 8h 19m
        punch(8, 10, 45, 19, 7), // Present, 8h 22m
    )
    private val holidays = setOf(day(2))
    private val leaves = setOf(day(3), day(5))

    private fun summarize(
        recs: Map<LocalDate, DailyAttendance> = records,
        leaveDates: Set<LocalDate> = leaves,
        holidayDates: Set<LocalDate> = holidays,
    ) = AttendanceStats.summarize(
        records = recs, rangeStart = monthStart, rangeEnd = monthEnd, today = today, rangeLabel = "",
        now = now, zone = zone, lateAfter = shiftStart, leaveDates = leaveDates, holidayDates = holidayDates,
    )

    @Test
    fun `October 2026 summary matches Daily History`() {
        val summary = summarize()

        assertEquals(4, summary.presentDays) // 3 on time + 1 late
        assertEquals(1, summary.lateDays)
        assertEquals(0, summary.absentDays)
        assertEquals(2, summary.leaveDays)
        assertEquals(100, summary.attendanceRatePercent)
        assertEquals(Duration.ofHours(32).plusMinutes(30), summary.worked)
        assertEquals("32h 30m", AttendanceTimeFormat.elapsedLabel(summary.worked))
    }

    @Test
    fun `summary counts are exactly the Daily History statuses`() {
        val month = DailyHistory.buildMonth(
            monthStart = monthStart, today = today,
            punches = records.mapValues { (_, r) -> DayPunch(r.checkInAt, r.checkOutAt) },
            leaveLabels = leaves.associateWith { "Casual leave" },
            holidayNames = holidays.associateWith { "Gandhi Jayanti" },
            zone = zone, lateAfter = shiftStart,
        )
        val statuses = month.history.associate { it.date to it.status }
        assertEquals(AttendanceStatus.PRESENT, statuses[day(1)])
        assertEquals(AttendanceStatus.HOLIDAY, statuses[day(2)])
        assertEquals(AttendanceStatus.LEAVE, statuses[day(3)])
        assertEquals(AttendanceStatus.WEEKEND, statuses[day(4)])
        assertEquals(AttendanceStatus.LEAVE, statuses[day(5)])
        assertEquals(AttendanceStatus.LATE, statuses[day(6)])
        assertEquals(AttendanceStatus.PRESENT, statuses[day(7)])
        assertEquals(AttendanceStatus.PRESENT, statuses[day(8)])

        val counts = statuses.values.groupingBy { it }.eachCount()
        val summary = summarize()
        assertEquals((counts[AttendanceStatus.PRESENT] ?: 0) + (counts[AttendanceStatus.LATE] ?: 0), summary.presentDays)
        assertEquals(counts[AttendanceStatus.LATE] ?: 0, summary.lateDays)
        assertEquals(counts[AttendanceStatus.ABSENT] ?: 0, summary.absentDays)
        assertEquals(counts[AttendanceStatus.LEAVE] ?: 0, summary.leaveDays)
    }

    @Test
    fun `leave, holiday and week off never become absent`() {
        // Past dates with no punch at all: only the plain working day may be absent.
        val past = LocalDate.of(2026, 9, 1)
        val sundayInSeptember = LocalDate.of(2026, 9, 6)
        assertEquals(
            AttendanceStatus.HOLIDAY,
            AttendanceStats.classifyDay(null, past, today, zone, shiftStart, isHoliday = true),
        )
        assertEquals(
            AttendanceStatus.LEAVE,
            AttendanceStats.classifyDay(null, past, today, zone, shiftStart, isOnLeave = true),
        )
        assertEquals(
            AttendanceStatus.WEEKEND,
            AttendanceStats.classifyDay(null, sundayInSeptember, today, zone, shiftStart),
        )
        assertEquals(AttendanceStatus.ABSENT, AttendanceStats.classifyDay(null, past, today, zone, shiftStart))

        // Dropping the holiday makes Oct 2 an ordinary missed day — the absence the old summary
        // reported — which proves the holiday, not luck, is what keeps it out of "absent".
        val withoutHoliday = summarize(holidayDates = emptySet())
        assertEquals(1, withoutHoliday.absentDays)
        assertEquals(80, withoutHoliday.attendanceRatePercent)
        assertNotEquals(withoutHoliday.absentDays, summarize().absentDays)
    }

    @Test
    fun `Reports performance and punctuality use the same corrected figures`() {
        val performance = ReportsStats.performance(
            records = records,
            currentStart = monthStart, currentEnd = monthEnd,
            previousStart = LocalDate.of(2026, 9, 1), previousEnd = LocalDate.of(2026, 9, 30),
            today = today, lateAfter = shiftStart, leaveDates = leaves, holidayDates = holidays,
        )
        val summary = summarize()
        assertEquals(summary.presentDays, performance.presentDays)
        assertEquals(summary.absentDays, performance.absentDays)
        assertEquals(summary.lateDays, performance.lateDays)
        assertEquals(summary.leaveDays, performance.leaveDays)
        assertEquals(100, performance.attendanceRatePercent)

        val punctuality = ReportsStats.punctuality(
            records, monthStart, monthEnd, today, shiftStart, leaves, holidays, zone,
        )
        assertEquals(1, punctuality.lateDays)
        assertEquals(75, punctuality.onTimePercent) // 3 on time of 4 attendance days, 0 absent
        assertEquals(11, punctuality.averageLateMinutes)

        val workHours = ReportsStats.workHours(records, monthStart, monthEnd, today, now, zone)
        assertEquals(Duration.ofHours(32).plusMinutes(30), workHours.totalWorked)
    }
}
