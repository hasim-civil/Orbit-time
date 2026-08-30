package com.hasim.orbittime.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ReportsPerformance(
    val attendanceRatePercent: Int = 0,
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val lateDays: Int = 0,
    val leaveDays: Int = 0,
    /** Null when the previous period has no scheduled days yet to compare against. */
    val vsPreviousPeriodPercent: Int? = null,
)

data class ReportsWorkHours(
    val totalWorked: Duration = Duration.ZERO,
    val averagePerDay: Duration = Duration.ZERO,
    val averageCheckIn: LocalTime? = null,
    val averageCheckOut: LocalTime? = null,
)

data class ReportsPunctuality(
    val onTimePercent: Int = 0,
    val lateDays: Int = 0,
    val averageLateMinutes: Int = 0,
)

data class ReportsTrendPoint(val label: String, val attendanceRatePercent: Int)

/**
 * Pure Reports math, built entirely on top of [AttendanceStats] and the same
 * [DailyAttendance] records every other screen already fetches from
 * [com.hasim.orbittime.data.attendance.AttendanceRepository] — no new Firestore reads, no new
 * attendance model, no new late/absence rules. Every figure here is derived in memory from
 * whatever range of records the caller already has loaded.
 */
object ReportsStats {

    /** [currentStart]/[currentEnd] and [previousStart]/[previousEnd] both use
     * [AttendanceStats.summarize] — the exact same rollup Home and Timesheet already use — so the
     * attendance rate and day counts here always agree with the rest of the app. */
    fun performance(
        records: Map<LocalDate, DailyAttendance>,
        currentStart: LocalDate,
        currentEnd: LocalDate,
        previousStart: LocalDate,
        previousEnd: LocalDate,
        today: LocalDate,
        lateAfter: LocalTime,
        leaveDates: Set<LocalDate>,
    ): ReportsPerformance {
        val current = AttendanceStats.summarize(
            records = records, rangeStart = currentStart, rangeEnd = currentEnd,
            today = today, rangeLabel = "", lateAfter = lateAfter, leaveDates = leaveDates,
        )
        val previous = AttendanceStats.summarize(
            records = records, rangeStart = previousStart, rangeEnd = previousEnd,
            today = today, rangeLabel = "", lateAfter = lateAfter, leaveDates = leaveDates,
        )
        val previousCounted = previous.presentDays + previous.absentDays
        val delta = if (previousCounted > 0) current.attendanceRatePercent - previous.attendanceRatePercent else null

        return ReportsPerformance(
            attendanceRatePercent = current.attendanceRatePercent,
            presentDays = current.presentDays,
            absentDays = current.absentDays,
            lateDays = current.lateDays,
            leaveDays = current.leaveDays,
            vsPreviousPeriodPercent = delta,
        )
    }

    /** Totals and averages come straight from each day's own check-in/check-out instants — the
     * same fields [AttendanceStats.summarize] itself reads — never from a separately stored total. */
    fun workHours(
        records: Map<LocalDate, DailyAttendance>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ReportsWorkHours {
        var totalWorked = Duration.ZERO
        var daysWithHours = 0
        val checkInSeconds = mutableListOf<Long>()
        val checkOutSeconds = mutableListOf<Long>()

        var date = rangeStart
        val effectiveEnd = if (rangeEnd.isAfter(today)) today else rangeEnd
        while (!date.isAfter(effectiveEnd)) {
            val record = records[date]
            val checkInAt = record?.checkInAt
            if (checkInAt != null) {
                checkInSeconds += checkInAt.atZone(zone).toLocalTime().toSecondOfDay().toLong()
                val end = record.checkOutAt ?: if (date == today) now else null
                if (end != null) {
                    val duration = Duration.between(checkInAt, end).let { if (it.isNegative) Duration.ZERO else it }
                    totalWorked += duration
                    daysWithHours += 1
                }
                record.checkOutAt?.let { checkOutSeconds += it.atZone(zone).toLocalTime().toSecondOfDay().toLong() }
            }
            date = date.plusDays(1)
        }

        return ReportsWorkHours(
            totalWorked = totalWorked,
            averagePerDay = if (daysWithHours > 0) totalWorked.dividedBy(daysWithHours.toLong()) else Duration.ZERO,
            averageCheckIn = checkInSeconds.averageSecondOfDay(),
            averageCheckOut = checkOutSeconds.averageSecondOfDay(),
        )
    }

    /** Walks the same [AttendanceStats.classifyDay] every calendar/history view already uses, so
     * "late" here means exactly what it means everywhere else in the app — no separate rule. */
    fun punctuality(
        records: Map<LocalDate, DailyAttendance>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        today: LocalDate,
        lateAfter: LocalTime,
        leaveDates: Set<LocalDate>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ReportsPunctuality {
        var present = 0
        var absent = 0
        var late = 0
        val lateMinutes = mutableListOf<Long>()

        var date = rangeStart
        val effectiveEnd = if (rangeEnd.isAfter(today)) today else rangeEnd
        while (!date.isAfter(effectiveEnd)) {
            val checkInAt = records[date]?.checkInAt
            when (AttendanceStats.classifyDay(checkInAt, date, today, zone, lateAfter, date in leaveDates)) {
                AttendanceStatus.PRESENT -> present += 1
                AttendanceStatus.LATE -> {
                    present += 1
                    late += 1
                    checkInAt?.let {
                        val minutesLate = Duration.between(lateAfter, it.atZone(zone).toLocalTime()).toMinutes()
                        if (minutesLate > 0) lateMinutes += minutesLate
                    }
                }
                AttendanceStatus.ABSENT -> absent += 1
                else -> Unit
            }
            date = date.plusDays(1)
        }

        val counted = present + absent
        val onTimePercent = if (counted > 0) (((present - late) * 100) / counted).coerceIn(0, 100) else 0
        val averageLateMinutes = if (lateMinutes.isNotEmpty()) (lateMinutes.sum() / lateMinutes.size).toInt() else 0

        return ReportsPunctuality(onTimePercent = onTimePercent, lateDays = late, averageLateMinutes = averageLateMinutes)
    }

    private fun List<Long>.averageSecondOfDay(): LocalTime? =
        if (isEmpty()) null else LocalTime.ofSecondOfDay(sum() / size)
}
