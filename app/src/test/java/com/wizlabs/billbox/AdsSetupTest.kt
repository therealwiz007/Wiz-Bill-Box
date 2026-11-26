package com.wizlabs.billbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsSetupTest {
    @Test fun adsAreOff() = assertFalse(AdsSetup.ENABLED)
    @Test fun noSlotOnANoAdScreen() = AdsSetup.slots.forEach { assertFalse(it.id, it.screen in AdsSetup.noAdScreens) }
    @Test fun everyNoAdScreenExists() = assertTrue(AdsSetup.allScreens.containsAll(AdsSetup.noAdScreens))
    @Test fun slotScreensAreAdScreens() = AdsSetup.slots.forEach { assertTrue(it.screen in AdsSetup.screens) }
    @Test fun editorDetailViewerWelcomeSearchAreNoAd() =
        assertTrue(AdsSetup.noAdScreens.containsAll(setOf("welcome", "editor", "detail", "viewer", "pdf", "notification", "search")))
    @Test fun slotsMatchHandoff() = assertEquals(listOf("home", "settings", "adfree"), AdsSetup.slots.map { it.id })
    @Test fun noAdSdkIsLinked() = assertFalse(com.wizlabs.ads.WIZ_ADS_SDK_LINKED)
}
