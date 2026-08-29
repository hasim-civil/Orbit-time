package com.hasim.orbittime.ui.screens.auth

/** State for the "Verify your email" gate screen. */
data class VerifyEmailUiState(
    val email: String = "",
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val cooldownSecondsRemaining: Int = 0,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)
