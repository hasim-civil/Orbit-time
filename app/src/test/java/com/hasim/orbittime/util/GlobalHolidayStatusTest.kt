package com.hasim.orbittime.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How an organization-wide holiday resolves against each user's own data, following
 * Leave > Holiday > Week Off > Attendance > Absent — and who the app offers holiday editing to.
 *
 * Fixed dates and zone: Fri 2 Oct 2026 (a working day), Sun 4 Oct 2026 (week off), judged on
 * Thu 8 Oct 2026 so every date is in the past.
 */
class GlobalHolidayStatusTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val shiftStart: LocalTime = LocalTime.of(10, 0)
    private val friday: LocalDate = LocalDate.of(2026, 10, 2)
    private val sunday: LocalDate = LocalDate.of(2026, 10, 4)
    private val today: LocalDate = LocalDate.of(2026, 10, 8)

    private fun at(date: LocalDate, hour: Int, minute: Int): Instant =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun classify(date: LocalDate, checkInAt: Instant? = null, isOnLeave: Boolean = false, isHoliday: Boolean = false) =
        AttendanceStats.classifyDay(checkInAt, date, today, zone, shiftStart, isOnLeave = isOnLeave, isHoliday = isHoliday)

    @Test
    fun `E - holiday with no attendance is Holiday, not Absent`() {
        assertEquals(AttendanceStatus.HOLIDAY, classify(friday, isHoliday = true))
        assertEquals(AttendanceStatus.ABSENT, classify(friday)) // the same day without the holiday
    }

    @Test
    fun `F - holiday on a week off is Holiday`() {
        assertEquals(AttendanceStatus.HOLIDAY, classify(sunday, isHoliday = true))
    }

    @Test
    fun `G - holiday with attendance is Holiday`() {
        assertEquals(AttendanceStatus.HOLIDAY, classify(friday, checkInAt = at(friday, 9, 30), isHoliday = true))
        assertEquals(AttendanceStatus.HOLIDAY, classify(friday, checkInAt = at(friday, 11, 30), isHoliday = true))
    }

    @Test
    fun `H - leave on a holiday is Leave`() {
        assertEquals(AttendanceStatus.LEAVE, classify(friday, isOnLeave = true, isHoliday = true))
        assertEquals(AttendanceStatus.LEAVE, classify(sunday, isOnLeave = true, isHoliday = true))
        assertEquals(AttendanceStatus.LEAVE, classify(friday, checkInAt = at(friday, 9, 30), isOnLeave = true, isHoliday = true))
    }

    @Test
    fun `Daily History row shows the same resolved status`() {
        val leaveOnHoliday = classify(friday, isOnLeave = true, isHoliday = true)
        assertEquals(
            DailyHistoryStatus.LEAVE,
            DailyHistory.statusOf(leaveOnHoliday, hasCheckIn = false, hasCheckOut = false, isOngoingToday = false, worked = null, locationName = null),
        )
        val workedHoliday = classify(friday, checkInAt = at(friday, 9, 30), isHoliday = true)
        assertEquals(
            DailyHistoryStatus.HOLIDAY,
            DailyHistory.statusOf(workedHoliday, hasCheckIn = true, hasCheckOut = true, isOngoingToday = false, worked = null, locationName = null),
        )
    }

    @Test
    fun `one holiday set applies identically to every employee, while leave stays personal`() {
        val holidays = setOf(friday)
        val summarize = { leaves: Set<LocalDate> ->
            AttendanceStats.summarize(
                records = emptyMap(), rangeStart = friday, rangeEnd = friday, today = today, rangeLabel = "",
                zone = zone, lateAfter = shiftStart, leaveDates = leaves, holidayDates = holidays,
            )
        }
        val withoutLeave = summarize(emptySet())
        assertEquals(0, withoutLeave.absentDays)
        assertEquals(0, withoutLeave.leaveDays)

        val onLeave = summarize(setOf(friday))
        assertEquals(0, onLeave.absentDays)
        assertEquals(1, onLeave.leaveDays)
    }

    @Test
    fun `only the verified Holiday Manager is offered holiday editing`() {
        assertTrue(HolidayAccess.canManageHolidays("hasim.radiant@gmail.com", isEmailVerified = true))
        assertTrue(HolidayAccess.canManageHolidays("Hasim.Radiant@gmail.com", isEmailVerified = true))
        assertFalse(HolidayAccess.canManageHolidays("hasim.radiant@gmail.com", isEmailVerified = false))
        assertFalse(HolidayAccess.canManageHolidays("someone.else@gmail.com", isEmailVerified = true))
        assertFalse(HolidayAccess.canManageHolidays("hasim.radiant@gmail.com.evil.com", isEmailVerified = true))
        assertFalse(HolidayAccess.canManageHolidays(null, isEmailVerified = true))
    }
}
