package com.wizlabs.billbox.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.BuildConfigInfo
import com.wizlabs.billbox.SyncState
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.ThemeMode
import com.wizlabs.billbox.ui.components.IconBadge
import com.wizlabs.billbox.ui.components.Overline
import com.wizlabs.billbox.ui.components.WizCard
import com.wizlabs.billbox.ui.theme.Wiz

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    vm: AppViewModel, data: AppData, sync: SyncState, signingIn: Boolean,
    onBack: () -> Unit, onSignIn: () -> Unit, onPrivacy: () -> Unit,
) {
    val c = Wiz.c
    val context = LocalContext.current
    var confirmSignOut by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text, navigationIconContentColor = c.text),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Overline("Google Drive")
            WizCard {
                val acc = data.account
                if (acc == null) {
                    Text("Not signed in", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text("Bills are saved on this phone only. Sign in to store them in your Google Drive and restore them on any phone.",
                        style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onSignIn, enabled = !signingIn, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                    ) { Text(if (signingIn) "Connecting…" else "Sign in with Google", fontWeight = FontWeight.Bold) }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.AccountCircle, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            if (acc.name.isNotBlank()) Text(acc.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(acc.email, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = c.line)
                    val pending = data.bills.count { it.dirty }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CloudSync, null, tint = c.accentInk)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (sync) {
                                    SyncState.Running -> "Syncing…"
                                    is SyncState.Failed -> "Last sync failed"
                                    SyncState.NeedsSignIn -> "Sign in again to sync"
                                    else -> data.lastSyncAt?.let { "Synced ${Fmt.ago(it)}" } ?: "Not synced yet"
                                },
                                style = MaterialTheme.typography.titleSmall, color = c.text,
                            )
                            Text(
                                if (sync is SyncState.Failed) sync.message
                                else if (pending > 0) "$pending bill${if (pending == 1) "" else "s"} waiting to upload"
                                else "${data.bills.size} bills in Drive › Wiz Bill Box",
                                style = MaterialTheme.typography.bodySmall, color = c.muted,
                            )
                        }
                        TextButton(onClick = { if (sync is SyncState.NeedsSignIn) onSignIn() else vm.requestSync() }, enabled = sync != SyncState.Running) {
                            Text("Sync now", color = c.accentInk, fontWeight = FontWeight.Bold)
                        }
                    }
                    data.rootFolderId?.let { root ->
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/folders/$root"))) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.FolderOpen, null, tint = c.accentInk)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Wiz Bill Box folder in Drive", color = c.text)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign out / switch account", color = c.bad)
                    }
                }
            }

            Overline("Warranty reminder", Modifier.padding(top = 8.dp))
            WizCard {
                Text("Show “ending soon” for warranties ending within", style = MaterialTheme.typography.bodyMedium, color = c.text)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 15, 30, 60, 90).forEach { d -> SmallChip("$d days", data.warrantyReminderDays == d) { vm.setReminderDays(d) } }
                }
            }

            Overline("Appearance", Modifier.padding(top = 8.dp))
            WizCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallChip("System", data.theme == ThemeMode.SYSTEM) { vm.setTheme(ThemeMode.SYSTEM) }
                    SmallChip("Dark", data.theme == ThemeMode.DARK) { vm.setTheme(ThemeMode.DARK) }
                    SmallChip("Light", data.theme == ThemeMode.LIGHT) { vm.setTheme(ThemeMode.LIGHT) }
                }
            }

            Overline("About", Modifier.padding(top = 8.dp))
            WizCard(onClick = onPrivacy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Policy, null, tint = c.accentInk)
                    Spacer(Modifier.width(10.dp))
                    Text("Privacy policy", style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                }
            }
            Text("Wiz Bill Box ${BuildConfigInfo.versionName(context)} · Wiz Labs", style = MaterialTheme.typography.labelSmall, color = c.muted,
                modifier = Modifier.padding(start = 4.dp, bottom = 24.dp))
        }
    }

    if (confirmSignOut) {
        val pending = data.bills.count { it.dirty }
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    if (pending > 0) "$pending bill${if (pending == 1) " hasn't" else "s haven't"} uploaded yet and will be lost from this phone. Sync first if you can.\n\nBills already in Google Drive stay there; sign in again to get them back."
                    else "Bills are removed from this phone but stay in your Google Drive. Sign in again (with any phone) to get them back."
                )
            },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; vm.signOut() }) { Text("Sign out", color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val c = Wiz.c
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text("Privacy policy") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
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
