package com.wizlabs.billbox.reminders

import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** One reminder to show now. [daysLeft] is the real number of days until the warranty ends. */
data class Due(val billId: String, val offset: Int, val daysLeft: Int)

/**
 * Pure maths for warranty reminders (N3). No Android types.
 * The phone checks once a day at [HOUR]:00 local time; a bill is reminded at most once per enabled offset (30, 7, 1 days).
 */
object WarrantyScheduler {
    const val HOUR = 10
    val OFFSETS = listOf(30, 7, 1)

    /** Offsets the user left switched on, largest first. Empty when reminders are off. */
    fun enabledOffsets(d: AppData): List<Int> =
        if (!d.remindersOn) emptyList()
        else OFFSETS.filter { (it == 30 && d.remind30) || (it == 7 && d.remind7) || (it == 1 && d.remind1) }

    fun key(billId: String, offset: Int) = "$billId:$offset"

    /**
     * Reminders to post today. A warranty that ended is skipped. For a bill added with 5 days left only the 7-day
     * reminder fires (the larger 30-day one is marked done at the same time), so nobody gets two at once.
     * [notified] holds keys already shown; the result's [Result.mark] lists keys to add to it.
     */
    fun dueToday(bills: List<Bill>, offsets: List<Int>, today: LocalDate, notified: Set<String>): Result {
        val due = mutableListOf<Due>()
        val mark = mutableSetOf<String>()
        for (b in bills) {
            val w = b.warrantyUntil ?: continue
            val left = ChronoUnit.DAYS.between(today, w).toInt()
            if (left < 0) continue
            // offsets whose moment has come (left <= offset), smallest = the most relevant one
            val reached = offsets.filter { left <= it }
            if (reached.isEmpty()) continue
            val fresh = reached.filter { key(b.id, it) !in notified }
            if (fresh.isEmpty()) continue
            due += Due(b.id, reached.min(), left)
            reached.forEach { mark += key(b.id, it) }
        }
        return Result(due.sortedBy { it.daysLeft }, mark)
    }

    data class Result(val due: List<Due>, val mark: Set<String>)

    /** The next check: today at 10:00 if that is still ahead, else tomorrow at 10:00 (DST-safe, uses the zone's rules). */
    fun nextCheck(now: ZonedDateTime): ZonedDateTime {
        val today = ZonedDateTime.of(now.toLocalDate(), LocalTime.of(HOUR, 0), now.zone)
        return if (today.isAfter(now)) today
        else ZonedDateTime.of(now.toLocalDate().plusDays(1), LocalTime.of(HOUR, 0), now.zone)
    }

    fun nextCheckMillis(nowMillis: Long, zone: ZoneId): Long =
        nextCheck(java.time.Instant.ofEpochMilli(nowMillis).atZone(zone)).toInstant().toEpochMilli()

    /** Keys for bills that no longer exist or whose end date moved, so a changed warranty can remind again. */
    fun prune(notified: Set<String>, bills: List<Bill>): Set<String> {
        val ids = bills.filter { it.warrantyUntil != null }.associate { it.id to it.warrantyUntil }
        return notified.filter { k -> k.substringBeforeLast(':') in ids }.toSet()
    }
}
