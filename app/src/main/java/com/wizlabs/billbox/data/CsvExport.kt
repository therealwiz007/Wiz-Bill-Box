package com.wizlabs.billbox.data

import com.wizlabs.billbox.model.Bill
import java.time.Instant
import java.time.ZoneOffset

/** CSV of every bill (N6). Pure Kotlin. Amounts are plain numbers (rupees), dates are ISO (yyyy-MM-dd). */
object CsvExport {
    val HEADER = listOf(
        "Invoice date", "Store", "Product", "Amount (INR)", "Invoice no.", "Category", "Payment mode",
        "Warranty until", "GSTIN", "Notes", "Photos", "PDFs", "Added on",
    )

    fun toCsv(bills: List<Bill>): String = buildString {
        append('﻿') // so spreadsheet apps read UTF-8 (₹, Hindi notes) correctly
        append(row(HEADER))
        bills.sortedWith(compareByDescending<Bill> { it.invoiceDate }.thenBy { it.store.lowercase() }).forEach { b ->
            append(row(listOf(
                b.invoiceDate.toString(), b.store, b.product,
                b.amount?.let { plain(it) }.orEmpty(),
                b.invoiceNumber, b.category?.label.orEmpty(), b.paymentMode?.label.orEmpty(),
                b.warrantyUntil?.toString().orEmpty(), b.gstin, b.notes,
                b.photos.size.toString(), b.files.size.toString(),
                Instant.ofEpochMilli(b.createdAt).atZone(ZoneOffset.UTC).toLocalDate().toString(),
            )))
        }
    }

    /** 1299.5 -> "1299.50", 999.0 -> "999". */
    fun plain(v: Double): String {
        val paise = Math.round(v * 100)
        val r = paise / 100; val p = Math.abs(paise % 100)
        return if (p == 0L) r.toString() else "$r.${p.toString().padStart(2, '0')}"
    }

    fun cell(s: String): String {
        // Protect against spreadsheet formula injection from text typed or restored from Drive.
        val safe = if (s.isNotEmpty() && s[0] in "=+-@\t\r") "'$s" else s
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    private fun row(cells: List<String>) = cells.joinToString(",") { cell(it) } + "\r\n"
}
