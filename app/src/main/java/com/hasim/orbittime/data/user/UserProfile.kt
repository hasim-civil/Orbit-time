package com.hasim.orbittime.data.user

/** Basic profile document stored at Firestore path `users/{uid}`. */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
)
