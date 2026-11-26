package com.wizlabs.billbox

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.wizlabs.billbox.data.BillRepository
import com.wizlabs.billbox.data.CsvExport
import com.wizlabs.billbox.data.Files
import com.wizlabs.billbox.model.BillFile
import com.wizlabs.billbox.reminders.WarrantyAlarms
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
    data class Failed(val message: String, val offline: Boolean = false) : SyncState
    /** Google wants the user to pick the account / agree again. */
    data object NeedsSignIn : SyncState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private fun str(id: Int, vararg a: Any) = getApplication<Application>().getString(id, *a)

    val photos = Photos(app)
    val files = Files(app)
    private val repo = BillRepository(Store(File(app.filesDir, "bills.json")), photos, files)
    private val auth = DriveAuth(app)

    val data: StateFlow<AppData> = repo.data

    private val _sync = MutableStateFlow<SyncState>(SyncState.Idle)
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    /** Google's sign-in screen, which the activity has to launch. */
    private val _consent = Channel<PendingIntent>(Channel.BUFFERED)
    val consent = _consent.receiveAsFlow()

    /** The last sign-in problem, shown inline on the welcome screen with a Retry button. */
    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    private suspend fun signInFailed(e: Exception) {
        _signingIn.value = false
        val msg = explain(e)
        _signInError.value = msg
        val onWelcome = data.value.account == null && !data.value.skippedSignIn
        if (!onWelcome) _messages.send(msg)
    }

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** A bill the user asked to open from outside the app (a reminder notification). */
    private val _openBill = MutableStateFlow<String?>(null)
    val openBill: StateFlow<String?> = _openBill.asStateFlow()
    fun requestOpen(id: String?) { _openBill.value = id }
    fun openHandled() { _openBill.value = null }

    private var syncJob: Job? = null

    /**
     * Store-screenshot mode: debug builds only, switched on by a `demo-mode` file in app storage.
     * Sync is skipped so sample bills show as saved without a real Google account.
     */
    private val demoMode = (app.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 &&
        File(app.filesDir, "demo-mode").exists()

    init {
        if (data.value.account != null) requestSync()
        WarrantyAlarms.schedule(app, data.value)
    }

    // ---- Sign-in ----

    fun signIn() {
        if (_signingIn.value) return
        _signingIn.value = true
        _signInError.value = null
        viewModelScope.launch {
            try {
                when (val r = auth.authorize(data.value.account?.email)) {
                    is AuthOutcome.NeedsUser -> _consent.send(r.intent)
                    is AuthOutcome.Token -> onToken(r.token)
                }
            } catch (e: Exception) {
                signInFailed(e)
            }
        }
    }

    fun onConsentResult(resultOk: Boolean, data: Intent?) {
        viewModelScope.launch {
            try {
                if (!resultOk && data == null) { _signingIn.value = false; return@launch }
                when (val r = auth.fromIntent(data)) {
                    is AuthOutcome.Token -> onToken(r.token)
                    is AuthOutcome.NeedsUser -> { _signingIn.value = false; _messages.send(str(R.string.msg_signin_incomplete)) }
                }
            } catch (e: Exception) {
                signInFailed(e)
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
            signInFailed(e)
        }
    }

    fun skipSignIn() = repo.skipSignIn()

    fun signOut() {
        viewModelScope.launch {
            syncJob?.cancel()
            auth.signOut(data.value.account?.email)
            repo.clearLocal()
            _sync.value = SyncState.Idle
            _messages.send(str(R.string.msg_signed_out))
        }
    }

    // ---- Sync ----

    /** Uploads pending changes and pulls bills saved elsewhere. Silent: never shows Google's UI. */
    fun requestSync(delayMs: Long = 0) {
        if (data.value.account == null || demoMode) return
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
        _sync.value = SyncState.Failed(explain(e), isOffline(e)); null
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
            if (announce && r.restored > 0) _messages.send(getApplication<Application>().resources.getQuantityString(R.plurals.msg_restored, r.restored, r.restored))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _sync.value = SyncState.Failed(explain(e), isOffline(e))
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
        WarrantyAlarms.schedule(getApplication(), data.value)
        requestSync(delayMs = 800)
    }

    fun deleteBill(id: String) {
        repo.deleteBill(id)
        WarrantyAlarms.schedule(getApplication(), data.value)
        requestSync(delayMs = 800)
    }

    /** Copies a picked PDF into app storage. Returns the new [BillFile] or a short error key. */
    suspend fun importPdf(uri: Uri): Result<BillFile> = withContext(Dispatchers.IO) {
        runCatching { files.import(uri) }.map { BillFile(name = it.displayName, localName = it.localName) }
    }

    /** Makes sure a PDF saved from another phone is downloaded. */
    fun ensureFile(billId: String, fileId: String, done: (String?) -> Unit) {
        viewModelScope.launch {
            val token = silentToken()
            val name = if (token == null) null else runCatching { repo.fetchFile(token, billId, fileId) }.getOrNull()
            done(name)
        }
    }

    /** Writes every bill as CSV to a file the user picked (N6). */
    suspend fun exportCsv(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri)!!.use { it.write(CsvExport.toCsv(data.value.bills).toByteArray(Charsets.UTF_8)) }
        }.isSuccess
    }

    suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching { photos.import(uri) }.getOrNull()
    }

    fun setTheme(t: ThemeMode) = repo.setTheme(t)
    fun setReminderDays(d: Int) = repo.setReminderDays(d)
    fun setReminders(on: Boolean? = null, r30: Boolean? = null, r7: Boolean? = null, r1: Boolean? = null) {
        repo.setReminders(on, r30, r7, r1)
        WarrantyAlarms.schedule(getApplication(), data.value)
    }
    fun notifAsked() = repo.setNotifAsked()
    fun toast(msg: String) { viewModelScope.launch { _messages.send(msg) } }

    private fun isOffline(e: Exception) =
        e is UnknownHostException || e is java.net.SocketTimeoutException || e is java.net.ConnectException || (e is ApiException && e.statusCode == 7)

    private fun explain(e: Exception): String = when {
        e is ApiException && e.statusCode == 10 -> str(R.string.err_not_set_up)
        e is ApiException && (e.statusCode == 16 || e.statusCode == 12501) -> str(R.string.err_cancelled)
        e is ApiException && e.statusCode == 7 -> str(R.string.err_no_internet)
        e is ApiException -> str(R.string.err_signin_failed, e.statusCode)
        e is UnknownHostException || e is java.net.SocketTimeoutException || e is java.net.ConnectException -> str(R.string.err_no_internet)
        e is IOException -> e.message ?: str(R.string.err_drive)
        else -> e.message ?: e.javaClass.simpleName
    }
}
