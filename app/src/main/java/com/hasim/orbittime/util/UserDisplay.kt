package com.hasim.orbittime.util

import com.google.firebase.auth.FirebaseUser
import java.util.Locale

/** Derives a short display name / avatar initials from whatever profile data Firebase has. */
object UserDisplay {
    fun firstName(user: FirebaseUser?): String {
        val name = user?.displayName?.trim()
        if (!name.isNullOrBlank()) return name.substringBefore(" ")
        return user?.email?.substringBefore("@") ?: ""
    }

    fun initials(user: FirebaseUser?): String {
        val name = user?.displayName?.trim()
        if (!name.isNullOrBlank()) {
            val parts = name.split(" ").filter { it.isNotBlank() }
            return parts.take(2).joinToString("") { it.first().uppercase(Locale.getDefault()) }
        }
        val email = user?.email?.trim()
        if (!email.isNullOrBlank()) return email.first().uppercase(Locale.getDefault())
        return "?"
    }
}
