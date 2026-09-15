package com.hasim.orbittime.util

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** One day's check-in/check-out, in plain types — mirrors AttendanceRecord without the Firebase Timestamp. */
data class DailyAttendance(
    val date: LocalDate,
    val checkInAt: Instant? = null,
    val checkOutAt: Instant? = null,
)

enum class AttendanceRangeMode { WEEK, MONTH }

/**
 * WEEKEND covers every non-scheduled day (the work week is Mon–Sat, so: Sunday). It used to be
 * reported as "no status at all", which left those dates with nothing to render — and so with
 * nothing to list in Daily History.
 */
enum class AttendanceStatus { PRESENT, LATE, ABSENT, LEAVE, HOLIDAY, WEEKEND }

/**
 * The working-hours configuration a day's balance is measured against.
 *
 * [requiredPerDay] is deliberately independent of the user's scheduled shift: shift start/end
 * say *when* someone is expected in (and so decide "late"), while this says how long a full
 * day's work is. A 9:00–17:30 shift is 8h30m of clock time, but the required working duration
 * is still 8 hours.
 *
 * It is a value passed into the calculations rather than a constant read from inside them, so
 * the day a required duration or work week becomes per-user (or per-day) configurable, only the
 * callers that load the configuration change — the maths below already asks per date, via
 * [AttendanceStats.requiredWorkingTime].
 */
data class WorkingHours(
    val requiredPerDay: Duration = DEFAULT_REQUIRED_PER_DAY,
    val workingDays: Set<DayOfWeek> = DEFAULT_WORKING_DAYS,
) {
    fun isWorkingDay(date: LocalDate): Boolean = date.dayOfWeek in workingDays

    companion object {
        val DEFAULT_REQUIRED_PER_DAY: Duration = Duration.ofHours(8)

        /** The scheduled work week: Mon–Sat, so Sunday is the only non-scheduled day. */
        val DEFAULT_WORKING_DAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
        )

        val DEFAULT: WorkingHours = WorkingHours()
    }
}

data class AttendanceSummary(
    val rangeLabel: String = "",
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val lateDays: Int = 0,
    val leaveDays: Int = 0,
    val worked: Duration = Duration.ZERO,
    /**
     * The *net* accumulated balance over the range: every worked day's
     * `worked − required` added up, so a short day cancels out a long one
     * (+1:00, −0:30, +2:00 → +2:30). Negative when the range is behind its required hours.
     *
     * This replaced a positive-only "overtime" total, which reported +3:00 for a range that was
     * actually 30 minutes short.
     */
    val overtimeBalance: Duration = Duration.ZERO,
    val attendanceRatePercent: Int = 0,
)

/**
 * Pure attendance-rollup math — no Firebase or Android types — so the schedule
 * rules (Mon–Sat, late after each user's own shift start, the daily balance against the
 * required working duration) stay testable outside an Android runtime, matching every other
 * calculation in this app.
 */
object AttendanceStats {

    /** Fallback only for a profile that hasn't loaded its own shift start/end yet — matches
     * [com.hasim.orbittime.data.user.UserProfile]'s own defaults. */
    val DEFAULT_LATE_AFTER: LocalTime = LocalTime.of(9, 0)
    val DEFAULT_SHIFT_END: LocalTime = LocalTime.of(17, 30)

    /**
     * One day's signed balance: `worked − required`. Positive is overtime, negative is a
     * deficit, and the sign is kept — clamping it to zero per day is exactly what made the old
     * total unable to report a shortfall.
     */
    fun dailyBalance(worked: Duration, required: Duration): Duration = worked.minus(required)

    /**
     * The positive-only part of a day's balance, from the actual worked duration — never from
     * "checked out after the shift end", which would call a late-starting full day overtime and
     * miss an early-starting long one. Used where a day is being *labelled* ("Overtime"), not
     * where balances are being added up; see [AttendanceSummary.overtimeBalance] for the latter.
     */
    fun overtime(worked: Duration, required: Duration = WorkingHours.DEFAULT.requiredPerDay): Duration =
        dailyBalance(worked, required).coerceAtLeast(Duration.ZERO)

    /**
     * How much work a specific date requires. Zero — so the day can only ever add overtime,
     * never a deficit — when nobody was expected in: a non-scheduled day (Sunday), an approved
     * leave day, or a holiday. Working such a day therefore counts in full towards the balance
     * instead of being ignored or, worse, read as a short day.
     */
    fun requiredWorkingTime(
        date: LocalDate,
        workingHours: WorkingHours = WorkingHours.DEFAULT,
        isOnLeave: Boolean = false,
        isHoliday: Boolean = false,
    ): Duration = if (workingHours.isWorkingDay(date) && !isOnLeave && !isHoliday) {
        workingHours.requiredPerDay
    } else {
        Duration.ZERO
    }

    /** A shift's real scheduled length, handling the (rare) overnight case where end wraps past
     * midnight before start. Used as the denominator for shift-completion progress bars instead
     * of a hardcoded guess, so it always reflects each user's own configured shift. */
    fun shiftDuration(shiftStart: LocalTime, shiftEnd: LocalTime): Duration =
        Duration.between(shiftStart, shiftEnd).let { if (it.isNegative || it.isZero) it.plusHours(24) else it }

    /**
     * One day's actual worked duration, or null when the day has no reliable total.
     *
     * Measured from the stored instants, so a session that runs past midnight is its real length
     * rather than a negative or wrapped one. A day still in progress (checked in today, no
     * check-out yet) is measured up to [now] — the app's own clock, so a manual App Time
     * override applies here too. A past day that was never checked out has no total at all,
     * which is what Daily History already reports as "Incomplete".
     */
    fun workedDuration(
        checkInAt: Instant?,
        checkOutAt: Instant?,
        date: LocalDate,
        today: LocalDate,
        now: Instant,
    ): Duration? {
        if (checkInAt == null) return null
        val end = checkOutAt ?: now.takeIf { date == today } ?: return null
        return Duration.between(checkInAt, end).coerceAtLeast(Duration.ZERO)
    }

    fun workedDuration(
        record: DailyAttendance?,
        date: LocalDate,
        today: LocalDate,
        now: Instant,
    ): Duration? = workedDuration(record?.checkInAt, record?.checkOutAt, date, today, now)

    /**
     * Rolls a date range up into the figures Home, Punch and Reports all show.
     *
     * Day counts (present/absent/late/leave, and so the attendance rate) follow the schedule
     * exactly as before: only scheduled Mon–Sat dates are counted, a leave day is leave rather
     * than absent, and nothing in the future is held against the user.
     *
     * Worked time and [AttendanceSummary.overtimeBalance] are deliberately *not* limited to
     * scheduled days: they come from every date that actually carries attendance, so hours
     * worked on a Sunday, a holiday or a leave day are included instead of being silently
     * dropped. Each such date is visited exactly once, and each date has at most one attendance
     * record, so no session can be counted twice.
     *
     * A day with no attendance at all contributes nothing — an absent day is reported as absent
     * and left out of the balance rather than charged as a deficit, which is the behaviour this
     * app has always had. A day still running is only ever credited (see below), never debited
     * for hours it hasn't had the chance to work yet.
     */
    fun summarize(
        records: Map<LocalDate, DailyAttendance>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        rangeLabel: String,
        now: Instant = OrbitClock.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        lateAfter: LocalTime = DEFAULT_LATE_AFTER,
        leaveDates: Set<LocalDate> = emptySet(),
        holidayDates: Set<LocalDate> = emptySet(),
        workingHours: WorkingHours = WorkingHours.DEFAULT,
    ): AttendanceSummary {
        var present = 0
        var absent = 0
        var late = 0
        var leave = 0
        var worked = Duration.ZERO
        var balance = Duration.ZERO

        var date = rangeStart
        while (!date.isAfter(rangeEnd)) {
            val record = records[date]
            val checkInAt = record?.checkInAt
            val isOnLeave = date in leaveDates
            val isHoliday = date in holidayDates

            if (workingHours.isWorkingDay(date)) {
                if (isOnLeave) {
                    leave += 1
                } else {
                    when {
                        checkInAt != null -> {
                            present += 1
                            if (checkInAt.atZone(zone).toLocalTime().isAfter(lateAfter)) late += 1
                        }
                        date.isBefore(today) -> absent += 1
                    }
                }
            }

            val dayWorked = workedDuration(record, date, today, now)
            if (dayWorked != null) {
                worked += dayWorked
                val required = requiredWorkingTime(date, workingHours, isOnLeave = isOnLeave, isHoliday = isHoliday)
                val dayBalance = dailyBalance(dayWorked, required)
                // A session still running can only be ahead, never behind: at 9:05 on an 8-hour
                // day the user is not 7h55m in deficit, they simply haven't finished yet. Once
                // they check out, the day's real (possibly negative) balance counts in full.
                val isStillRunning = record?.checkOutAt == null
                balance += if (isStillRunning) dayBalance.coerceAtLeast(Duration.ZERO) else dayBalance
            }

            date = date.plusDays(1)
        }

        val counted = present + absent
        val rate = if (counted > 0) (present * 100) / counted else 0

        return AttendanceSummary(
            rangeLabel = rangeLabel,
            presentDays = present,
            absentDays = absent,
            lateDays = late,
            leaveDays = leave,
            worked = worked,
            overtimeBalance = balance,
            attendanceRatePercent = rate,
        )
    }

    /**
     * Per-day status for calendar/history views. Returns null only for a day that genuinely has
     * nothing to say yet: today, or a future scheduled day, with no check-in.
     *
     * The order below is the rule, and it is deliberate:
     *  - a holiday or an approved leave outranks both the punch record and "absent", so adding
     *    either for a past date re-labels that date on its own — no attendance record needed.
     *    The hours worked on such a day are not discarded; [DailyHistory] still shows them, and
     *    [summarize] still counts them;
     *  - a non-scheduled day is never "absent" — nobody was expected in — and is never "late"
     *    either, since the shift start it would be measured against doesn't apply that day;
     *  - a check-in then decides on-time vs late;
     *  - only then does a past scheduled day with nothing at all on it count as absent.
     */
    fun classifyDay(
        checkInAt: Instant?,
        date: LocalDate,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        lateAfter: LocalTime = DEFAULT_LATE_AFTER,
        isOnLeave: Boolean = false,
        isHoliday: Boolean = false,
        workingHours: WorkingHours = WorkingHours.DEFAULT,
    ): AttendanceStatus? = when {
        isHoliday -> AttendanceStatus.HOLIDAY
        isOnLeave -> AttendanceStatus.LEAVE
        !workingHours.isWorkingDay(date) -> AttendanceStatus.WEEKEND
        checkInAt != null ->
            if (checkInAt.atZone(zone).toLocalTime().isAfter(lateAfter)) AttendanceStatus.LATE else AttendanceStatus.PRESENT
        date.isBefore(today) -> AttendanceStatus.ABSENT
        else -> null
    }
}
