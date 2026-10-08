package com.hasim.orbittime.data.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/** Which pastel/ink pairing a notification renders with — kept as plain categories here (no
 * Compose Color) so this data layer stays UI-framework-free, same as [com.hasim.orbittime.data.attendance.AttendanceLocation]. */
enum class NotificationKind { LATE_ARRIVAL, ATTENDANCE_INFO, SHIFT_REMINDER }

/** Notifications written by the current attendance rules carry this; anything lower was written
 * by an earlier version of the rules and is shown as such. */
const val CURRENT_NOTIFICATION_RULES_VERSION = 2

/** One entry in a user's personal notification feed, stored at
 * `users/{uid}/notifications/{id}` — one document per alert. */
data class UserNotification(
    val id: String = "",
    val kind: NotificationKind = NotificationKind.ATTENDANCE_INFO,
    val title: String = "",
    val body: String = "",
    val createdAt: Timestamp? = null,
    val read: Boolean = false,
    /** The day this notification is about ("yyyy-MM-dd"), blank when it isn't about one day. */
    val date: String = "",
    /** 0 for notifications created before the current rules ([CURRENT_NOTIFICATION_RULES_VERSION]). */
    val rulesVersion: Int = 0,
    /** Withdrawn because the data no longer supports it (e.g. that day turned out to be leave).
     * Kept in the feed, with [retractedReason], rather than deleted. */
    val retracted: Boolean = false,
    val retractedReason: String = "",
    /** Hidden by "Clear all". The document stays as a marker so the same fact is never created
     * again the next time the rules run. */
    val dismissed: Boolean = false,
) {
    /** Derived, never stored (Firestore would otherwise write it out as a "historical" field). */
    @get:Exclude
    val isHistorical: Boolean get() = rulesVersion < CURRENT_NOTIFICATION_RULES_VERSION
}
