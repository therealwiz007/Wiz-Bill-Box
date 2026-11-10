package com.wizlabs.billbox

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.wizlabs.billbox.data.BillRepository
import com.wizlabs.billbox.data.Photos
import com.wizlabs.billbox.data.Store
import com.wizlabs.billbox.drive.AuthOutcome
import com.wizlabs.billbox.drive.DriveApi
import com.wizlabs.billbox.drive.DriveAuth
import com.wizlabs.billbox.drive.DriveAuthException
import com.wizlabs.billbox.model.Account
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.UnknownHostException

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data class Done(val at: Long, val restored: Int) : SyncState
    data class Failed(val message: String) : SyncState
    /** Google wants the user to pick the account / agree again. */
    data object NeedsSignIn : SyncState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val photos = Photos(app)
    private val repo = BillRepository(Store(File(app.filesDir, "bills.json")), photos)
    private val auth = DriveAuth(app)

    val data: StateFlow<AppData> = repo.data

    private val _sync = MutableStateFlow<SyncState>(SyncState.Idle)
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    /** Google's sign-in screen, which the activity has to launch. */
    private val _consent = Channel<PendingIntent>(Channel.BUFFERED)
    val consent = _consent.receiveAsFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var syncJob: Job? = null

    init {
        if (data.value.account != null) requestSync()
    }

    // ---- Sign-in ----

    fun signIn() {
        if (_signingIn.value) return
        _signingIn.value = true
        viewModelScope.launch {
            try {
                when (val r = auth.authorize(data.value.account?.email)) {
                    is AuthOutcome.NeedsUser -> _consent.send(r.intent)
                    is AuthOutcome.Token -> onToken(r.token)
                }
            } catch (e: Exception) {
                _signingIn.value = false
                _messages.send(explain(e))
            }
        }
    }

    fun onConsentResult(resultOk: Boolean, data: Intent?) {
        viewModelScope.launch {
            try {
                if (!resultOk && data == null) { _signingIn.value = false; return@launch }
                when (val r = auth.fromIntent(data)) {
                    is AuthOutcome.Token -> onToken(r.token)
                    is AuthOutcome.NeedsUser -> { _signingIn.value = false; _messages.send("Sign-in wasn't completed") }
                }
            } catch (e: Exception) {
                _signingIn.value = false
                _messages.send(explain(e))
            }
        }
    }

    private suspend fun onToken(token: String) {
        try {
            val previous = data.value.account?.email
            val (email, name) = withContext(Dispatchers.IO) { DriveApi(token).about() }
            if (previous != null && !previous.equals(email, ignoreCase = true)) {
                // A different Google account: its Drive has its own bills.
                repo.clearLocal()
            }
            repo.setAccount(Account(email, name))
            _signingIn.value = false
            runSync(token, announce = true)
        } catch (e: Exception) {
            _signingIn.value = false
            _messages.send(explain(e))
        }
    }

    fun skipSignIn() = repo.skipSignIn()

    fun signOut() {
        viewModelScope.launch {
            syncJob?.cancel()
            auth.signOut(data.value.account?.email)
            repo.clearLocal()
            _sync.value = SyncState.Idle
            _messages.send("Signed out. Your bills are still in Google Drive.")
        }
    }

    // ---- Sync ----

    /** Uploads pending changes and pulls bills saved elsewhere. Silent: never shows Google's UI. */
    fun requestSync(delayMs: Long = 0) {
        if (data.value.account == null) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            val token = silentToken() ?: return@launch
            runSync(token, announce = false)
        }
    }

    private suspend fun silentToken(): String? = try {
        when (val r = auth.authorize(data.value.account?.email)) {
            is AuthOutcome.Token -> r.token
            is AuthOutcome.NeedsUser -> { _sync.value = SyncState.NeedsSignIn; null }
        }
    } catch (e: Exception) {
        _sync.value = SyncState.Failed(explain(e)); null
    }

    private suspend fun runSync(token: String, announce: Boolean) {
        _sync.value = SyncState.Running
        try {
            val r = try {
                repo.sync(token)
            } catch (e: DriveAuthException) {
                auth.clearToken(token)
                val fresh = silentToken() ?: return
                repo.sync(fresh)
            }
            _sync.value = SyncState.Done(System.currentTimeMillis(), r.restored)
            if (announce && r.restored > 0) _messages.send("Restored ${r.restored} bill${if (r.restored == 1) "" else "s"} from Google Drive")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _sync.value = SyncState.Failed(explain(e))
        }
    }

    /** Makes sure a photo saved from another phone is downloaded. */
    fun ensurePhoto(billId: String, photoId: String, done: (String?) -> Unit) {
        viewModelScope.launch {
            val token = silentToken()
            val name = if (token == null) null else runCatching { repo.fetchPhoto(token, billId, photoId) }.getOrNull()
            done(name)
        }
    }

    // ---- Bills ----

    fun saveBill(b: Bill) {
        repo.saveBill(b)
        requestSync(delayMs = 800)
    }

    fun deleteBill(id: String) {
        repo.deleteBill(id)
        requestSync(delayMs = 800)
    }

    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching { photos.import(uri) }.getOrNull()
    }

    fun setTheme(t: ThemeMode) = repo.setTheme(t)
    fun setReminderDays(d: Int) = repo.setReminderDays(d)
    fun toast(msg: String) { viewModelScope.launch { _messages.send(msg) } }

    private fun explain(e: Exception): String = when {
        e is ApiException && e.statusCode == 10 ->
            "Google sign-in isn't set up for this build yet (OAuth client / SHA-1 missing). See the README."
        e is ApiException && (e.statusCode == 16 || e.statusCode == 12501) -> "Sign-in cancelled"
        e is ApiException && e.statusCode == 7 -> "No internet. Bills stay on this phone and upload later."
        e is ApiException -> "Google sign-in failed (code ${e.statusCode})"
        e is UnknownHostException || e is java.net.SocketTimeoutException || e is java.net.ConnectException ->
            "No internet. Bills stay on this phone and upload later."
        e is IOException -> e.message ?: "Google Drive error"
        else -> e.message ?: e.javaClass.simpleName
    }
}
