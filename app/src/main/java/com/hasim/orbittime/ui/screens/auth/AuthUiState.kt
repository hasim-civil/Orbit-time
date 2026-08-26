package com.hasim.orbittime.ui.screens.auth

/** Shared submit-state for the Sign In and Create Account screens. */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
