package com.wizlabs.billbox.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Fmt {
    private val day = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val month = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun date(d: LocalDate): String = d.format(day)
    fun month(d: LocalDate): String = d.format(month)

    /** Indian digit grouping: 12,34,567.50. Paise are shown only when present. */
    fun money(v: Double): String {
        val neg = v < 0
        val paise = (abs(v) * 100).roundToLong()
        val rupees = paise / 100
        val rem = paise % 100
        val digits = rupees.toString()
        val grouped = if (digits.length <= 3) digits else {
            val last3 = digits.takeLast(3)
            val rest = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$rest,$last3"
        }
        val s = if (rem == 0L) grouped else "$grouped.${rem.toString().padStart(2, '0')}"
        return if (neg) "-$s" else s
    }

    fun rupees(v: Double) = "₹" + money(v)

    /** "Warranty ends in 12 days", "Warranty ended 3 months ago". */
    fun warranty(until: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, until)
        return when {
            days == 0L -> "Warranty ends today"
            days > 0 -> "Warranty ends in ${span(days)}"
            else -> "Warranty ended ${span(-days)} ago"
        }
    }

    private fun span(days: Long): String = when {
        days < 45 -> "$days day${if (days == 1L) "" else "s"}"
        days < 365 * 2 -> "${(days / 30.4).roundToLong()} months"
        else -> "${days / 365} years"
    }

    fun ago(millis: Long, now: Long = System.currentTimeMillis()): String {
        val s = (now - millis) / 1000
        return when {
            s < 60 -> "just now"
            s < 3600 -> "${s / 60} min ago"
            s < 86400 -> "${s / 3600} h ago"
            else -> "${s / 86400} d ago"
        }
    }

    /** Parses what the user typed in the amount box ("1,299.50", "₹ 499"). */
    fun parseAmount(s: String): Double? = s.replace(",", "").replace("₹", "").trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
}
