package com.wizlabs.billbox

import com.wizlabs.billbox.engine.WarrantyStatus
import com.wizlabs.billbox.engine.WarrantyStatus.Kind
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.WarrantyFilter
import com.wizlabs.billbox.model.SortBy
import com.wizlabs.billbox.ui.screens.BillQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WarrantyStatusTest {
    private val today = LocalDate.of(2027, 6, 15)

    @Test fun kinds() {
        assertEquals(Kind.EXPIRED, WarrantyStatus.of(today.minusDays(1), today).kind)
        assertEquals(Kind.URGENT, WarrantyStatus.of(today, today).kind)
        assertEquals(Kind.URGENT, WarrantyStatus.of(today.plusDays(7), today).kind)
        assertEquals(Kind.EXPIRING, WarrantyStatus.of(today.plusDays(8), today).kind)
        assertEquals(Kind.EXPIRING, WarrantyStatus.of(today.plusDays(30), today).kind)
        assertEquals(Kind.ACTIVE, WarrantyStatus.of(today.plusDays(31), today).kind)
    }

    @Test fun windowIsConfigurable() {
        assertEquals(Kind.EXPIRING, WarrantyStatus.of(today.plusDays(60), today, windowDays = 90).kind)
        assertEquals(Kind.ACTIVE, WarrantyStatus.of(today.plusDays(60), today, windowDays = 30).kind)
    }

    @Test fun yearsMonthsDays() {
        val s = WarrantyStatus.of(LocalDate.of(2029, 9, 20), today)
        assertEquals(2, s.years); assertEquals(3, s.months); assertEquals(5, s.restDays)
    }

    private fun bill(id: String, until: LocalDate?) = Bill(id = id, store = "S$id", product = "P", invoiceDate = LocalDate.of(2026, 1, 1), warrantyUntil = until)

    @Test fun segmentedFiltersAndCounts() {
        val bills = listOf(
            bill("none", null), bill("far", today.plusDays(200)), bill("soon", today.plusDays(20)),
            bill("urgent", today.plusDays(3)), bill("old", today.minusDays(10)),
        )
        fun ids(f: WarrantyFilter) = BillQuery.apply(bills, "", null, SortBy.STORE, f, 30, today).map { it.id }.toSet()
        assertEquals(setOf("none", "far", "soon", "urgent", "old"), ids(WarrantyFilter.ALL))
        assertEquals(setOf("far", "soon", "urgent"), ids(WarrantyFilter.ACTIVE))
        assertEquals(setOf("soon", "urgent"), ids(WarrantyFilter.EXPIRING))
        assertEquals(setOf("old"), ids(WarrantyFilter.EXPIRED))
        val c = BillQuery.counts(bills, 30, today)
        assertEquals(5, c[WarrantyFilter.ALL]); assertEquals(3, c[WarrantyFilter.ACTIVE]); assertEquals(2, c[WarrantyFilter.EXPIRING]); assertEquals(1, c[WarrantyFilter.EXPIRED])
    }

    @Test fun filtersCombineWithSearch() {
        val bills = listOf(bill("a", today.plusDays(5)), bill("b", today.plusDays(5)).copy(store = "Zed"))
        assertEquals(listOf("b"), BillQuery.apply(bills, "zed", null, SortBy.DATE, WarrantyFilter.EXPIRING, 30, today).map { it.id })
    }

    @Test fun recentlyExpiredFindsOnlyRecentOnes() {
        assertEquals("old", BillQuery.recentlyExpired(listOf(bill("old", today.minusDays(10))), today)?.id)
        assertTrue(BillQuery.recentlyExpired(listOf(bill("older", today.minusDays(200))), today) == null)
    }
}
