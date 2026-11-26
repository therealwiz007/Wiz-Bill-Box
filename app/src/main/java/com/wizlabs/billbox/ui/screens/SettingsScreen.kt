@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.wizlabs.billbox.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import com.wizlabs.billbox.AdSlot
import com.wizlabs.billbox.AdsSetup
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.BuildConfigInfo
import com.wizlabs.billbox.R
import com.wizlabs.billbox.SyncState
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.engine.TallyBrain
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.ThemeMode
import com.wizlabs.billbox.ui.Icons
import com.wizlabs.billbox.ui.anim.WizBackHandler
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.rememberHaptics
import com.wizlabs.billbox.ui.components.IconBadge
import com.wizlabs.billbox.ui.components.Overline
import com.wizlabs.billbox.ui.components.PrimaryButton
import com.wizlabs.billbox.ui.components.SmallChip
import com.wizlabs.billbox.ui.components.WizCard
import com.wizlabs.billbox.ui.mascot.Tally
import com.wizlabs.billbox.ui.mascot.tallyContext
import com.wizlabs.billbox.ui.theme.Numeric
import com.wizlabs.billbox.ui.theme.Wiz
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
private fun agoText(millis: Long): String {
    val s = (System.currentTimeMillis() - millis) / 1000
    return when {
        s < 60 -> stringResource(R.string.ago_now)
        s < 3600 -> stringResource(R.string.ago_min, (s / 60).toInt())
        s < 86400 -> stringResource(R.string.ago_h, (s / 3600).toInt())
        else -> stringResource(R.string.ago_d, (s / 86400).toInt())
    }
}

@Composable
fun SettingsScreen(
    vm: AppViewModel, data: AppData, sync: SyncState, signingIn: Boolean,
    onBack: () -> Unit, onSignIn: () -> Unit, onPrivacy: () -> Unit,
) {
    val c = Wiz.c
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmSignOut by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    WizBackHandler("settings", onBack = onBack)
    val exported = stringResource(R.string.export_done)
    val exportFailed = stringResource(R.string.export_failed)
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { vm.toast(if (vm.exportCsv(uri)) exported else exportFailed) }
    }
    var canNotify by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text, navigationIconContentColor = c.text),
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- Account ----
            Overline(stringResource(R.string.sec_account))
            WizCard(Modifier.enterUp(1)) {
                val acc = data.account
                if (acc == null) {
                    Text(stringResource(R.string.not_signed_in), style = MaterialTheme.typography.titleMedium, color = c.text)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.not_signed_in_body), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(stringResource(R.string.sign_in_google), onSignIn, Modifier.fillMaxWidth(), loading = signingIn)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.AccountCircle, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            if (acc.name.isNotBlank()) Text(acc.name, style = MaterialTheme.typography.titleSmall, color = c.text)
                            Text(acc.email, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.sign_out_switch), color = c.bad)
                    }
                }
            }

            // ---- Backup ----
            if (data.account != null) {
                Overline(stringResource(R.string.sec_backup), Modifier.padding(top = 8.dp))
                WizCard(Modifier.enterUp(2)) {
                    val pending = data.bills.count { it.dirty }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Tally(TallyBrain.decide(tallyContext(data, sync)), 48.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            val head = when (sync) {
                                SyncState.Running -> stringResource(R.string.syncing)
                                is SyncState.Failed -> stringResource(R.string.last_sync_failed)
                                SyncState.NeedsSignIn -> stringResource(R.string.sign_in_again)
                                else -> data.lastSyncAt?.let { stringResource(R.string.synced_ago, agoText(it)) } ?: stringResource(R.string.not_synced)
                            }
                            AnimatedContent(head, transitionSpec = { fadeIn(tween(200, 40)) togetherWith fadeOut(tween(120)) }, label = "syncHead") { h ->
                                Text(h, style = MaterialTheme.typography.titleSmall, color = c.text)
                            }
                            Text(
                                if (sync is SyncState.Failed) sync.message
                                else if (pending > 0) pluralStringResource(R.plurals.waiting_upload, pending, pending)
                                else pluralStringResource(R.plurals.bills_in_drive, data.bills.size, data.bills.size),
                                style = MaterialTheme.typography.bodySmall, color = c.muted,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { haptics.tap(); if (sync is SyncState.NeedsSignIn) onSignIn() else vm.requestSync() }, enabled = sync != SyncState.Running,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.sync_now), color = c.accentInk, fontWeight = FontWeight.Bold) }
                        data.rootFolderId?.let { root ->
                            OutlinedButton(
                                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/folders/$root"))) } },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) {
                                Icon(Icons.Rounded.FolderOpen, null, tint = c.accentInk, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.open_folder), color = c.text, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // ---- Reminders (N3) ----
            Overline(stringResource(R.string.sec_reminders), Modifier.padding(top = 8.dp))
            WizCard(Modifier.enterUp(3)) {
                SwitchRow(stringResource(R.string.remind_master), data.remindersOn) { vm.setReminders(on = it) }
                if (data.remindersOn) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = c.line)
                    SwitchRow(stringResource(R.string.remind_30), data.remind30) { vm.setReminders(r30 = it) }
                    SwitchRow(stringResource(R.string.remind_7), data.remind7) { vm.setReminders(r7 = it) }
                    SwitchRow(stringResource(R.string.remind_1), data.remind1) { vm.setReminders(r1 = it) }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.remind_time), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = c.text)
                        Text("10:00", style = MaterialTheme.typography.bodyLarge.merge(Numeric), color = c.muted)
                    }
                    if (!canNotify) {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.notif_blocked), style = MaterialTheme.typography.bodySmall, color = c.warn)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                                runCatching {
                                    if (Build.VERSION.SDK_INT < 33 || !NotificationManagerCompat.from(context).areNotificationsEnabled())
                                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.notif_allow), color = c.accentInk) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.remind_note), style = MaterialTheme.typography.labelMedium, color = c.muted)
                }
            }

            // ---- Warranty window ----
            Overline(stringResource(R.string.sec_window), Modifier.padding(top = 8.dp))
            WizCard(Modifier.enterUp(4)) {
                var w by remember(data.warrantyReminderDays) { mutableFloatStateOf(data.warrantyReminderDays.toFloat()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.window_body), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.text)
                    Text(stringResource(R.string.n_days, w.toInt()), style = MaterialTheme.typography.titleSmall.merge(Numeric), color = c.accentInk)
                }
                Slider(
                    value = w, onValueChange = { w = it }, valueRange = 7f..90f,
                    onValueChangeFinished = { vm.setReminderDays(w.toInt()) },
                    colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.line),
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }

            // ---- Export (N6) ----
            Overline(stringResource(R.string.sec_export), Modifier.padding(top = 8.dp))
            WizCard(Modifier.enterUp(5), onClick = {
                if (data.bills.isEmpty()) vm.toast(context.getString(R.string.export_empty)) else csv.launch("wiz-bill-box-${LocalDate.now()}.csv")
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Download, null, tint = c.accentInk)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.export_csv), style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text(stringResource(R.string.export_body), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }

            // ---- Appearance ----
            Overline(stringResource(R.string.sec_appearance), Modifier.padding(top = 8.dp))
            WizCard(Modifier.enterUp(6)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallChip(stringResource(R.string.theme_system), data.theme == ThemeMode.SYSTEM, { vm.setTheme(ThemeMode.SYSTEM) })
                    SmallChip(stringResource(R.string.theme_light), data.theme == ThemeMode.LIGHT, { vm.setTheme(ThemeMode.LIGHT) })
                    SmallChip(stringResource(R.string.theme_dark), data.theme == ThemeMode.DARK, { vm.setTheme(ThemeMode.DARK) })
                }
            }

            // ---- About ----
            Overline(stringResource(R.string.sec_about), Modifier.padding(top = 8.dp))
            WizCard(Modifier.enterUp(7), onClick = onPrivacy) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 32.dp)) {
                    Icon(Icons.Rounded.Policy, null, tint = c.accentInk)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                }
            }
            Column(Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                Text(Wordmark(18f), maxLines = 1)
                Text(stringResource(R.string.about_line, BuildConfigInfo.versionName(context)), style = MaterialTheme.typography.labelMedium.merge(Numeric), color = c.muted)
            }
            // Ad slots (ads OFF: both render nothing and take no space).
            if (AdsSetup.ENABLED) { AdSlot("settings"); AdSlot("adfree") }
            Spacer(Modifier.height(16.dp))
        }
        }
    }

    if (confirmSignOut) {
        val pending = data.bills.count { it.dirty }
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.signout_title)) },
            text = { Text(if (pending > 0) pluralStringResource(R.plurals.signout_pending, pending, pending) else stringResource(R.string.signout_body)) },
            confirmButton = { TextButton(onClick = { haptics.reject(); confirmSignOut = false; vm.signOut() }) { Text(stringResource(R.string.sign_out), color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Wiz.c
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = c.text)
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.line, uncheckedThumbColor = c.muted))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val c = Wiz.c
    WizBackHandler("privacy", onBack = onBack)
    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_policy)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text, navigationIconContentColor = c.text),
            )
        },
    ) { pad ->
        AndroidView(
            factory = { ctx -> WebView(ctx).apply { settings.javaScriptEnabled = false; loadUrl("file:///android_asset/privacy-policy.html") } },
            modifier = Modifier.fillMaxSize().padding(pad),
        )
    }
}
