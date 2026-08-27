package com.hasim.orbittime.data.notification

import com.google.firebase.Timestamp

/** Which pastel/ink pairing a notification renders with — kept as plain categories here (no
 * Compose Color) so this data layer stays UI-framework-free, same as [com.hasim.orbittime.data.attendance.AttendanceLocation]. */
enum class NotificationKind { LATE_ARRIVAL, ATTENDANCE_INFO, SHIFT_REMINDER }

/** One entry in a user's personal notification feed, stored at
 * `users/{uid}/notifications/{id}` — one document per alert. */
data class UserNotification(
    val id: String = "",
    val kind: NotificationKind = NotificationKind.ATTENDANCE_INFO,
    val title: String = "",
    val body: String = "",
    val createdAt: Timestamp? = null,
    val read: Boolean = false,
)
