package com.wizlabs.billbox

import com.wizlabs.billbox.engine.Action
import com.wizlabs.billbox.engine.Expression
import com.wizlabs.billbox.engine.Line
import com.wizlabs.billbox.engine.TallyBrain
import com.wizlabs.billbox.engine.TallyContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TallyBrainTest {
    private val ok = TallyContext(bills = 3, signedIn = true)

    @Test fun noBillsIsSleepyAndStill() {
        val m = TallyBrain.decide(TallyContext())
        assertEquals(Expression.SLEEPY, m.expression); assertEquals(Action.STILL, m.action); assertEquals(Line.EmptyBox, m.line)
    }

    @Test fun tapWakesTheEmptyBox() {
        val m = TallyBrain.onTap(TallyBrain.decide(TallyContext()))
        assertEquals(Expression.CURIOUS, m.expression); assertEquals(Action.LID_OPEN, m.action)
    }

    @Test fun tapDoesNothingElsewhere() {
        val m = TallyBrain.decide(ok.copy(syncError = true))
        assertEquals(m, TallyBrain.onTap(m))
    }

    @Test fun billSavedIsProudWithReceiptDrop() {
        val m = TallyBrain.decide(ok.copy(justSaved = true))
        assertEquals(Expression.PROUD, m.expression); assertEquals(Action.RECEIPT_DROP, m.action); assertEquals(Line.Saved, m.line)
    }

    @Test fun uploadDoneIsHappyHop() {
        val m = TallyBrain.decide(ok.copy(justUploaded = true))
        assertEquals(Expression.HAPPY, m.expression); assertEquals(Action.HOP, m.action); assertEquals(Line.SafeInDrive, m.line)
    }

    @Test fun uploadingBobsTheLid() {
        val m = TallyBrain.decide(ok.copy(uploading = 2))
        assertEquals(Expression.NEUTRAL, m.expression); assertEquals(Action.LID_BOB, m.action); assertEquals(Line.Uploading(2), m.line)
    }

    @Test fun offlineIsSleepyStill() {
        val m = TallyBrain.decide(ok.copy(offline = true))
        assertEquals(Expression.SLEEPY, m.expression); assertEquals(Action.STILL, m.action); assertEquals(Line.Offline, m.line)
    }

    @Test fun syncErrorIsWorriedShake() {
        val m = TallyBrain.decide(ok.copy(syncError = true))
        assertEquals(Expression.WORRIED, m.expression); assertEquals(Action.SHAKE, m.action); assertEquals(Line.SyncError, m.line)
    }

    @Test fun signedOutIsCuriousStill() {
        val m = TallyBrain.decide(TallyContext(bills = 2, signedIn = false))
        assertEquals(Expression.CURIOUS, m.expression); assertEquals(Action.STILL, m.action); assertEquals(Line.SignInToBackUp, m.line)
    }

    @Test fun warrantiesSoonIsAlertWithBell() {
        val m = TallyBrain.decide(ok.copy(expiringSoon = 2))
        assertEquals(Expression.ALERT, m.expression); assertEquals(Action.BELL_RING, m.action); assertEquals(Line.WarrantiesSoon(2), m.line)
    }

    @Test fun expiredWarrantyIsNeutralStill() {
        val m = TallyBrain.decide(ok.copy(recentlyExpired = "LG TV"))
        assertEquals(Expression.NEUTRAL, m.expression); assertEquals(Action.STILL, m.action); assertEquals(Line.WarrantyEnded("LG TV"), m.line)
    }

    @Test fun searchWithNoResultsShowsMagnifier() {
        val m = TallyBrain.decide(ok.copy(searchNoResults = true))
        assertEquals(Expression.SEARCHING, m.expression); assertEquals(Action.MAGNIFIER_SWEEP, m.action); assertEquals(Line.NoMatch, m.line)
    }

    @Test fun onboardingPages() {
        assertEquals(Expression.HAPPY to Action.WAVE, TallyBrain.onboarding(0).let { it.expression to it.action })
        assertEquals(Expression.PROUD to Action.FILE, TallyBrain.onboarding(1).let { it.expression to it.action })
        assertEquals(Expression.ALERT to Action.BELL, TallyBrain.onboarding(2).let { it.expression to it.action })
    }

    @Test fun everyOneShotIsShortEnough() {
        Action.entries.forEach { assertTrue(it.name, TallyBrain.durationMs(it) <= 700) }
        assertEquals(380, TallyBrain.durationMs(Action.RECEIPT_DROP))
        assertEquals(240, TallyBrain.durationMs(Action.HOP))
        assertEquals(280, TallyBrain.durationMs(Action.SHAKE))
        assertEquals(600, TallyBrain.durationMs(Action.BELL_RING))
    }

    @Test fun savedBeatsEverythingElse() {
        val m = TallyBrain.decide(TallyContext(justSaved = true, syncError = true, offline = true))
        assertEquals(Line.Saved, m.line)
    }

    @Test fun healthyAndQuietHasNoLine() {
        assertEquals(null, TallyBrain.decide(ok).line)
    }
}
