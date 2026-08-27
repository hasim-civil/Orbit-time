package com.hasim.orbittime.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await().user
            ?: error("Google sign-in succeeded but no user was returned.")
    }.recoverCatching { throw AuthException(mapAuthError(it)) }

    fun signOut() {
        auth.signOut()
    }

    /** Keeps FirebaseAuth's own displayName in sync — used by [com.hasim.orbittime.util.UserDisplay]. */
    suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("You're not signed in.")
        user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
        Unit
    }.recoverCatching { throw AuthException(mapAuthError(it)) }

    suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("You're not signed in.")
        @Suppress("DEPRECATION")
        user.updateEmail(newEmail.trim()).await()
        Unit
    }.recoverCatching { throw AuthException(mapAuthError(it)) }

    suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("You're not signed in.")
        user.updatePassword(newPassword).await()
        Unit
    }.recoverCatching { throw AuthException(mapAuthError(it)) }

    /** True when the current user can re-verify their identity with a password (i.e. they have
     * an email/password credential on file) — a Google-only account has none. */
    val currentUserHasPasswordCredential: Boolean
        get() = auth.currentUser?.providerData.orEmpty().any { it.providerId == EmailAuthProvider.PROVIDER_ID }

    /** Refreshes the "recent login" Firebase requires before a sensitive action (e.g. deleting
     * the account) — only works for a user with a password credential. */
    suspend fun reauthenticateWithPassword(password: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("You're not signed in.")
        val email = user.email ?: error("Your account has no email on file.")
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()
        Unit
    }.recoverCatching { throw AuthException(mapAuthError(it)) }

    private fun mapAuthError(throwable: Throwable): String = when (throwable) {
        is FirebaseAuthInvalidUserException -> "No account found with that email."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthUserCollisionException -> "An account with that email already exists."
        is FirebaseAuthWeakPasswordException -> "Password is too weak — use at least ${AuthValidation.MIN_PASSWORD_LENGTH} characters."
        is FirebaseAuthRecentLoginRequiredException -> "Please sign out and sign in again before changing this."
        is FirebaseNetworkException -> "Network error — check your connection and try again."
        else -> "Something went wrong. Please try again."
    }
}

/** Carries an already-friendly, user-facing message up to the ViewModel layer. */
class AuthException(message: String) : Exception(message)
