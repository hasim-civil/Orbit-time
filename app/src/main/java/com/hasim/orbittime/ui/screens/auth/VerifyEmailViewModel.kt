package com.hasim.orbittime.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SECONDS = 60

/** Backs the "Verify your email" gate screen — Firebase Authentication's own built-in
 * verification flow (`sendEmailVerification()` / `reload()` / `isEmailVerified`), never a custom
 * OTP or mail server. */
class VerifyEmailViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerifyEmailUiState(email = authRepository.currentUser?.email.orEmpty()))
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    /** Reloads the Firebase user first (never trusts a possibly-stale cached flag), then checks
     * [com.google.firebase.auth.FirebaseUser.isEmailVerified]. */
    fun checkVerification(onVerified: () -> Unit) {
        if (_uiState.value.isChecking) return
        _uiState.update { it.copy(isChecking = true, infoMessage = null, errorMessage = null) }
        viewModelScope.launch {
            authRepository.reloadCurrentUser()
                .onSuccess { user ->
                    if (user.isEmailVerified) {
                        _uiState.update { it.copy(isChecking = false) }
                        onVerified()
                    } else {
                        _uiState.update {
                            it.copy(
                                isChecking = false,
                                errorMessage = "Email not verified yet. Please check your inbox and try again.",
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isChecking = false, errorMessage = error.message) }
                }
        }
    }

    fun resendVerificationEmail() {
        val state = _uiState.value
        if (state.isResending || state.cooldownSecondsRemaining > 0) return
        _uiState.update { it.copy(isResending = true, infoMessage = null, errorMessage = null) }
        viewModelScope.launch {
            authRepository.sendEmailVerification()
                .onSuccess {
                    _uiState.update { it.copy(isResending = false, infoMessage = "Verification email sent.") }
                    startCooldown()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isResending = false, errorMessage = error.message) }
                }
        }
    }

    fun signOut() {
        cooldownJob?.cancel()
        authRepository.signOut()
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in RESEND_COOLDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(cooldownSecondsRemaining = remaining) }
                delay(1000)
            }
            _uiState.update { it.copy(cooldownSecondsRemaining = 0) }
        }
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }
}
