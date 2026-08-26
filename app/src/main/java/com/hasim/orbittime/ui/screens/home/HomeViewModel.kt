package com.hasim.orbittime.ui.screens.home

import androidx.lifecycle.ViewModel
import com.hasim.orbittime.data.auth.AuthRepository

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
) : ViewModel() {
    val displayName: String? get() = authRepository.currentUser?.displayName
    val email: String? get() = authRepository.currentUser?.email

    fun signOut() = authRepository.signOut()
}
