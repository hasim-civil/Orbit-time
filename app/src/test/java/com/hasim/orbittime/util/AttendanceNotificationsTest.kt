package com.hasim.orbittime.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The attendance notification rules ([AttendanceNotifications]) against the same resolver Home,
 * Timesheet and Reports use. October 2026 is the real history:
 *
 *   Oct 1 Thu  Present 08:42–16:37        Oct 5 Mon  Leave
 *   Oct 2 Fri  Holiday                    Oct 6 Tue  Late 11:11–19:05
 *   Oct 3 Sat  Leave                      Oct 7 Wed  Present 10:42–19:01
 *   Oct 4 Sun  Week off                   Oct 8 Thu  Present 10:45–19:07 (today)
 *
 * Shift 11:00–19:00; fixed UTC zone so nothing depends on the machine running the suite.
 */
class AttendanceNotificationsTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val shiftStart: LocalTime = LocalTime.of(11, 0)
    private val shiftEnd: LocalTime = LocalTime.of(19, 0)
    private val today: LocalDate = LocalDate.of(2026, 10, 8)
    private val rangeStart: LocalDate = LocalDate.of(2026, 9, 1)

    private fun oct(day: Int): LocalDate = LocalDate.of(2026, 10, day)
    private fun at(date: LocalDate, hour: Int, minute: Int): Instant =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun punch(date: LocalDate, inH: Int, inM: Int, outH: Int, outM: Int) =
        date to DailyAttendance(date, at(date, inH, inM), at(date, outH, outM))

    private val octoberRecords = mapOf(
        punch(oct(1), 8, 42, 16, 37),
        punch(oct(6), 11, 11, 19, 5),
        punch(oct(7), 10, 42, 19, 1),
        punch(oct(8), 10, 45, 19, 7),
    )
    private val leaves = setOf(oct(3), oct(5))
    private val holidays = setOf(oct(2))

    /** September filled with on-time days, so the October cases are judged on their own. */
    private val septemberRecords = generateSequence(rangeStart) { it.plusDays(1) }
        .takeWhile { it.monthValue == 9 }
        .associateWith { DailyAttendance(it, at(it, 10, 30), at(it, 19, 0)) }

    private fun plan(
        records: Map<LocalDate, DailyAttendance> = septemberRecords + octoberRecords,
        leaveDates: Set<LocalDate> = leaves,
        holidayDates: Set<LocalDate> = holidays,
        now: Instant = at(today, 21, 0),
        existing: List<ExistingNotification> = emptyList(),
        end: LocalDate = today,
    ) = AttendanceNotifications.plan(
        rangeStart, end, today, now, records, leaveDates, holidayDates, shiftStart, shiftEnd, existing, zone,
    )

    private fun List<NotificationChange>.createdIds() = filterIsInstance<NotificationChange.Create>().map { it.id }.toSet()

    private fun statusOf(date: LocalDate, records: Map<LocalDate, DailyAttendance> = octoberRecords, now: Instant = at(today, 21, 0)) =
        AttendanceNotifications.dailyStatuses(date, date, today, now, records, leaves, holidays, shiftStart, shiftEnd, zone)[date]

    @Test
    fun `1 - leave with no attendance is Leave and never missed`() {
        assertEquals(AttendanceStatus.LEAVE, statusOf(oct(5)))
        assertEquals(AttendanceStatus.LEAVE, statusOf(oct(3)))
        assertFalse(AttendanceNotifications.missedId(oct(5)) in plan().createdIds())
        assertFalse(AttendanceNotifications.missedId(oct(3)) in plan().createdIds())
    }

    @Test
    fun `2 - holiday with no attendance is Holiday and never missed`() {
        assertEquals(AttendanceStatus.HOLIDAY, statusOf(oct(2)))
        assertFalse(AttendanceNotifications.missedId(oct(2)) in plan().createdIds())
    }

    @Test
    fun `3 - week off with no attendance is Week Off and never missed`() {
        assertEquals(AttendanceStatus.WEEKEND, statusOf(oct(4)))
        assertFalse(AttendanceNotifications.missedId(oct(4)) in plan().createdIds())
    }

    @Test
    fun `4 - on-time attendance is Present, no missed and no late`() {
        assertEquals(AttendanceStatus.PRESENT, statusOf(oct(7)))
        val ids = plan().createdIds()
        assertFalse(AttendanceNotifications.missedId(oct(7)) in ids)
        assertFalse(AttendanceNotifications.lateId(oct(7)) in ids)
    }

    @Test
    fun `5 - late attendance is Late, gets a late notice and never a missed one`() {
        assertEquals(AttendanceStatus.LATE, statusOf(oct(6)))
        val changes = plan()
        val ids = changes.createdIds()
        assertTrue(AttendanceNotifications.lateId(oct(6)) in ids)
        assertFalse(AttendanceNotifications.missedId(oct(6)) in ids)
        val late = changes.filterIsInstance<NotificationChange.Create>().single { it.id == AttendanceNotifications.lateId(oct(6)) }
        assertTrue(late.body, late.body.contains("11 minutes after shift start"))
    }

    @Test
    fun `6 - working day with no attendance after the shift ends is missed`() {
        // Drop Oct 7's record: a plain Wednesday with nothing on it.
        val records = septemberRecords + octoberRecords - oct(7)
        assertEquals(AttendanceStatus.ABSENT, statusOf(oct(7), records))
        val ids = plan(records = records).createdIds()
        assertTrue(AttendanceNotifications.missedId(oct(7)) in ids)
        assertFalse(AttendanceNotifications.lateId(oct(7)) in ids)
    }

    @Test
    fun `7 - today is not missed while the shift is in progress, and is once it ends`() {
        val records = septemberRecords + octoberRecords - oct(8)
        val during = at(today, 15, 0)
        assertNull(statusOf(today, records, during))
        assertFalse(AttendanceNotifications.missedId(today) in plan(records = records, now = during).createdIds())

        val after = at(today, 19, 1)
        assertEquals(AttendanceStatus.ABSENT, statusOf(today, records, after))
        assertTrue(AttendanceNotifications.missedId(today) in plan(records = records, now = after).createdIds())
    }

    @Test
    fun `8 - a future date is never missed`() {
        val future = oct(12) // Monday, a working day
        assertNull(statusOf(future))
        assertFalse(AttendanceStats.isWorkdayOver(future, today, at(today, 23, 59), zone, shiftStart, shiftEnd))
        assertFalse(AttendanceNotifications.missedId(future) in plan(end = oct(15)).createdIds())
    }

    @Test
    fun `9 - running the rules again creates nothing new`() {
        val first = plan()
        val created = first.filterIsInstance<NotificationChange.Create>()
        assertTrue(created.isNotEmpty())
        val existing = created.map { ExistingNotification(it.id, it.title, it.date) }
        assertEquals(emptyList<NotificationChange>(), plan(existing = existing))
    }

    @Test
    fun `10 - October matches the statuses Timesheet and Reports show`() {
        val month = DailyHistory.buildMonth(
            monthStart = oct(1), today = today,
            punches = octoberRecords.mapValues { (_, r) -> DayPunch(r.checkInAt, r.checkOutAt) },
            leaveLabels = leaves.associateWith { "Casual leave" },
            holidayNames = holidays.associateWith { "Gandhi Jayanti" },
            zone = zone, lateAfter = shiftStart, now = at(today, 21, 0), shiftEnd = shiftEnd,
        )
        val timesheet = month.history.associate { it.date to it.status }
        val notifications = AttendanceNotifications.dailyStatuses(
            oct(1), today, today, at(today, 21, 0), octoberRecords, leaves, holidays, shiftStart, shiftEnd, zone,
        )
        assertEquals(timesheet, notifications)

        val summary = AttendanceStats.summarize(
            octoberRecords, oct(1), oct(31), today, "", now = at(today, 21, 0), zone = zone,
            lateAfter = shiftStart, leaveDates = leaves, holidayDates = holidays, shiftEnd = shiftEnd,
        )
        assertEquals(0, summary.absentDays)

        // The whole October plan: exactly one late notice (Oct 6), no missed attendance at all,
        // and no monthly late count (one late day is under the threshold of two).
        val octoberCreates = plan().filterIsInstance<NotificationChange.Create>().filter { it.date?.monthValue == 10 || it.date == null }
        assertEquals(setOf(AttendanceNotifications.lateId(oct(6))), octoberCreates.map { it.id }.toSet())
    }

    @Test
    fun `stale notifications from the old rules are withdrawn, not deleted`() {
        val existing = listOf(
            ExistingNotification("missed-2026-10-05", AttendanceNotifications.MISSED_TITLE, oct(5)), // leave
            ExistingNotification("missed-2026-10-02", AttendanceNotifications.MISSED_TITLE, oct(2)), // holiday
            ExistingNotification("missed-2026-10-06", AttendanceNotifications.MISSED_TITLE, oct(6)), // late attendance
            ExistingNotification("missed-2026-10-07", AttendanceNotifications.MISSED_TITLE, oct(7)), // attended
            ExistingNotification("aB3xRandomId", AttendanceNotifications.LATE_TITLE, oct(6)), // legacy late, still true
            ExistingNotification("Zq9RandomId", AttendanceNotifications.LATE_TITLE, oct(7)), // legacy late, judged on 9:00
            ExistingNotification("late-allowance-2026-10", "Late allowance used", oct(8)),
        )
        val changes = plan(existing = existing)
        val retracted = changes.filterIsInstance<NotificationChange.Retract>().associate { it.id to it.reason }

        assertEquals(
            setOf("missed-2026-10-05", "missed-2026-10-02", "missed-2026-10-06", "missed-2026-10-07", "Zq9RandomId", "late-allowance-2026-10"),
            retracted.keys,
        )
        assertTrue(retracted.getValue("missed-2026-10-05").contains("leave"))
        assertTrue(retracted.getValue("missed-2026-10-02").contains("holiday"))
        assertTrue(retracted.getValue("late-allowance-2026-10").contains("approval"))
        // The legacy late notice for Oct 6 is still right, so no second one is created for it.
        assertFalse(AttendanceNotifications.lateId(oct(6)) in changes.createdIds())
    }

    @Test
    fun `a withdrawn missed notification comes back if the day is absent again`() {
        val records = septemberRecords + octoberRecords - oct(7)
        val existing = listOf(ExistingNotification("missed-2026-10-07", AttendanceNotifications.MISSED_TITLE, oct(7), retracted = true))
        val changes = plan(records = records, existing = existing)
        assertTrue(NotificationChange.Restore("missed-2026-10-07") in changes)
        assertFalse("missed-2026-10-07" in changes.createdIds()) // restored, not duplicated
    }

    @Test
    fun `late count reflects real late days and claims no approval`() {
        val records = septemberRecords + octoberRecords + punch(oct(7), 11, 30, 19, 30) // a second late day
        val create = plan(records = records).filterIsInstance<NotificationChange.Create>()
            .single { it.id == "late-count-2026-10" }
        assertEquals(AttendanceNotifications.LATE_COUNT_TITLE, create.title)
        assertFalse(create.body.contains("approved", ignoreCase = true))
        assertTrue(create.body.contains("2 times in October 2026"))
    }

    @Test
    fun `leave on a worked day stays leave and produces no late notice`() {
        val records = septemberRecords + octoberRecords + punch(oct(5), 11, 30, 19, 30)
        assertEquals(AttendanceStatus.LEAVE, statusOf(oct(5), records))
        assertFalse(AttendanceNotifications.lateId(oct(5)) in plan(records = records).createdIds())
    }

    @Test
    fun `an overnight shift is not over until the next morning`() {
        val nightStart = LocalTime.of(22, 0)
        val nightEnd = LocalTime.of(6, 0)
        assertFalse(AttendanceStats.isWorkdayOver(oct(7), today, at(oct(8), 5, 0), zone, nightStart, nightEnd))
        assertTrue(AttendanceStats.isWorkdayOver(oct(7), today, at(oct(8), 6, 0), zone, nightStart, nightEnd))
        assertFalse(AttendanceStats.isWorkdayOver(today, today, at(today, 23, 0), zone, nightStart, nightEnd))
    }
}
