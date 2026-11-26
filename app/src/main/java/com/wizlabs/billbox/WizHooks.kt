package com.wizlabs.billbox

import android.app.Activity
import androidx.compose.ui.graphics.toArgb
import com.wizlabs.billbox.ui.theme.DarkPalette
import com.wizlabs.billbox.ui.theme.LightPalette
import com.wizlabs.update.UpdatePalette
import com.wizlabs.update.WizUpdate

/** Screen state read outside Compose (set from the app composable). */
internal object BillUi {
    /** True while onboarding or the editor is showing: no update popup then. */
    @Volatile var busy = false
    @Volatile var dark = false
}

/** Non-blocking Play update popup (flexible, always dismissible), coloured from this app's tokens. */
internal fun createWizUpdate(activity: Activity): WizUpdate = WizUpdate(activity, object : WizUpdate.Host {
    override fun palette(): UpdatePalette {
        val p = if (BillUi.dark) DarkPalette else LightPalette
        return UpdatePalette(p.surface.toArgb(), p.line.toArgb(), p.text.toArgb(), p.muted.toArgb(), p.accent.toArgb(), p.onAccent.toArgb(), 18)
    }
    override fun mayPromptNow() = !BillUi.busy
    override fun appName() = activity.getString(R.string.app_full_name)
    override fun restartNote(): String? = null
}, BuildConfig.DEBUG)
