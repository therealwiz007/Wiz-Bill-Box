package com.wizlabs.billbox

import com.wizlabs.billbox.data.CsvExport
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.model.PaymentMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExportTest {
    private val b = Bill(
        store = "Croma, Delhi", product = "LG 43\" TV", invoiceDate = LocalDate.of(2026, 10, 2), amount = 32999.0,
        invoiceNumber = "CR-118", category = Category.ELECTRONICS, paymentMode = PaymentMode.UPI,
        warrantyUntil = LocalDate.of(2027, 10, 2), gstin = "07AAACC1234F1Z5", notes = "line1\nline2", createdAt = 1_790_000_000_000L,
    )

    @Test fun headerThenOneRowPerBill() {
        val lines = CsvExport.toCsv(listOf(b, b.copy(store = "X"))).trimEnd().split("\r\n")
        assertEquals(3, lines.size)
        assertEquals(13, CsvExport.HEADER.size)
    }

    @Test fun commasQuotesAndNewlinesAreEscaped() {
        val csv = CsvExport.toCsv(listOf(b))
        assertTrue(csv.contains("\"Croma, Delhi\""))
        assertTrue(csv.contains("\"LG 43\"\" TV\""))
        assertTrue(csv.contains("\"line1\nline2\""))
    }

    @Test fun amountsArePlainNumbersAndDatesAreIso() {
        val csv = CsvExport.toCsv(listOf(b, b.copy(amount = 1299.5, store = "B"), b.copy(amount = null, store = "C")))
        assertTrue(csv.contains(",32999,"))
        assertTrue(csv.contains(",1299.50,"))
        assertTrue(csv.contains("2026-10-02"))
        assertTrue(csv.contains("2027-10-02"))
        assertEquals("1299.50", CsvExport.plain(1299.5)); assertEquals("999", CsvExport.plain(999.0)); assertEquals("0.05", CsvExport.plain(0.05))
    }

    @Test fun formulaStartsAreNeutralised() {
        assertEquals("'=SUM(A1)", CsvExport.cell("=SUM(A1)"))
        assertEquals("'+91", CsvExport.cell("+91"))
        assertEquals("plain", CsvExport.cell("plain"))
    }

    @Test fun startsWithByteOrderMarkForSpreadsheetApps() {
        assertTrue(CsvExport.toCsv(emptyList()).startsWith("﻿"))
    }

    @Test fun newestBillsFirst() {
        val rows = CsvExport.toCsv(listOf(b.copy(store = "Old", invoiceDate = LocalDate.of(2020, 1, 1)), b.copy(store = "New"))).split("\r\n")
        assertTrue(rows[1].contains("New")); assertTrue(rows[2].contains("Old"))
    }
}
