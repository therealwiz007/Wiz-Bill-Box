package com.wizlabs.billbox

import com.wizlabs.billbox.data.Codec
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.BillFile
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** bill.json must stay readable both ways between 1.x and 2.0.0 (handoff 25). */
class CodecCompatTest {
    /** Exactly what version 1.1.0 wrote to Drive: no "files" field. */
    private val oldJson = """
        {"format":1,"app":"Wiz Bill Box","id":"b1","store":"Croma","product":"TV","invoiceDate":"2026-10-02","amount":32999.0,
         "invoiceNumber":"CR-1","category":"ELECTRONICS","paymentMode":"UPI","warrantyUntil":"2027-10-02","gstin":"","notes":"",
         "createdAt":1,"updatedAt":2,"photos":[{"id":"p1","driveId":"d1"}]}
    """.trimIndent()

    @Test fun oldBillJsonDecodesWithNoFiles() {
        val b = Codec.billFromJson(JSONObject(oldJson))
        assertEquals("Croma", b.store); assertTrue(b.files.isEmpty()); assertEquals("d1", b.photos.single().driveId)
    }

    @Test fun newBillWithFilesRoundTrips() {
        val b = Bill(id = "x", store = "S", product = "P", invoiceDate = LocalDate.of(2027, 1, 1), files = listOf(BillFile(id = "f1", name = "inv.pdf", driveId = "dr")))
        val back = Codec.billFromJson(Codec.billToJson(b))
        assertEquals(listOf(BillFile(id = "f1", name = "inv.pdf", mime = "application/pdf", localName = null, driveId = "dr")), back.files)
    }

    @Test fun billWithoutFilesWritesNoFilesField() {
        val b = Bill(store = "S", product = "P", invoiceDate = LocalDate.of(2027, 1, 1))
        assertFalse(Codec.billToJson(b).has("files"))
    }

    @Test fun oldReaderIgnoresTheNewField() {
        // The 1.x reader only calls getString / optX on known keys; org.json never fails on an extra key.
        val b = Bill(store = "S", product = "P", invoiceDate = LocalDate.of(2027, 1, 1), files = listOf(BillFile(name = "a.pdf")))
        val o = Codec.billToJson(b)
        val keys = o.keys().asSequence().toSet()
        assertTrue(keys.containsAll(setOf("id", "store", "product", "invoiceDate", "photos", "updatedAt")))
        assertEquals("S", o.getString("store")); assertEquals(1, o.getInt("format"))
    }

    @Test fun localStoreKeepsPdfLocalNamesAndReminderSettings() {
        val b = Bill(id = "x", store = "S", product = "P", invoiceDate = LocalDate.of(2027, 1, 1), files = listOf(BillFile(id = "f", name = "a.pdf", localName = "f.pdf")))
        val d = AppData(bills = listOf(b), remind7 = false, remindersOn = true, notifAsked = true)
        val back = Codec.decode(Codec.encode(d))
        assertEquals("f.pdf", back.bills.single().files.single().localName)
        assertFalse(back.remind7); assertTrue(back.remind30); assertTrue(back.notifAsked)
    }

    @Test fun oldLocalStoreWithoutReminderFieldsGetsDefaults() {
        val old = """{"version":1,"theme":"SYSTEM","account":null,"rootFolderId":null,"lastSyncAt":null,"warrantyReminderDays":30,"skippedSignIn":false,"pendingDeletes":[],"bills":[]}"""
        val d = Codec.decode(old)
        assertTrue(d.remindersOn && d.remind30 && d.remind7 && d.remind1); assertFalse(d.notifAsked)
        assertNull(d.account)
    }
}
