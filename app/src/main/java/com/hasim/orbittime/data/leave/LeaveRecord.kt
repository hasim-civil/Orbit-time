package com.hasim.orbittime.data.leave

import java.time.LocalDate

/** A user's own personal leave entry — no approval workflow, purely self-tracked. */
enum class LeaveType(val label: String) {
    SICK("Sick leave"),
    CASUAL("Casual leave"),
    EARNED("Earned leave"),
    UNPAID("Unpaid leave"),
    OTHER("Other"),
}

/**
 * One leave entry stored at `users/{uid}/leaves/{id}`. Dates are "yyyy-MM-dd"; a single-day
 * leave has [startDate] == [endDate]. [id] is blank for a not-yet-saved record.
 */
data class LeaveRecord(
    val id: String = "",
    val type: String = LeaveType.CASUAL.name,
    val startDate: String = "",
    val endDate: String = "",
    val note: String = "",
) {
    /** Every calendar date this leave covers, inclusive — a single day when [startDate] == [endDate]. */
    fun dateRange(): List<LocalDate> {
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return emptyList()
        val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: start
        if (end.isBefore(start)) return listOf(start)
        val dates = mutableListOf<LocalDate>()
        var date = start
        while (!date.isAfter(end)) {
            dates += date
            date = date.plusDays(1)
        }
        return dates
    }
}
