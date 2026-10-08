package com.hasim.orbittime.data.holiday

/**
 * One organization-wide holiday, stored at `holidays/{id}` and shared by every employee.
 * [date] is "yyyy-MM-dd". [id] is the document id — blank for a not-yet-saved record — and is
 * never written into the document itself.
 */
data class HolidayRecord(
    val id: String = "",
    val name: String = "",
    val date: String = "",
    val description: String = "",
)
