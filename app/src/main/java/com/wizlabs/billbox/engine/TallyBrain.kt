package com.wizlabs.billbox.engine

/** How Tally looks. Pure Kotlin (no Android types) so every mapping can be unit-tested. */
enum class Expression { NEUTRAL, HAPPY, PROUD, CURIOUS, SLEEPY, WORRIED, ALERT, SEARCHING }

/** What Tally does once when the mood starts (or keeps doing, for [LID_BOB]). */
enum class Action {
    /** Perfectly still. */
    STILL,
    /** Lid opens for 300 ms (tap on the empty box). */
    LID_OPEN,
    /** Receipt drops in: lid open then close in 380 ms, check badge pops. */
    RECEIPT_DROP,
    /** Small hop, 240 ms. */
    HOP,
    /** Lid bobs +-3 degrees while uploading. */
    LID_BOB,
    /** One shake, 280 ms. */
    SHAKE,
    /** Bell rings: +-12 degrees, 3 swings, 600 ms, once. */
    BELL_RING,
    /** Magnifier sweeps once. */
    MAGNIFIER_SWEEP,
    /** Onboarding page 1. */
    WAVE,
    /** Onboarding page 2: receipt slides into the box. */
    FILE,
    /** Onboarding page 3: bell. */
    BELL,
}

/** The sentence Tally says. The UI maps each key to a (translated) string resource. */
sealed interface Line {
    data object EmptyBox : Line
    data object Saved : Line
    data object SafeInDrive : Line
    data class Uploading(val count: Int) : Line
    data object Offline : Line
    data object SyncError : Line
    data object SignInToBackUp : Line
    data class WarrantiesSoon(val count: Int) : Line
    data class WarrantyEnded(val product: String) : Line
    data object NoMatch : Line
    data object Onboard1 : Line
    data object Onboard2 : Line
    data object Onboard3 : Line
}

data class Mood(val expression: Expression, val action: Action, val line: Line?)

/** Everything the brain needs, as plain values. */
data class TallyContext(
    val bills: Int = 0,
    val signedIn: Boolean = false,
    val uploading: Int = 0,
    val offline: Boolean = false,
    val syncError: Boolean = false,
    /** Warranties ending within 30 days (not yet ended). */
    val expiringSoon: Int = 0,
    /** Product name of a warranty that ended recently, if any. */
    val recentlyExpired: String? = null,
    val searchNoResults: Boolean = false,
    /** A bill was saved a moment ago. */
    val justSaved: Boolean = false,
    /** The last upload just finished. */
    val justUploaded: Boolean = false,
)

object TallyBrain {

    /** Mood for the Home screen and Settings backup card. First matching rule wins. */
    fun decide(c: TallyContext): Mood = when {
        c.justSaved -> Mood(Expression.PROUD, Action.RECEIPT_DROP, Line.Saved)
        c.justUploaded -> Mood(Expression.HAPPY, Action.HOP, Line.SafeInDrive)
        c.searchNoResults -> Mood(Expression.SEARCHING, Action.MAGNIFIER_SWEEP, Line.NoMatch)
        c.bills == 0 && !c.syncError -> Mood(Expression.SLEEPY, Action.STILL, Line.EmptyBox)
        c.syncError -> Mood(Expression.WORRIED, Action.SHAKE, Line.SyncError)
        c.signedIn && c.offline -> Mood(Expression.SLEEPY, Action.STILL, Line.Offline)
        c.signedIn && c.uploading > 0 -> Mood(Expression.NEUTRAL, Action.LID_BOB, Line.Uploading(c.uploading))
        !c.signedIn -> Mood(Expression.CURIOUS, Action.STILL, Line.SignInToBackUp)
        c.expiringSoon > 0 -> Mood(Expression.ALERT, Action.BELL_RING, Line.WarrantiesSoon(c.expiringSoon))
        c.recentlyExpired != null -> Mood(Expression.NEUTRAL, Action.STILL, Line.WarrantyEnded(c.recentlyExpired))
        else -> Mood(Expression.HAPPY, Action.STILL, null)
    }

    /** Tapping the sleepy empty box wakes it up and opens the lid. */
    fun onTap(m: Mood): Mood =
        if (m.expression == Expression.SLEEPY && m.line == Line.EmptyBox) Mood(Expression.CURIOUS, Action.LID_OPEN, Line.EmptyBox)
        else m

    /** Onboarding pages 0..2. */
    fun onboarding(page: Int): Mood = when (page) {
        0 -> Mood(Expression.HAPPY, Action.WAVE, Line.Onboard1)
        1 -> Mood(Expression.PROUD, Action.FILE, Line.Onboard2)
        else -> Mood(Expression.ALERT, Action.BELL, Line.Onboard3)
    }

    /** Duration of each one-shot action in ms (0 = loops or none). */
    fun durationMs(a: Action): Int = when (a) {
        Action.LID_OPEN -> 300
        Action.RECEIPT_DROP -> 380
        Action.HOP -> 240
        Action.SHAKE -> 280
        Action.BELL_RING, Action.BELL -> 600
        Action.MAGNIFIER_SWEEP -> 500
        Action.WAVE -> 700
        Action.FILE -> 420
        Action.STILL, Action.LID_BOB -> 0
    }
}
