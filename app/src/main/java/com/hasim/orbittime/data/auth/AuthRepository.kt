package com.hasim.orbittime.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

/** Thin wrapper around [FirebaseAuth], translating its exceptions into user-facing copy. */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign in succeeded but no user was returned.")
    }.recoverCatching { throw AuthException(mapAuthError(it)) }

    suspend fun createAccount(name: String, email: String, password: String): Result<FirebaseUser> = runCatching {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Account created but no user was returned.")
        user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
        user
    }.recoverCatching { throw AuthException(mapAuthError(it)) }

    fun signOut() {
        auth.signOut()
    }

    private fun mapAuthError(throwable: Throwable): String = when (throwable) {
        is FirebaseAuthInvalidUserException -> "No account found with that email."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthUserCollisionException -> "An account with that email already exists."
        is FirebaseAuthWeakPasswordException -> "Password is too weak — use at least ${AuthValidation.MIN_PASSWORD_LENGTH} characters."
        is FirebaseNetworkException -> "Network error — check your connection and try again."
        else -> "Something went wrong. Please try again."
    }
}

/** Carries an already-friendly, user-facing message up to the ViewModel layer. */
class AuthException(message: String) : Exception(message)
