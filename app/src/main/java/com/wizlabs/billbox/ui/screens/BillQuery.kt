package com.wizlabs.billbox.ui.screens

import com.wizlabs.billbox.engine.WarrantyStatus
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.model.SortBy
import com.wizlabs.billbox.model.WarrantyFilter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Filters and sorts bills for the list. Kept free of UI so it can be unit-tested. */
object BillQuery {
    fun apply(
        bills: List<Bill>, query: String, category: Category?, sort: SortBy,
        warranty: WarrantyFilter = WarrantyFilter.ALL, windowDays: Int = 30, today: LocalDate = LocalDate.now(),
    ): List<Bill> {
        val q = query.trim().lowercase()
        val filtered = bills.filter { b ->
            (category == null || b.category == category) && matches(b, warranty, windowDays, today) &&
                (q.isEmpty() || listOf(b.store, b.product, b.invoiceNumber, b.notes, b.gstin, b.category?.label.orEmpty())
                    .any { it.lowercase().contains(q) })
        }
        return when (sort) {
            SortBy.DATE -> filtered.sortedWith(compareByDescending<Bill> { it.invoiceDate }.thenByDescending { it.createdAt })
            SortBy.ADDED -> filtered.sortedByDescending { it.createdAt }
            SortBy.AMOUNT -> filtered.sortedByDescending { it.amount ?: -1.0 }
            SortBy.STORE -> filtered.sortedWith(compareBy<Bill> { it.store.lowercase() }.thenByDescending { it.invoiceDate })
        }
    }

    /** "Under warranty" = not ended; "Expiring" = ends within [windowDays]; "Expired" = ended. Bills without a warranty only match All. */
    fun matches(b: Bill, f: WarrantyFilter, windowDays: Int, today: LocalDate): Boolean {
        if (f == WarrantyFilter.ALL) return true
        val w = b.warrantyUntil ?: return false
        val st = WarrantyStatus.of(w, today, windowDays)
        return when (f) {
            WarrantyFilter.ALL -> true
            WarrantyFilter.ACTIVE -> st.isActive
            WarrantyFilter.EXPIRING -> st.kind == WarrantyStatus.Kind.EXPIRING || st.kind == WarrantyStatus.Kind.URGENT
            WarrantyFilter.EXPIRED -> st.kind == WarrantyStatus.Kind.EXPIRED
        }
    }

    fun counts(bills: List<Bill>, windowDays: Int, today: LocalDate = LocalDate.now()): Map<WarrantyFilter, Int> =
        WarrantyFilter.entries.associateWith { f -> bills.count { matches(it, f, windowDays, today) } }

    fun warrantySoon(bills: List<Bill>, days: Int, today: LocalDate = LocalDate.now()): List<Bill> =
        bills.filter { b -> b.warrantyUntil?.let { !it.isBefore(today) && ChronoUnit.DAYS.between(today, it) <= days } == true }
            .sortedBy { it.warrantyUntil }

    /** A warranty that ended within the last 30 days (for Tally's "ended" line). */
    fun recentlyExpired(bills: List<Bill>, today: LocalDate = LocalDate.now()): Bill? =
        bills.filter { b -> b.warrantyUntil?.let { it.isBefore(today) && ChronoUnit.DAYS.between(it, today) <= 30 } == true }
            .maxByOrNull { it.warrantyUntil!! }
}
