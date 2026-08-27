package com.hasim.orbittime.data.holiday

/**
 * One entry in a user's personal holiday calendar, stored at `users/{uid}/holidays/{id}`.
 * [date] is "yyyy-MM-dd". [id] is blank for a not-yet-saved record.
 */
data class HolidayRecord(
    val id: String = "",
    val name: String = "",
    val date: String = "",
    val description: String = "",
)
