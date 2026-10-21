package com.ipb.castelobranco.core.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ipb.castelobranco.BuildConfig
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Opens the Google account picker (Credential Manager) and returns a fresh ID token for the server's client ID.
 * `null` when the person cancels, the picker fails or answers with a credential type the app does not know: callers
 * treat all of them as "no token", with no message. Shared by the login screen and the account deletion, which needs
 * a fresh sign-in of an account without a password.
 */
suspend fun requestGoogleIdToken(context: Context): String? {
    Timber.d("Starting Google Sign-In flow")
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        Timber.d("Calling credentialManager.getCredential...")
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        Timber.d("Credential received: %s - type: %s", credential::class.simpleName, credential.type)

        when {
            credential is GoogleIdTokenCredential -> credential.idToken
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                if (BuildConfig.DEBUG) Timber.d("idToken extracted: %s", idToken.take(20))
                idToken
            }
            else -> {
                Timber.w("Unrecognized credential type: %s", credential.type)
                null
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Google Sign-In failed")
        null
    }
}
