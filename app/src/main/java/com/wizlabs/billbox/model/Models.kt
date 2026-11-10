package com.wizlabs.billbox.model

import java.time.LocalDate
import java.util.UUID

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class Category(val label: String) {
    ELECTRONICS("Electronics"),
    APPLIANCES("Appliances"),
    GROCERY("Grocery"),
    FUEL("Fuel"),
    MEDICAL("Medical"),
    CLOTHING("Clothing"),
    FURNITURE("Furniture"),
    VEHICLE("Vehicle"),
    UTILITIES("Utilities"),
    RESTAURANT("Food & dining"),
    TRAVEL("Travel"),
    OTHER("Other");

    companion object {
        fun of(name: String?): Category? = entries.firstOrNull { it.name == name }
    }
}

enum class PaymentMode(val label: String) {
    CASH("Cash"), UPI("UPI"), CARD("Card"), NET_BANKING("Net banking"), EMI("EMI"), OTHER("Other");

    companion object {
        fun of(name: String?): PaymentMode? = entries.firstOrNull { it.name == name }
    }
}

/**
 * One photo of a bill. [localName] is the JPEG inside the app's files (null until it has been
 * downloaded on a new phone). [driveId] is the Drive file (null until it has been uploaded).
 */
data class BillPhoto(
    val id: String = UUID.randomUUID().toString(),
    val localName: String? = null,
    val driveId: String? = null,
)

data class Bill(
    val id: String = UUID.randomUUID().toString(),
    // Required
    val store: String,
    val product: String,
    val invoiceDate: LocalDate,
    // Optional
    val amount: Double? = null,
    val invoiceNumber: String = "",
    val category: Category? = null,
    val paymentMode: PaymentMode? = null,
    val warrantyUntil: LocalDate? = null,
    val gstin: String = "",
    val notes: String = "",
    val photos: List<BillPhoto> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Drive bookkeeping (not shown to the user)
    val driveFolderId: String? = null,
    val driveJsonId: String? = null,
    /** True when this phone has changes Drive doesn't have yet. */
    val dirty: Boolean = true,
)

data class Account(val email: String, val name: String = "")

data class AppData(
    val bills: List<Bill> = emptyList(),
    /** Bills deleted on this phone whose Drive folder still has to be moved to the bin. */
    val pendingDeletes: List<String> = emptyList(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val account: Account? = null,
    val rootFolderId: String? = null,
    val lastSyncAt: Long? = null,
    val warrantyReminderDays: Int = 30,
    /** The user chose "Use without Google for now" on the welcome screen. */
    val skippedSignIn: Boolean = false,
)

enum class SortBy(val label: String) { DATE("Invoice date"), ADDED("Recently added"), AMOUNT("Amount"), STORE("Store") }
