package com.hasim.orbittime.data.user

/** Basic profile document stored at Firestore path `users/{uid}`. Shift times are "HH:mm", 24-hour. */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val photoUrl: String = "",
    val shiftStart: String = "09:00",
    val shiftEnd: String = "17:30",
)
