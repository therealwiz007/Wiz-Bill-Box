package com.wizlabs.billbox

import com.wizlabs.billbox.data.BillRepository
import com.wizlabs.billbox.data.Codec
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.model.Account
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.BillPhoto
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.model.PaymentMode
import com.wizlabs.billbox.model.SortBy
import com.wizlabs.billbox.ui.screens.BillQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CoreTest {
    private val tv = Bill(
        store = "Croma", product = "LG 43\" TV", invoiceDate = LocalDate.of(2026, 10, 2), amount = 32999.0,
        invoiceNumber = "CR-118", category = Category.ELECTRONICS, paymentMode = PaymentMode.UPI,
        warrantyUntil = LocalDate.of(2027, 10, 2), notes = "Serial 7XK",
        photos = listOf(BillPhoto(localName = "a.jpg", driveId = "d1")), driveFolderId = "f", driveJsonId = "j", dirty = false,
    )
    private val milk = Bill(store = "Reliance Fresh", product = "Groceries", invoiceDate = LocalDate.of(2026, 11, 1), category = Category.GROCERY)

    @Test fun driveJsonRoundTripKeepsDetailsButNotPhoneFiles() {
        val back = Codec.billFromJson(Codec.billToJson(tv))
        assertEquals(tv.store, back.store); assertEquals(tv.product, back.product)
        assertEquals(tv.invoiceDate, back.invoiceDate); assertEquals(tv.amount, back.amount)
        assertEquals(tv.warrantyUntil, back.warrantyUntil); assertEquals(Category.ELECTRONICS, back.category)
        assertEquals("d1", back.photos.single().driveId)
        assertNull(back.photos.single().localName) // a new phone has to download it
    }

    @Test fun localStoreRoundTrip() {
        val d = AppData(bills = listOf(tv, milk), pendingDeletes = listOf("x"), account = Account("a@b.c", "A"), rootFolderId = "r")
        val back = Codec.decode(Codec.encode(d))
        assertEquals(d.copy(bills = back.bills), back)
        assertEquals("a.jpg", back.bills[0].photos[0].localName)
        assertFalse(back.bills[0].dirty); assertTrue(back.bills[1].dirty)
        assertNull(back.bills[1].amount)
    }

    @Test fun indianMoney() {
        assertEquals("1,23,45,678", Fmt.money(12345678.0))
        assertEquals("999", Fmt.money(999.0))
        assertEquals("1,299.50", Fmt.money(1299.5))
        assertEquals(1299.5, Fmt.parseAmount("₹1,299.50")!!, 0.0)
        assertNull(Fmt.parseAmount("abc"))
    }

    @Test fun searchAndSort() {
        val all = listOf(tv, milk)
        assertEquals(listOf(milk, tv), BillQuery.apply(all, "", null, SortBy.DATE))
        assertEquals(listOf(tv), BillQuery.apply(all, "cr-118", null, SortBy.DATE))
        assertEquals(listOf(milk), BillQuery.apply(all, "", Category.GROCERY, SortBy.DATE))
        assertEquals(listOf(tv, milk), BillQuery.apply(all, "", null, SortBy.AMOUNT))
    }

    @Test fun warrantySoon() {
        val today = LocalDate.of(2027, 9, 20)
        assertEquals(listOf(tv), BillQuery.warrantySoon(listOf(tv, milk), 30, today))
        assertTrue(BillQuery.warrantySoon(listOf(tv), 7, today).isEmpty())
    }

    @Test fun driveNamesAreSafe() {
        assertEquals("2026-10-02 · Croma · LG 43  TV", BillRepository.folderTitle(tv))
        assertEquals("A B", BillRepository.clean("A/B"))
    }
}
