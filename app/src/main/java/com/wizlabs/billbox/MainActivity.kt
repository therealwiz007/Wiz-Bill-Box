package com.wizlabs.billbox

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wizlabs.billbox.ui.components.SyncingIcon
import com.wizlabs.billbox.ui.screens.BillDetail
import com.wizlabs.billbox.ui.screens.BillEditor
import com.wizlabs.billbox.ui.screens.HomeScreen
import com.wizlabs.billbox.ui.screens.PrivacyScreen
import com.wizlabs.billbox.ui.screens.SettingsScreen
import com.wizlabs.billbox.ui.screens.WelcomeScreen
import com.wizlabs.billbox.ui.theme.Wiz
import com.wizlabs.billbox.ui.theme.WizTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val data by vm.data.collectAsStateWithLifecycle()
            WizTheme(data.theme) {
                val c = Wiz.c
                LaunchedEffect(c.isDark) {
                    val bar = if (c.isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, c.text.toArgb())
                    enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
                }
                App(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.requestSync(delayMs = 300)
    }
}

object BuildConfigInfo {
    fun versionName(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: AppViewModel) {
    val c = Wiz.c
    val data by vm.data.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val signingIn by vm.signingIn.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var route by rememberSaveable { mutableStateOf("home") }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        vm.onConsentResult(r.resultCode == Activity.RESULT_OK, r.data)
    }
    LaunchedEffect(Unit) {
        vm.consent.collect { pi -> consentLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build()) }
    }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val showWelcome = data.account == null && !data.skippedSignIn
    val knownStores = remember(data.bills) { data.bills.map { it.store }.distinct().sorted() }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        AnimatedContent(if (showWelcome) "welcome" else route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { r ->
            when {
                r == "welcome" -> WelcomeScreen(signingIn, onSignIn = vm::signIn, onSkip = vm::skipSignIn, onPrivacy = { route = "privacy" })
                r == "privacy" -> PrivacyScreen { route = "home" }
                r == "settings" -> SettingsScreen(vm, data, sync, signingIn, onBack = { route = "home" }, onSignIn = vm::signIn, onPrivacy = { route = "privacy" })
                r == "new" -> BillEditor(vm, null, knownStores) { saved -> route = if (saved != null) "detail:${saved.id}" else "home" }
                r.startsWith("edit:") -> {
                    val b = data.bills.find { it.id == r.removePrefix("edit:") }
                    if (b == null) LaunchedEffect(Unit) { route = "home" }
                    else BillEditor(vm, b, knownStores) { route = "detail:${b.id}" }
                }
                r.startsWith("detail:") -> {
                    val b = data.bills.find { it.id == r.removePrefix("detail:") }
                    if (b == null) LaunchedEffect(Unit) { route = "home" }
                    else BillDetail(vm, b, data.account != null, onBack = { route = "home" }, onEdit = { route = "edit:${b.id}" })
                }
                else -> Scaffold(
                    containerColor = c.bg,
                    topBar = {
                        TopAppBar(
                            title = { Text("Wiz Bill Box", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) },
                            actions = {
                                when {
                                    sync is SyncState.Running -> SyncingIcon()
                                    data.account == null -> IconButton(onClick = vm::signIn) { Icon(Icons.Rounded.CloudOff, "Sign in", tint = c.warn) }
                                    else -> IconButton(onClick = { vm.requestSync() }) {
                                        Icon(Icons.Rounded.CloudDone, "Synced with Google Drive", tint = if (sync is SyncState.Failed) c.bad else c.good)
                                    }
                                }
                                IconButton(onClick = { route = "settings" }) { Icon(Icons.Rounded.Settings, "Settings") }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text, actionIconContentColor = c.text),
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = { route = "new" }, containerColor = c.accent, contentColor = c.onAccent,
                            icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Add bill", fontWeight = FontWeight.Bold) },
                        )
                    },
                    snackbarHost = { SnackbarHost(snack) },
                ) { pad ->
                    HomeScreen(vm, data, sync, pad, onOpen = { route = "detail:${it.id}" }, onSignIn = vm::signIn)
                }
            }
        }
        if (showWelcome || route != "home") {
            SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }
}
