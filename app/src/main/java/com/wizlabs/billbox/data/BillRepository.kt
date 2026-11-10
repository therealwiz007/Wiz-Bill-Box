package com.wizlabs.billbox.data

import com.wizlabs.billbox.drive.DriveApi
import com.wizlabs.billbox.drive.DriveHttpException
import com.wizlabs.billbox.model.Account
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.format.DateTimeFormatter

data class SyncResult(val uploaded: Int, val restored: Int, val removed: Int)

/**
 * Owns the bill list. Every change is saved on the phone at once and marked "dirty";
 * [sync] then pushes dirty bills to Google Drive and pulls bills saved from other phones.
 *
 * Drive layout (all inside the user's own Drive, visible to them):
 * ```
 * Wiz Bill Box/
 *   2026-11-10 · Croma · LG 43" TV/
 *     Croma - LG 43" TV (1).jpg
 *     bill.json              ← store, product, date and the optional details
 * ```
 */
class BillRepository(private val store: Store, val photos: Photos) {

    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val syncLock = Mutex()

    private fun commit(change: (AppData) -> AppData) {
        _data.update(change)
        store.save(_data.value)
    }

    // ---- Local edits ----

    fun saveBill(bill: Bill) = commit { d ->
        val old = d.bills.find { it.id == bill.id }
        val removed = old?.photos.orEmpty().filter { p -> bill.photos.none { it.id == p.id } }
        removed.forEach { photos.delete(it.localName) }
        val saved = bill.copy(updatedAt = System.currentTimeMillis(), dirty = true)
        d.copy(
            bills = if (old == null) d.bills + saved else d.bills.map { if (it.id == bill.id) saved else it },
            pendingDeletes = d.pendingDeletes + removed.mapNotNull { it.driveId },
        )
    }

    fun deleteBill(id: String) = commit { d ->
        val b = d.bills.find { it.id == id } ?: return@commit d
        b.photos.forEach { photos.delete(it.localName) }
        d.copy(bills = d.bills.filter { it.id != id }, pendingDeletes = d.pendingDeletes + listOfNotNull(b.driveFolderId))
    }

    fun setTheme(t: ThemeMode) = commit { it.copy(theme = t) }
    fun setReminderDays(days: Int) = commit { it.copy(warrantyReminderDays = days) }
    fun setAccount(a: Account) = commit { it.copy(account = a, skippedSignIn = false) }
    fun skipSignIn() = commit { it.copy(skippedSignIn = true) }

    /** Forgets everything on this phone (after sign-out). Drive keeps the bills. */
    fun clearLocal() {
        val theme = _data.value.theme
        photos.deleteAll()
        _data.value = AppData(theme = theme)
        store.save(_data.value)
    }

    // ---- Drive sync ----

    suspend fun sync(token: String): SyncResult = syncLock.withLock {
        withContext(Dispatchers.IO) {
            val api = DriveApi(token)
            if (_data.value.account == null) {
                val (email, name) = api.about()
                setAccount(Account(email, name))
            }
            val root = api.findOrCreateRoot(_data.value.rootFolderId)
            if (root != _data.value.rootFolderId) commit { it.copy(rootFolderId = root) }

            // 1. Things deleted on this phone go to the Drive bin.
            for (id in _data.value.pendingDeletes.toList()) {
                api.trash(id)
                commit { it.copy(pendingDeletes = it.pendingDeletes - id) }
            }

            // 2. Upload new and edited bills.
            var uploaded = 0
            for (b in _data.value.bills.filter { it.dirty }) {
                push(api, root, b)
                uploaded++
            }

            // 3. Pull bills saved from other phones (or before a reinstall).
            val remote = api.listBills()
            var restored = 0
            for (r in remote) {
                val local = _data.value.bills.find { it.id == r.billId }
                val newer = local != null && !local.dirty && r.updatedAt > local.updatedAt
                if (local != null && !newer) {
                    if (local.driveJsonId != r.jsonId || local.driveFolderId != r.folderId) {
                        replace(local.copy(driveJsonId = r.jsonId, driveFolderId = r.folderId))
                    }
                    continue
                }
                val fetched = Codec.billFromJson(JSONObject(api.download(r.jsonId).decodeToString()))
                val merged = fetched.copy(
                    driveFolderId = r.folderId, driveJsonId = r.jsonId, dirty = false,
                    photos = fetched.photos.map { p -> p.copy(localName = local?.photos?.find { it.id == p.id }?.localName) },
                )
                local?.photos?.filter { lp -> merged.photos.none { it.id == lp.id } }?.forEach { photos.delete(it.localName) }
                commit { d ->
                    val cur = d.bills.find { it.id == merged.id }
                    if (cur != null && cur.dirty) d // edited meanwhile; keep the user's change
                    else d.copy(bills = if (cur == null) d.bills + merged else d.bills.map { if (it.id == merged.id) merged else it })
                }
                restored++
            }

            // 4. Bills deleted from another phone (or from Drive itself) disappear here too.
            val remoteIds = remote.map { it.billId }.toSet()
            val gone = _data.value.bills.filter { !it.dirty && it.driveJsonId != null && it.id !in remoteIds }
            if (gone.isNotEmpty()) {
                gone.forEach { b -> b.photos.forEach { photos.delete(it.localName) } }
                val ids = gone.map { it.id }.toSet()
                commit { d -> d.copy(bills = d.bills.filter { it.id !in ids || it.dirty }) }
            }

            commit { it.copy(lastSyncAt = System.currentTimeMillis()) }
            SyncResult(uploaded, restored, gone.size)
        }
    }

    private fun push(api: DriveApi, root: String, start: Bill) {
        var b = start
        val title = folderTitle(b)
        val desc = description(b)

        // Folder: rename the existing one, or create it (and remember it straight away).
        val folder = b.driveFolderId?.let { id ->
            try { api.rename(id, title, desc); id } catch (e: DriveHttpException) { if (e.code == 404) null else throw e }
        } ?: api.createFolder(title, root, mapOf("wizKind" to "billFolder", "billId" to b.id), desc).also { id ->
            // A brand-new folder (or one deleted in Drive): everything in it has to be uploaded again.
            b = b.copy(
                driveFolderId = id, driveJsonId = null,
                photos = b.photos.map { p -> if (p.localName != null && photos.file(p.localName).exists()) p.copy(driveId = null) else p },
            )
            mergeDrive(b)
        }

        // Photos that aren't in Drive yet.
        b.photos.forEachIndexed { i, p ->
            if (p.driveId != null) return@forEachIndexed
            val name = p.localName ?: return@forEachIndexed
            val f = photos.file(name)
            if (!f.exists()) return@forEachIndexed
            val id = api.upload(
                name = "${clean(b.store)} - ${clean(b.product)} (${i + 1}).jpg",
                mime = "image/jpeg", content = f.readBytes(), parent = folder,
                props = mapOf("wizKind" to "photo", "billId" to b.id, "photoId" to p.id),
                description = desc,
            )
            b = b.copy(photos = b.photos.map { if (it.id == p.id) it.copy(driveId = id) else it })
            mergeDrive(b)
        }

        // The bill details.
        val json = Codec.billToJson(b).toString(2).toByteArray()
        val props = mapOf("wizKind" to "bill", "billId" to b.id, "updatedAt" to b.updatedAt.toString())
        val jsonId = b.driveJsonId?.let { id ->
            try { api.update(id, "bill.json", "application/json", json, props); id }
            catch (e: DriveHttpException) { if (e.code == 404) null else throw e }
        } ?: api.upload("bill.json", "application/json", json, folder, props)
        b = b.copy(driveJsonId = jsonId)
        mergeDrive(b, done = true)
    }

    /** Copies Drive ids from a bill being uploaded into the live list without losing edits made meanwhile. */
    private fun mergeDrive(pushed: Bill, done: Boolean = false) = commit { d ->
        d.copy(bills = d.bills.map { cur ->
            if (cur.id != pushed.id) cur
            else cur.copy(
                driveFolderId = pushed.driveFolderId,
                driveJsonId = pushed.driveJsonId,
                photos = cur.photos.map { p -> pushed.photos.find { it.id == p.id }?.let { p.copy(driveId = it.driveId) } ?: p },
                dirty = if (done) cur.updatedAt != pushed.updatedAt else cur.dirty,
            )
        })
    }

    private fun replace(b: Bill) = commit { d -> d.copy(bills = d.bills.map { if (it.id == b.id) b else it }) }

    /** Downloads a photo saved from another phone. Returns its local file name. */
    suspend fun fetchPhoto(token: String, billId: String, photoId: String): String? = withContext(Dispatchers.IO) {
        val bill = _data.value.bills.find { it.id == billId } ?: return@withContext null
        val p = bill.photos.find { it.id == photoId } ?: return@withContext null
        p.localName?.takeIf { photos.file(it).exists() }?.let { return@withContext it }
        val driveId = p.driveId ?: return@withContext null
        val name = "${p.id}.jpg"
        DriveApi(token).downloadTo(driveId, photos.file(name))
        commit { d ->
            d.copy(bills = d.bills.map { b ->
                if (b.id != billId) b else b.copy(photos = b.photos.map { if (it.id == photoId) it.copy(localName = name) else it })
            })
        }
        name
    }

    companion object {
        private val DAY = DateTimeFormatter.ISO_LOCAL_DATE

        fun folderTitle(b: Bill) = "${b.invoiceDate.format(DAY)} · ${clean(b.store)} · ${clean(b.product)}"

        fun description(b: Bill) = buildString {
            append("${b.store} — ${b.product}\nInvoice date: ${b.invoiceDate}")
            b.amount?.let { append("\nAmount: ₹${Fmt.money(it)}") }
            if (b.invoiceNumber.isNotBlank()) append("\nInvoice no: ${b.invoiceNumber}")
            b.warrantyUntil?.let { append("\nWarranty until: $it") }
            append("\nSaved with Wiz Bill Box")
        }

        fun clean(s: String) = s.replace(Regex("[\\\\/:*?\"<>|\\n\\r]"), " ").trim().take(60).ifEmpty { "Bill" }
    }
}
