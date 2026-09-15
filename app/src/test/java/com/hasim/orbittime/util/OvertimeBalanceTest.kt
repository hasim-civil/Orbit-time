package com.hasim.orbittime.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Home → Overtime figure: a net balance of every worked day's `worked − required`, so a
 * short day cancels out a long one instead of being thrown away.
 *
 * Fixed dates and a fixed zone throughout — Mon 7 Sep 2026 to Sun 13 Sep 2026 — so nothing here
 * depends on when the suite runs.
 */
class OvertimeBalanceTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val monday: LocalDate = LocalDate.of(2026, 9, 7)
    private val tuesday: LocalDate = monday.plusDays(1)
    private val wednesday: LocalDate = monday.plusDays(2)
    private val thursday: LocalDate = monday.plusDays(3)
    private val saturday: LocalDate = monday.plusDays(5)
    private val sunday: LocalDate = monday.plusDays(6)

    /** The Monday after the test week: every date under test is in the past, so nothing is "in progress". */
    private val nextMonday: LocalDate = monday.plusDays(7)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0, second: Int = 0): Instant =
        date.atTime(LocalTime.of(hour, minute, second)).atZone(zone).toInstant()

    private fun worked(date: LocalDate, from: Instant, to: Instant?): Pair<LocalDate, DailyAttendance> =
        date to DailyAttendance(date = date, checkInAt = from, checkOutAt = to)

    private fun summarize(
        records: Map<LocalDate, DailyAttendance>,
        rangeStart: LocalDate = monday,
        rangeEnd: LocalDate = sunday,
        today: LocalDate = nextMonday,
        now: Instant = at(nextMonday, 9),
        leaveDates: Set<LocalDate> = emptySet(),
        holidayDates: Set<LocalDate> = emptySet(),
        workingHours: WorkingHours = WorkingHours.DEFAULT,
    ): AttendanceSummary = AttendanceStats.summarize(
        records = records,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        today = today,
        rangeLabel = "",
        now = now,
        zone = zone,
        leaveDates = leaveDates,
        holidayDates = holidayDates,
        workingHours = workingHours,
    )

    @Test
    fun `exact required hours balance to zero`() {
        val summary = summarize(mapOf(worked(monday, at(monday, 9), at(monday, 17))))

        assertEquals(Duration.ofHours(8), summary.worked)
        assertEquals(Duration.ZERO, summary.overtimeBalance)
    }

    @Test
    fun `a long day is positive overtime`() {
        // Required 8:00, worked 9:30 -> +1:30.
        val summary = summarize(mapOf(worked(monday, at(monday, 9), at(monday, 18, 30))))

        assertEquals(Duration.ofMinutes(90), summary.overtimeBalance)
    }

    @Test
    fun `a short day is a negative deficit`() {
        // Required 8:00, worked 7:30 -> -0:30. The old positive-only total reported 0 here.
        val summary = summarize(mapOf(worked(monday, at(monday, 9), at(monday, 16, 30))))

        assertEquals(Duration.ofMinutes(-30), summary.overtimeBalance)
    }

    @Test
    fun `positive and negative days accumulate into a net balance`() {
        // Mon +1:00, Tue -0:30, Wed +2:00 -> +2:30.
        val summary = summarize(
            mapOf(
                worked(monday, at(monday, 9), at(monday, 18)),
                worked(tuesday, at(tuesday, 9), at(tuesday, 16, 30)),
                worked(wednesday, at(wednesday, 9), at(wednesday, 19)),
            ),
        )

        assertEquals(Duration.ofHours(26).plusMinutes(30), summary.worked)
        assertEquals(Duration.ofMinutes(150), summary.overtimeBalance)
    }

    @Test
    fun `a net negative week reports a deficit`() {
        // Mon -0:45, Tue +0:15 -> -0:30 overall.
        val summary = summarize(
            mapOf(
                worked(monday, at(monday, 9), at(monday, 16, 15)),
                worked(tuesday, at(tuesday, 9), at(tuesday, 17, 15)),
            ),
        )

        assertEquals(Duration.ofMinutes(-30), summary.overtimeBalance)
    }

    @Test
    fun `minutes and seconds are kept, not rounded away`() {
        val summary = summarize(mapOf(worked(monday, at(monday, 9, 0, 30), at(monday, 17, 15, 0))))

        assertEquals(Duration.ofMinutes(14).plusSeconds(30), summary.overtimeBalance)
        // Displayed to the minute, truncated rather than rounded up.
        assertEquals("+14m", AttendanceTimeFormat.signedDurationLabel(summary.overtimeBalance))
    }

    @Test
    fun `a worked Sunday counts as overtime in full and is never a deficit`() {
        val summary = summarize(
            records = mapOf(worked(sunday, at(sunday, 10), at(sunday, 13))),
            rangeStart = sunday,
            rangeEnd = sunday,
        )

        // Nothing was required on a non-scheduled day, so all three hours are overtime...
        assertEquals(Duration.ofHours(3), summary.overtimeBalance)
        // ...the time is included in the worked total...
        assertEquals(Duration.ofHours(3), summary.worked)
        // ...and the day is still not counted as a scheduled present/absent day.
        assertEquals(0, summary.presentDays)
        assertEquals(0, summary.absentDays)
    }

    @Test
    fun `an unworked Sunday is not a deficit`() {
        val summary = summarize(records = emptyMap(), rangeStart = sunday, rangeEnd = sunday)

        assertEquals(Duration.ZERO, summary.overtimeBalance)
        assertEquals(0, summary.absentDays)
    }

    @Test
    fun `a worked holiday counts in full instead of reading as a short day`() {
        val summary = summarize(
            records = mapOf(worked(tuesday, at(tuesday, 9), at(tuesday, 12))),
            holidayDates = setOf(tuesday),
        )

        assertEquals(Duration.ofHours(3), summary.overtimeBalance)
        assertEquals(Duration.ofHours(3), summary.worked)
    }

    @Test
    fun `a worked leave day counts in full and stays reported as leave`() {
        val summary = summarize(
            records = mapOf(worked(tuesday, at(tuesday, 9), at(tuesday, 11))),
            leaveDates = setOf(tuesday),
        )

        assertEquals(Duration.ofHours(2), summary.overtimeBalance)
        assertEquals(1, summary.leaveDays)
        assertEquals(0, summary.presentDays)
    }

    @Test
    fun `an absent scheduled day is reported absent but never charged as a deficit`() {
        val summary = summarize(
            records = mapOf(worked(monday, at(monday, 9), at(monday, 17))),
            rangeEnd = tuesday,
        )

        assertEquals(1, summary.absentDays)
        assertEquals(Duration.ZERO, summary.overtimeBalance)
    }

    @Test
    fun `a past day that was never checked out contributes nothing`() {
        val summary = summarize(mapOf(worked(monday, at(monday, 9), null)))

        assertEquals(Duration.ZERO, summary.worked)
        assertEquals(Duration.ZERO, summary.overtimeBalance)
        // It is still a present day — the punch happened, only the total is unknowable.
        assertEquals(1, summary.presentDays)
    }

    @Test
    fun `a day still running is credited overtime but never debited a deficit`() {
        val inProgress = mapOf(worked(thursday, at(thursday, 9), null))

        val twoHoursIn = summarize(inProgress, today = thursday, now = at(thursday, 11))
        assertEquals(Duration.ofHours(2), twoHoursIn.worked)
        assertEquals(Duration.ZERO, twoHoursIn.overtimeBalance)

        val nineHoursIn = summarize(inProgress, today = thursday, now = at(thursday, 18))
        assertEquals(Duration.ofHours(9), nineHoursIn.worked)
        assertEquals(Duration.ofHours(1), nineHoursIn.overtimeBalance)
    }

    @Test
    fun `an overnight shift is measured at its real length`() {
        // 22:00 Saturday to 06:00 Sunday, stored against the day it started on.
        val summary = summarize(mapOf(worked(saturday, at(saturday, 22), at(sunday, 6))))

        assertEquals(Duration.ofHours(8), summary.worked)
        assertEquals(Duration.ZERO, summary.overtimeBalance)
    }

    @Test
    fun `each day is counted exactly once across a range`() {
        val week = mapOf(
            worked(monday, at(monday, 9), at(monday, 18)),
            worked(tuesday, at(tuesday, 9), at(tuesday, 18)),
            worked(wednesday, at(wednesday, 9), at(wednesday, 18)),
        )

        // Three +1:00 days, whatever the range is asked for: the whole week, or day by day.
        val whole = summarize(week)
        val perDay = listOf(monday, tuesday, wednesday)
            .map { summarize(week, rangeStart = it, rangeEnd = it).overtimeBalance }
            .fold(Duration.ZERO) { acc, d -> acc + d }

        assertEquals(Duration.ofHours(3), whole.overtimeBalance)
        assertEquals(whole.overtimeBalance, perDay)
        assertEquals(Duration.ofHours(27), whole.worked)
    }

    @Test
    fun `records outside the range are ignored`() {
        val summary = summarize(
            records = mapOf(
                worked(monday, at(monday, 9), at(monday, 18)),
                worked(wednesday, at(wednesday, 9), at(wednesday, 18)),
            ),
            rangeStart = monday,
            rangeEnd = tuesday,
        )

        assertEquals(Duration.ofHours(1), summary.overtimeBalance)
    }

    @Test
    fun `a different required duration is respected`() {
        val sixHourDay = WorkingHours(requiredPerDay = Duration.ofHours(6))
        val summary = summarize(
            records = mapOf(worked(monday, at(monday, 9), at(monday, 17))),
            workingHours = sixHourDay,
        )

        assertEquals(Duration.ofHours(2), summary.overtimeBalance)
    }

    @Test
    fun `the required time for a date follows the applicable configuration`() {
        assertEquals(Duration.ofHours(8), AttendanceStats.requiredWorkingTime(monday))
        assertEquals(Duration.ZERO, AttendanceStats.requiredWorkingTime(sunday))
        assertEquals(Duration.ZERO, AttendanceStats.requiredWorkingTime(monday, isOnLeave = true))
        assertEquals(Duration.ZERO, AttendanceStats.requiredWorkingTime(monday, isHoliday = true))
        assertEquals(
            Duration.ofHours(7),
            AttendanceStats.requiredWorkingTime(monday, WorkingHours(requiredPerDay = Duration.ofHours(7))),
        )
    }

    @Test
    fun `per-day overtime stays positive-only where a day is being labelled`() {
        assertEquals(Duration.ofMinutes(30), AttendanceStats.overtime(Duration.ofHours(8).plusMinutes(30)))
        assertEquals(Duration.ZERO, AttendanceStats.overtime(Duration.ofHours(7)))
        assertEquals(Duration.ofMinutes(-60), AttendanceStats.dailyBalance(Duration.ofHours(7), Duration.ofHours(8)))
    }

    @Test
    fun `day counts and attendance rate are unchanged by the balance rewrite`() {
        val summary = summarize(
            records = mapOf(
                worked(monday, at(monday, 9), at(monday, 17)),
                // 09:30 is after the 09:00 default shift start: present and late.
                worked(tuesday, at(tuesday, 9, 30), at(tuesday, 17)),
                worked(sunday, at(sunday, 10), at(sunday, 12)),
            ),
            leaveDates = setOf(wednesday),
        )

        assertEquals(2, summary.presentDays)
        assertEquals(1, summary.lateDays)
        assertEquals(1, summary.leaveDays)
        // Thu, Fri, Sat had no attendance and no leave.
        assertEquals(3, summary.absentDays)
        assertEquals(40, summary.attendanceRatePercent)
    }
}
