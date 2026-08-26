package com.hasim.orbittime.ui.screens.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Launches the system Credential Manager UI for "Sign in with Google" and
 * returns the Google ID token, ready to hand to Firebase's GoogleAuthProvider.
 * [serverClientId] is the Web client ID Firebase auto-creates once Google is
 * enabled as a sign-in provider (exposed as R.string.default_web_client_id).
 */
suspend fun requestGoogleIdToken(context: Context, serverClientId: String): Result<String> = runCatching {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val response = CredentialManager.create(context).getCredential(context, request)
    val credential = response.credential

    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    } else {
        error("Unexpected credential type returned by Credential Manager.")
    }
}
