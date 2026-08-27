package com.hasim.orbittime.data.user

/**
 * Basic profile document stored at Firestore path `users/{uid}`. Shift times are "HH:mm", 24-hour.
 * [photoBase64] holds a downsized JPEG encoded inline (no Firebase Storage — that requires the
 * paid Blaze plan) rather than a download URL, so it stays well under Firestore's 1MB document cap.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val company: String = "",
    val photoBase64: String = "",
    val shiftStart: String = "09:00",
    val shiftEnd: String = "17:30",
)
