package com.hasim.orbittime.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The kind of alert, by the same names as [com.hasim.orbittime.data.notification.NotificationKind]
 * (kept as a plain string there so this rules layer stays free of the data layer). */
enum class PlannedNotificationKind { LATE_ARRIVAL, ATTENDANCE_INFO }

/** One notification as it already exists in the user's feed — only what the rules need. */
data class ExistingNotification(
    val id: String,
    val title: String,
    /** The date the notification was created on, in the user's zone. Legacy late-arrival
     * notifications have random ids, so this is how they're tied back to a day. */
    val createdOn: LocalDate?,
    val retracted: Boolean = false,
)

sealed class NotificationChange {
    abstract val id: String

    data class Create(
        override val id: String,
        val kind: PlannedNotificationKind,
        val title: String,
        val body: String,
        val date: LocalDate?,
    ) : NotificationChange()

    /** An existing notification the data no longer supports. It's kept, marked withdrawn with
     * [reason], rather than deleted, so the feed still shows what was once said and why it's gone. */
    data class Retract(override val id: String, val reason: String) : NotificationChange()

    /** A previously withdrawn notification whose fact is true again (e.g. a leave was removed). */
    data class Restore(override val id: String) : NotificationChange()
}

/**
 * The attendance notification rules — decided from the very same day status Home, Timesheet and
 * Reports show ([AttendanceStats.classifyDay]), never from a separate "is there a check-in
 * document?" test. So a day Reports calls Leave can't be reported here as missed.
 *
 *  - **Missed attendance**: only a day that resolves to [AttendanceStatus.ABSENT] — a scheduled
 *    working day, with no leave, no holiday and no check-in, whose workday is over. Today counts
 *    only once its shift has ended; a future date never does.
 *  - **Late arrival logged**: only a day that resolves to [AttendanceStatus.LATE] — a check-in
 *    after the shift start on a day that is not leave, holiday or week off. A day is either
 *    Absent or Late, never both, so it can never get both notifications.
 *  - **Late check-ins this month**: once a month reaches [LATE_CHECK_IN_REMINDER_THRESHOLD] Late
 *    days. This is a heads-up count only — Orbit Time has no approval or allowance records, so
 *    the wording claims nothing about approval.
 *
 * Every notification has a deterministic id (type + date or month), so re-running the rules any
 * number of times creates each one at most once. Existing notifications the data now contradicts
 * — including ones written by the old, race-prone rules — are withdrawn, not deleted.
 */
object AttendanceNotifications {

    /** A reminder threshold, not an approval: no approval/allowance data exists in the app. */
    const val LATE_CHECK_IN_REMINDER_THRESHOLD = 2

    const val MISSED_TITLE = "Missed attendance"
    const val LATE_TITLE = "Late arrival logged"
    const val LATE_COUNT_TITLE = "Late check-ins this month"

    private const val MISSED_PREFIX = "missed-"
    private const val LATE_PREFIX = "late-"
    private const val LATE_COUNT_PREFIX = "late-count-"

    /** The old monthly notification, which called these late arrivals "approved". */
    private const val LEGACY_LATE_ALLOWANCE_PREFIX = "late-allowance-"

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val shortDayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun missedId(date: LocalDate) = "$MISSED_PREFIX$date"
    fun lateId(date: LocalDate) = "$LATE_PREFIX$date"
    fun lateCountId(month: YearMonth) = "$LATE_COUNT_PREFIX$month"

    /**
     * Everything that should change in the feed for [rangeStart]..[rangeEnd], from data that
     * must be complete for that range (attendance, the user's leave, the organization's holidays
     * and the user's own shift). Dates outside the range are left untouched.
     */
    fun plan(
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        now: Instant,
        records: Map<LocalDate, DailyAttendance>,
        leaveDates: Set<LocalDate>,
        holidayDates: Set<LocalDate>,
        shiftStart: LocalTime,
        shiftEnd: LocalTime?,
        existing: List<ExistingNotification>,
        zone: ZoneId = ZoneId.systemDefault(),
        workingHours: WorkingHours = WorkingHours.DEFAULT,
    ): List<NotificationChange> {
        val statuses = dailyStatuses(rangeStart, rangeEnd, today, now, records, leaveDates, holidayDates, shiftStart, shiftEnd, zone, workingHours)
        val byId = existing.associateBy { it.id }
        val changes = mutableListOf<NotificationChange>()

        fun want(create: NotificationChange.Create) {
            val current = byId[create.id]
            when {
                current == null -> changes += create
                current.retracted -> changes += NotificationChange.Restore(create.id)
            }
        }

        // What the data says should exist.
        val lateDatesCovered = existing
            .filter { !it.retracted && it.title == LATE_TITLE && it.id.isLegacyRandomId() }
            .mapNotNull { it.createdOn }
            .toSet()
        statuses.forEach { (date, status) ->
            when (status) {
                AttendanceStatus.ABSENT -> want(
                    NotificationChange.Create(
                        id = missedId(date),
                        kind = PlannedNotificationKind.LATE_ARRIVAL,
                        title = MISSED_TITLE,
                        body = "You didn't check in on ${shortDayFormatter.format(date)}.",
                        date = date,
                    ),
                )
                AttendanceStatus.LATE -> {
                    // A legacy late notification (random id) for this day already says it.
                    if (date !in lateDatesCovered) {
                        val checkIn = records.getValue(date).checkInAt!!.atZone(zone)
                        val minutes = Duration.between(shiftStart, checkIn.toLocalTime()).toMinutes()
                        want(
                            NotificationChange.Create(
                                id = lateId(date),
                                kind = PlannedNotificationKind.LATE_ARRIVAL,
                                title = LATE_TITLE,
                                body = "${shortDayFormatter.format(date)} — clocked in at " +
                                    "${clockFormatter.format(checkIn).lowercase(Locale.getDefault())}, " +
                                    "$minutes minute${if (minutes == 1L) "" else "s"} after shift start.",
                                date = date,
                            ),
                        )
                    }
                }
                else -> Unit
            }
        }
        val lateDaysByMonth = statuses.filterValues { it == AttendanceStatus.LATE }.keys.groupBy { YearMonth.from(it) }
        val monthsJudged = monthsFullyInside(rangeStart, rangeEnd, today)
        monthsJudged.forEach { month ->
            if ((lateDaysByMonth[month]?.size ?: 0) >= LATE_CHECK_IN_REMINDER_THRESHOLD) {
                want(
                    NotificationChange.Create(
                        id = lateCountId(month),
                        kind = PlannedNotificationKind.ATTENDANCE_INFO,
                        title = LATE_COUNT_TITLE,
                        body = "You've checked in late $LATE_CHECK_IN_REMINDER_THRESHOLD times in ${monthFormatter.format(month)}.",
                        date = null,
                    ),
                )
            }
        }

        // What the data no longer supports.
        existing.filter { !it.retracted }.forEach { notification ->
            val id = notification.id
            val reason: String? = when {
                id.startsWith(MISSED_PREFIX) -> id.dateAfter(MISSED_PREFIX)
                    ?.takeIf { it.isInside(rangeStart, rangeEnd) }
                    ?.let { date -> withdrawnUnless(date, statuses[date], AttendanceStatus.ABSENT) }
                id.startsWith(LATE_COUNT_PREFIX) -> runCatching { YearMonth.parse(id.removePrefix(LATE_COUNT_PREFIX)) }.getOrNull()
                    ?.takeIf { it in monthsJudged }
                    ?.takeIf { (lateDaysByMonth[it]?.size ?: 0) < LATE_CHECK_IN_REMINDER_THRESHOLD }
                    ?.let { "Fewer than $LATE_CHECK_IN_REMINDER_THRESHOLD late check-ins in ${monthFormatter.format(it)}." }
                id.startsWith(LEGACY_LATE_ALLOWANCE_PREFIX) ->
                    "Replaced: this described late arrivals as approved, but Orbit Time keeps no approval records."
                id.startsWith(LATE_PREFIX) -> id.dateAfter(LATE_PREFIX)
                    ?.takeIf { it.isInside(rangeStart, rangeEnd) }
                    ?.let { date -> withdrawnUnless(date, statuses[date], AttendanceStatus.LATE) }
                notification.title == LATE_TITLE && id.isLegacyRandomId() -> notification.createdOn
                    ?.takeIf { it.isInside(rangeStart, rangeEnd) }
                    ?.let { date -> withdrawnUnless(date, statuses[date], AttendanceStatus.LATE) }
                else -> null
            }
            if (reason != null) changes += NotificationChange.Retract(id, reason)
        }
        return changes
    }

    /** The resolved status of every date in the range — the same verdict Home, Timesheet and
     * Reports render. Null means nothing to say yet (today before its shift ends, or future). */
    fun dailyStatuses(
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        now: Instant,
        records: Map<LocalDate, DailyAttendance>,
        leaveDates: Set<LocalDate>,
        holidayDates: Set<LocalDate>,
        shiftStart: LocalTime,
        shiftEnd: LocalTime?,
        zone: ZoneId = ZoneId.systemDefault(),
        workingHours: WorkingHours = WorkingHours.DEFAULT,
    ): Map<LocalDate, AttendanceStatus?> =
        generateSequence(rangeStart) { it.plusDays(1) }
            .takeWhile { !it.isAfter(rangeEnd) }
            .associateWith { date ->
                AttendanceStats.classifyDay(
                    checkInAt = records[date]?.checkInAt,
                    date = date,
                    today = today,
                    zone = zone,
                    lateAfter = shiftStart,
                    isOnLeave = date in leaveDates,
                    isHoliday = date in holidayDates,
                    workingHours = workingHours,
                    isWorkdayOver = AttendanceStats.isWorkdayOver(date, today, now, zone, shiftStart, shiftEnd),
                )
            }

    private fun withdrawnUnless(date: LocalDate, status: AttendanceStatus?, expected: AttendanceStatus): String? {
        if (status == expected) return null
        val day = shortDayFormatter.format(date)
        return when (status) {
            AttendanceStatus.LEAVE -> "$day was a leave day."
            AttendanceStatus.HOLIDAY -> "$day was a holiday."
            AttendanceStatus.WEEKEND -> "$day was a week off."
            AttendanceStatus.PRESENT -> "Attendance is recorded for $day, on time."
            AttendanceStatus.LATE -> "Attendance is recorded for $day."
            AttendanceStatus.ABSENT -> "No check-in is recorded for $day."
            null -> "$day's workday isn't over yet."
        }
    }

    /** Months whose late count can be judged: every month that starts inside the range and is
     * covered by it up to its end or up to today, whichever comes first. */
    private fun monthsFullyInside(rangeStart: LocalDate, rangeEnd: LocalDate, today: LocalDate): Set<YearMonth> =
        generateSequence(YearMonth.from(rangeStart)) { it.plusMonths(1) }
            .takeWhile { !it.atDay(1).isAfter(rangeEnd) }
            .filter { month ->
                val lastNeeded = minOf(month.atEndOfMonth(), today)
                !month.atDay(1).isBefore(rangeStart) && !lastNeeded.isAfter(rangeEnd)
            }
            .toSet()

    private fun String.dateAfter(prefix: String): LocalDate? =
        runCatching { LocalDate.parse(removePrefix(prefix)) }.getOrNull()

    /** Every id these rules write is "<type>-<date or month>"; anything else came from the old
     * check-in-time late log, which used Firestore's random ids. */
    private fun String.isLegacyRandomId(): Boolean =
        !startsWith(MISSED_PREFIX) && !startsWith(LATE_PREFIX) && !startsWith(LEGACY_LATE_ALLOWANCE_PREFIX)

    private fun LocalDate.isInside(start: LocalDate, end: LocalDate) = !isBefore(start) && !isAfter(end)
}
