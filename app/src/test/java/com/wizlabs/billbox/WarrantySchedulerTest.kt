package com.wizlabs.billbox

import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.reminders.WarrantyScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WarrantySchedulerTest {
    private fun bill(id: String, until: LocalDate?) = Bill(id = id, store = "S", product = "P$id", invoiceDate = LocalDate.of(2026, 1, 1), warrantyUntil = until)
    private val all = listOf(30, 7, 1)
    private val today = LocalDate.of(2027, 3, 1)

    @Test fun reminderFiresOnThe30DayMark() {
        val r = WarrantyScheduler.dueToday(listOf(bill("a", today.plusDays(30))), all, today, emptySet())
        assertEquals(1, r.due.size); assertEquals(30, r.due[0].offset); assertEquals(30, r.due[0].daysLeft)
    }

    @Test fun sevenAndOneDayMarks() {
        assertEquals(7, WarrantyScheduler.dueToday(listOf(bill("a", today.plusDays(7))), all, today, setOf("a:30")).due.single().offset)
        assertEquals(1, WarrantyScheduler.dueToday(listOf(bill("a", today.plusDays(1))), all, today, setOf("a:30", "a:7")).due.single().offset)
    }

    @Test fun notBeforeThe30DayMark() {
        assertTrue(WarrantyScheduler.dueToday(listOf(bill("a", today.plusDays(31))), all, today, emptySet()).due.isEmpty())
    }

    @Test fun eachReminderOnlyOnce() {
        val b = listOf(bill("a", today.plusDays(30)))
        val first = WarrantyScheduler.dueToday(b, all, today, emptySet())
        val again = WarrantyScheduler.dueToday(b, all, today, first.mark)
        assertTrue(again.due.isEmpty())
    }

    @Test fun pastWarrantiesAreSkipped() {
        assertTrue(WarrantyScheduler.dueToday(listOf(bill("a", today.minusDays(1))), all, today, emptySet()).due.isEmpty())
    }

    @Test fun billWithoutWarrantyIsSkipped() {
        assertTrue(WarrantyScheduler.dueToday(listOf(bill("a", null)), all, today, emptySet()).due.isEmpty())
    }

    @Test fun lateAddedBillGetsOnlyTheClosestReminder() {
        val r = WarrantyScheduler.dueToday(listOf(bill("a", today.plusDays(5))), all, today, emptySet())
        assertEquals(1, r.due.size); assertEquals(7, r.due[0].offset); assertEquals(5, r.due[0].daysLeft)
        assertEquals(setOf("a:30", "a:7"), r.mark) // 30-day one is marked done too: no second notification later
    }

    @Test fun endsTodayStillReminds() {
        assertEquals(0, WarrantyScheduler.dueToday(listOf(bill("a", today)), all, today, setOf("a:30", "a:7")).due.single().daysLeft)
    }

    @Test fun switchedOffOffsetsAreIgnored() {
        val d = AppData(remind30 = false, remind7 = true, remind1 = false)
        assertEquals(listOf(7), WarrantyScheduler.enabledOffsets(d))
        assertTrue(WarrantyScheduler.enabledOffsets(AppData(remindersOn = false)).isEmpty())
        assertEquals(listOf(30, 7, 1), WarrantyScheduler.enabledOffsets(AppData()))
    }

    @Test fun leapYearDaysAreCountedRight() {
        // 2028 is a leap year: 28 Feb -> 29 Mar is 30 days
        val t = LocalDate.of(2028, 2, 28)
        assertEquals(30, WarrantyScheduler.dueToday(listOf(bill("a", LocalDate.of(2028, 3, 29))), all, t, emptySet()).due.single().daysLeft)
        // 1 year warranty from 29 Feb 2028 lands on 28 Feb 2029
        assertEquals(LocalDate.of(2029, 2, 28), LocalDate.of(2028, 2, 29).plusYears(1))
    }

    @Test fun nextCheckIsTodayAt10WhenStillAhead() {
        val zone = ZoneId.of("Asia/Kolkata")
        val n = WarrantyScheduler.nextCheck(ZonedDateTime.of(2027, 3, 1, 9, 30, 0, 0, zone))
        assertEquals(ZonedDateTime.of(2027, 3, 1, 10, 0, 0, 0, zone), n)
    }

    @Test fun nextCheckIsTomorrowAfter10() {
        val zone = ZoneId.of("Asia/Kolkata")
        assertEquals(ZonedDateTime.of(2027, 3, 2, 10, 0, 0, 0, zone), WarrantyScheduler.nextCheck(ZonedDateTime.of(2027, 3, 1, 10, 0, 0, 0, zone)))
        assertEquals(ZonedDateTime.of(2027, 3, 2, 10, 0, 0, 0, zone), WarrantyScheduler.nextCheck(ZonedDateTime.of(2027, 3, 1, 23, 59, 0, 0, zone)))
    }

    @Test fun timeZoneChangesTheInstantButNotTheLocalHour() {
        val ist = ZoneId.of("Asia/Kolkata"); val ny = ZoneId.of("America/New_York")
        val now = 1_800_000_000_000L
        val a = WarrantyScheduler.nextCheckMillis(now, ist); val b = WarrantyScheduler.nextCheckMillis(now, ny)
        assertEquals(10, java.time.Instant.ofEpochMilli(a).atZone(ist).hour)
        assertEquals(10, java.time.Instant.ofEpochMilli(b).atZone(ny).hour)
        assertTrue(a != b)
    }

    @Test fun nextCheckSurvivesDaylightSavingJump() {
        val ny = ZoneId.of("America/New_York")
        // 14 Mar 2027 is the US spring-forward day: 10:00 still exists and is local 10:00
        val n = WarrantyScheduler.nextCheck(ZonedDateTime.of(2027, 3, 13, 12, 0, 0, 0, ny))
        assertEquals(10, n.hour); assertEquals(14, n.dayOfMonth)
    }

    @Test fun pruneForgetsDeletedBillsAndChangedWarranties() {
        val kept = WarrantyScheduler.prune(setOf("a:30", "gone:7"), listOf(bill("a", today.plusDays(90))))
        assertEquals(setOf("a:30"), kept)
    }
}
