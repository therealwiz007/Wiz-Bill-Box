package com.wizlabs.billbox.drive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

sealed interface AuthOutcome {
    data class Token(val token: String) : AuthOutcome
    /** Google needs to show its account picker / consent screen first. */
    data class NeedsUser(val intent: PendingIntent) : AuthOutcome
}

/**
 * Google sign-in for Drive using the Google Identity Services AuthorizationClient.
 * Only the `drive.file` scope is requested: the app can see the files it creates and nothing else.
 */
class DriveAuth(private val context: Context) {

    private val client get() = Identity.getAuthorizationClient(context)

    private fun request(email: String?) = AuthorizationRequest.Builder()
        .setRequestedScopes(SCOPES)
        .apply { if (email != null) setAccount(android.accounts.Account(email, "com.google")) }
        .build()

    /** Gets a token silently if the user already agreed, else returns the intent to ask them. */
    suspend fun authorize(email: String?): AuthOutcome {
        val r = client.authorize(request(email)).await()
        return outcome(r)
    }

    fun fromIntent(data: Intent?): AuthOutcome = outcome(client.getAuthorizationResultFromIntent(data))

    private fun outcome(r: AuthorizationResult): AuthOutcome {
        val pi = r.pendingIntent
        if (r.hasResolution() && pi != null) return AuthOutcome.NeedsUser(pi)
        val token = r.accessToken ?: throw DriveAuthException("Google didn't return a Drive token")
        return AuthOutcome.Token(token)
    }

    /** Drops a cached token that Drive rejected so the next authorize() fetches a fresh one. */
    suspend fun clearToken(token: String) {
        runCatching { client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await() }
    }

    /** Signs out: revokes this app's Drive access for the account. Bills in Drive are kept. */
    suspend fun signOut(email: String?) {
        if (email == null) return
        runCatching {
            client.revokeAccess(
                RevokeAccessRequest.builder()
                    .setAccount(android.accounts.Account(email, "com.google"))
                    .setScopes(SCOPES)
                    .build()
            ).await()
        }
    }

    companion object {
        val SCOPES = listOf(Scope("https://www.googleapis.com/auth/drive.file"), Scope("email"), Scope("profile"))
    }
}
