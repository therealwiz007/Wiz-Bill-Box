package com.wizlabs.billbox

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.wizlabs.ads.AdPalette
import com.wizlabs.ads.AdsConfig
import com.wizlabs.ads.WizAds
import com.wizlabs.ads.compose.WizBannerAd
import com.wizlabs.ads.compose.WizNativeAdCard
import com.wizlabs.billbox.ui.theme.Wiz

/**
 * Ads-ready, ad-SDK-free (house rule 7). [ENABLED] is false and the shared `com.wizlabs.ads` copy is the
 * `--no-sdk` build (0.3), so no ad SDK, consent form, request or network call exists. Every slot below renders
 * nothing and takes zero height. Google AdMob comes in a later release, which only re-syncs wiz-ads and flips [ENABLED].
 */
object AdsSetup {
    const val ENABLED = false

    /** Every screen name the app uses. */
    val allScreens = setOf("welcome", "home", "editor", "detail", "viewer", "pdf", "notification", "search", "settings")

    /** Screens where a slot may appear at all. */
    val screens = setOf("home", "settings")

    /** Never any ad here: sign-in, editor, bill detail, photo viewer, PDF hand-off, reminder, search/filter. */
    val noAdScreens = setOf("welcome", "editor", "detail", "viewer", "pdf", "notification", "search")

    enum class Format { NATIVE, BANNER, REWARDED_ROW }
    data class Slot(val id: String, val format: Format, val screen: String)

    /** Handoff 16A: native after the first month group on Home, banner at the end of Settings, rewarded row in Settings. */
    val slots = listOf(
        Slot("home", Format.NATIVE, "home"),
        Slot("settings", Format.BANNER, "settings"),
        Slot("adfree", Format.REWARDED_ROW, "settings"),
    )

    fun install(context: Context) = WizAds.install(
        context,
        AdsConfig(enabled = ENABLED, adScreens = screens, noAdScreens = noAdScreens, debug = BuildConfig.DEBUG),
    )
}

/**
 * One ad slot. With ads off it returns before composing anything (zero height). In debug builds it throws if it is
 * placed on a no-ad screen, so a misplaced slot is caught while developing.
 */
@Composable
fun AdSlot(slot: String, modifier: Modifier = Modifier) {
    val s = AdsSetup.slots.first { it.id == slot }
    if (BuildConfig.DEBUG) check(s.screen !in AdsSetup.noAdScreens) { "ad slot '$slot' placed on no-ad screen '${s.screen}'" }
    if (!AdsSetup.ENABLED) return
    val c = Wiz.c
    when (s.format) {
        AdsSetup.Format.BANNER -> WizBannerAd(s.screen, modifier)
        AdsSetup.Format.NATIVE -> WizNativeAdCard(s.screen, AdPalette(c.surface.toArgb(), c.line.toArgb(), c.text.toArgb(), c.muted.toArgb(), c.accent.toArgb(), c.onAccent.toArgb(), 20), modifier)
        AdsSetup.Format.REWARDED_ROW -> Unit
    }
}
