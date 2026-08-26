package com.hasim.orbittime.data.auth

/**
 * Pure client-side validation for the auth forms. Deliberately has no Android or
 * Firebase dependency so the rules stay simple, predictable and easy to verify —
 * the messages here are shown directly to the user.
 */
object AuthValidation {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    const val MIN_PASSWORD_LENGTH = 6

    fun nameError(name: String): String? = when {
        name.isBlank() -> "Enter your name."
        else -> null
    }

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email."
        !EMAIL_REGEX.matches(email.trim()) -> "Enter a valid email address."
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.isBlank() -> "Enter your password."
        password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters."
        else -> null
    }

    fun confirmPasswordError(password: String, confirmPassword: String): String? = when {
        confirmPassword.isBlank() -> "Confirm your password."
        confirmPassword != password -> "Passwords don't match."
        else -> null
    }
}
