package com.wizlabs.billbox.engine

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Where a warranty stands today. Pure Kotlin, unit-tested. */
data class WarrantyStatus(
    val kind: Kind,
    /** Whole days until the end date (negative once ended). */
    val days: Long,
    val years: Int, val months: Int, val restDays: Int,
) {
    enum class Kind {
        /** Ends beyond the "expiring" window. */
        ACTIVE,
        /** Ends within the user's window (7..90 days) but not within a week. */
        EXPIRING,
        /** Ends within 7 days (today included). */
        URGENT,
        EXPIRED,
    }

    val isActive get() = kind != Kind.EXPIRED

    companion object {
        fun of(until: LocalDate, today: LocalDate = LocalDate.now(), windowDays: Int = 30): WarrantyStatus {
            val days = ChronoUnit.DAYS.between(today, until)
            val kind = when {
                days < 0 -> Kind.EXPIRED
                days <= 7 -> Kind.URGENT
                days <= windowDays -> Kind.EXPIRING
                else -> Kind.ACTIVE
            }
            val p = if (days >= 0) Period.between(today, until) else Period.ZERO
            return WarrantyStatus(kind, days, p.years, p.months, p.days)
        }
    }
}
