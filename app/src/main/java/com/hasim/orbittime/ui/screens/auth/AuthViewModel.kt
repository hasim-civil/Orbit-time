package com.hasim.orbittime.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasim.orbittime.data.auth.AuthRepository
import com.hasim.orbittime.data.user.UserProfile
import com.hasim.orbittime.data.user.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs both the Sign In and Create Account screens: they share the same
 * submit/loading/error shape and the same two repositories, so one ViewModel
 * covers both instead of duplicating near-identical state machinery.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val profileRepository: UserProfileRepository = UserProfileRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onSuccess {
                    _uiState.value = AuthUiState(isLoading = false)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(isLoading = false, errorMessage = error.message)
                }
        }
    }

    fun createAccount(name: String, email: String, password: String, onSuccess: () -> Unit) {
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            authRepository.createAccount(name, email, password)
                .onSuccess { user ->
                    val profile = UserProfile(uid = user.uid, name = name.trim(), email = email.trim())
                    runCatching { profileRepository.saveProfile(profile) }
                    _uiState.value = AuthUiState(isLoading = false)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(isLoading = false, errorMessage = error.message)
                }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            authRepository.signInWithGoogleIdToken(idToken)
                .onSuccess { user ->
                    val profile = UserProfile(
                        uid = user.uid,
                        name = user.displayName.orEmpty(),
                        email = user.email.orEmpty(),
                    )
                    runCatching { profileRepository.saveProfile(profile) }
                    _uiState.value = AuthUiState(isLoading = false)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(isLoading = false, errorMessage = error.message)
                }
        }
    }

    /** Surfaces a failure from the Google sign-in picker itself (before Firebase is even involved). */
    fun reportError(message: String) {
        _uiState.value = AuthUiState(isLoading = false, errorMessage = message)
    }
}
