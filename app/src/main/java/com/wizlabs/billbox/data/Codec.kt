package com.wizlabs.billbox.data

import com.wizlabs.billbox.model.Account
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.BillPhoto
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.model.PaymentMode
import com.wizlabs.billbox.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** JSON encoding shared by the local store and the bill.json files kept in Google Drive. */
object Codec {
    const val FORMAT = 1

    /** The portable form of a bill: what goes into Drive. No phone-specific file names. */
    fun billToJson(b: Bill): JSONObject = JSONObject().apply {
        put("format", FORMAT)
        put("app", "Wiz Bill Box")
        put("id", b.id)
        put("store", b.store)
        put("product", b.product)
        put("invoiceDate", b.invoiceDate.toString())
        put("amount", b.amount ?: JSONObject.NULL)
        put("invoiceNumber", b.invoiceNumber)
        put("category", b.category?.name ?: JSONObject.NULL)
        put("paymentMode", b.paymentMode?.name ?: JSONObject.NULL)
        put("warrantyUntil", b.warrantyUntil?.toString() ?: JSONObject.NULL)
        put("gstin", b.gstin)
        put("notes", b.notes)
        put("createdAt", b.createdAt)
        put("updatedAt", b.updatedAt)
        put("photos", JSONArray().apply {
            b.photos.forEach { p -> put(JSONObject().put("id", p.id).put("driveId", p.driveId ?: JSONObject.NULL)) }
        })
    }

    fun billFromJson(o: JSONObject): Bill = Bill(
        id = o.getString("id"),
        store = o.optString("store"),
        product = o.optString("product"),
        invoiceDate = LocalDate.parse(o.getString("invoiceDate")),
        amount = if (o.isNull("amount") || !o.has("amount")) null else o.getDouble("amount"),
        invoiceNumber = o.optString("invoiceNumber"),
        category = Category.of(o.optStringOrNull("category")),
        paymentMode = PaymentMode.of(o.optStringOrNull("paymentMode")),
        warrantyUntil = o.optStringOrNull("warrantyUntil")?.let { LocalDate.parse(it) },
        gstin = o.optString("gstin"),
        notes = o.optString("notes"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
        photos = o.optJSONArray("photos").objects().map {
            BillPhoto(id = it.getString("id"), localName = it.optStringOrNull("localName"), driveId = it.optStringOrNull("driveId"))
        },
        driveFolderId = o.optStringOrNull("driveFolderId"),
        driveJsonId = o.optStringOrNull("driveJsonId"),
        dirty = o.optBoolean("dirty", false),
    )

    /** Local form: the portable form plus the phone's own bookkeeping. */
    private fun billToLocalJson(b: Bill): JSONObject = billToJson(b).apply {
        put("driveFolderId", b.driveFolderId ?: JSONObject.NULL)
        put("driveJsonId", b.driveJsonId ?: JSONObject.NULL)
        put("dirty", b.dirty)
        put("photos", JSONArray().apply {
            b.photos.forEach { p ->
                put(JSONObject().put("id", p.id).put("driveId", p.driveId ?: JSONObject.NULL).put("localName", p.localName ?: JSONObject.NULL))
            }
        })
    }

    fun encode(d: AppData): String = JSONObject().apply {
        put("version", FORMAT)
        put("theme", d.theme.name)
        put("account", d.account?.let { JSONObject().put("email", it.email).put("name", it.name) } ?: JSONObject.NULL)
        put("rootFolderId", d.rootFolderId ?: JSONObject.NULL)
        put("lastSyncAt", d.lastSyncAt ?: JSONObject.NULL)
        put("warrantyReminderDays", d.warrantyReminderDays)
        put("skippedSignIn", d.skippedSignIn)
        put("pendingDeletes", JSONArray(d.pendingDeletes))
        put("bills", JSONArray().apply { d.bills.forEach { put(billToLocalJson(it)) } })
    }.toString()

    fun decode(text: String): AppData {
        val o = JSONObject(text)
        return AppData(
            bills = o.optJSONArray("bills").objects().map { billFromJson(it) },
            pendingDeletes = o.optJSONArray("pendingDeletes")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            theme = runCatching { ThemeMode.valueOf(o.optString("theme")) }.getOrDefault(ThemeMode.SYSTEM),
            account = o.optJSONObject("account")?.let { Account(it.getString("email"), it.optString("name")) },
            rootFolderId = o.optStringOrNull("rootFolderId"),
            lastSyncAt = if (o.isNull("lastSyncAt") || !o.has("lastSyncAt")) null else o.getLong("lastSyncAt"),
            warrantyReminderDays = o.optInt("warrantyReminderDays", 30),
            skippedSignIn = o.optBoolean("skippedSignIn", false),
        )
    }
}

internal fun JSONObject.optStringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

internal fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
