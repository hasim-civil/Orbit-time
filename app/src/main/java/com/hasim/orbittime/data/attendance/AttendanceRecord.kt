package com.hasim.orbittime.data.attendance

import com.google.firebase.Timestamp

/**
 * One calendar day's attendance for a user, stored at
 * `users/{uid}/attendance/{yyyy-MM-dd}` — one document per day keeps "already
 * checked in today" a direct document read instead of a query.
 */
data class AttendanceRecord(
    val date: String = "",
    val checkInAt: Timestamp? = null,
    val checkOutAt: Timestamp? = null,
) {
    val isCheckedIn: Boolean get() = checkInAt != null && checkOutAt == null
    val isCompleted: Boolean get() = checkInAt != null && checkOutAt != null
}
