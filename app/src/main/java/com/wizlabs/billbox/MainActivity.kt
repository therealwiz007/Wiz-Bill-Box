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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wizlabs.billbox.ui.anim.BackPeek
import com.wizlabs.billbox.ui.anim.LocalBackPeek
import com.wizlabs.billbox.ui.anim.backPeek
import com.wizlabs.billbox.ui.anim.LocalReduceMotion
import com.wizlabs.billbox.ui.anim.LocalSharedScope
import com.wizlabs.billbox.ui.anim.ProvideNavScope
import com.wizlabs.billbox.ui.anim.ProvideLoopClock
import com.wizlabs.billbox.ui.anim.pressScale
import com.wizlabs.billbox.ui.anim.rememberHaptics
import com.wizlabs.billbox.ui.art.ReceiptPrint
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.rememberReduceMotion
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
            CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
                WizTheme(data.theme) {
                    val c = Wiz.c
                    LaunchedEffect(c.isDark) {
                        val bar = if (c.isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK)
                        enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
                    }
                    ProvideLoopClock { App(vm) }
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) com.wizlabs.billbox.ui.anim.LoopIdle.onTouch()
        return super.dispatchTouchEvent(ev)
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

/** How deep a screen sits; navigating deeper slides forward, shallower slides back. */
private fun depth(r: String) = when {
    r == "welcome" || r == "home" -> 0
    r.startsWith("detail:") || r == "settings" -> 1
    else -> 2
}

private fun isSheet(r: String) = r == "new" || r.startsWith("edit:")
private fun isHeroPair(a: String, b: String) = (a == "home" && b.startsWith("detail:")) || (b == "home" && a.startsWith("detail:"))

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun AnimatedContentTransitionScope<String>.navTransition(reduce: Boolean): ContentTransform {
    val from = initialState
    val to = targetState
    val t = when {
        reduce -> fadeIn(tween(120)) togetherWith fadeOut(tween(120))
        // The photo/card morphs between list and bill (shared element), so the screens only crossfade.
        isHeroPair(from, to) -> fadeIn(tween(320, delayMillis = 40)) togetherWith fadeOut(tween(220))
        // Editor rises like a sheet and sinks back down.
        isSheet(to) -> (slideInVertically(tween(460, easing = Emphasized)) { it / 3 } + fadeIn(tween(300))) togetherWith
            (fadeOut(tween(220)) + scaleOut(tween(300), targetScale = 0.94f))
        isSheet(from) -> (fadeIn(tween(260)) + scaleIn(tween(360, easing = Emphasized), initialScale = 0.94f)) togetherWith
            (slideOutVertically(tween(340, easing = Emphasized)) { it / 3 } + fadeOut(tween(260)))
        from == "welcome" || to == "welcome" ->
            (fadeIn(tween(420)) + scaleIn(tween(520, easing = Emphasized), initialScale = 1.06f)) togetherWith
                (fadeOut(tween(260)) + scaleOut(tween(420), targetScale = 0.92f))
        depth(to) >= depth(from) -> (slideInHorizontally(tween(420, easing = Emphasized)) { it / 3 } + fadeIn(tween(280))) togetherWith
            (slideOutHorizontally(tween(420, easing = Emphasized)) { -it / 8 } + fadeOut(tween(220)))
        else -> (slideInHorizontally(tween(420, easing = Emphasized)) { -it / 8 } + fadeIn(tween(280))) togetherWith
            (slideOutHorizontally(tween(420, easing = Emphasized)) { it / 3 } + fadeOut(tween(220)))
    }
    return t.using(SizeTransform(clip = false))
}

/** True while the list is at the top or the user is scrolling back up (keeps the FAB label visible). */
@Composable
private fun LazyListState.expandFab(): Boolean {
    var prevIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var prevOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    var up by remember(this) { mutableStateOf(true) }
    val atTop by remember(this) { derivedStateOf { firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset < 40 } }
    LaunchedEffect(firstVisibleItemIndex, firstVisibleItemScrollOffset) {
        up = if (firstVisibleItemIndex != prevIndex) firstVisibleItemIndex < prevIndex else firstVisibleItemScrollOffset <= prevOffset
        prevIndex = firstVisibleItemIndex; prevOffset = firstVisibleItemScrollOffset
    }
    return atTop || up
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun App(vm: AppViewModel) {
    val c = Wiz.c
    val reduce = LocalReduceMotion.current
    val data by vm.data.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val signingIn by vm.signingIn.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var route by rememberSaveable { mutableStateOf("home") }
    var burst by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState() // kept here so the list keeps its scroll position across screens
    val peek = remember { BackPeek() }
    // Once the back transition has played, drop the peek so the next visit starts full size.
    LaunchedEffect(route) { kotlinx.coroutines.delay(650); if (peek.route != null && peek.route != route) peek.reset() }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        vm.onConsentResult(r.resultCode == Activity.RESULT_OK, r.data)
    }
    LaunchedEffect(Unit) {
        vm.consent.collect { pi -> consentLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build()) }
    }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    LaunchedEffect(route) { if (!route.startsWith("detail:")) burst = null }

    val showWelcome = data.account == null && !data.skippedSignIn
    val knownStores = remember(data.bills) { data.bills.map { it.store }.distinct().sorted() }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedScope provides this, LocalBackPeek provides peek) {
                AnimatedContent(
                    if (showWelcome) "welcome" else route,
                    transitionSpec = { navTransition(reduce) },
                    label = "nav",
                ) { r ->
                    ProvideNavScope(this) {
                      Box(Modifier.fillMaxSize().backPeek(r)) {
                        when {
                            r == "welcome" -> WelcomeScreen(signingIn, onSignIn = vm::signIn, onSkip = vm::skipSignIn, onPrivacy = { route = "privacy" })
                            r == "privacy" -> PrivacyScreen { route = "home" }
                            r == "settings" -> SettingsScreen(vm, data, sync, signingIn, onBack = { route = "home" }, onSignIn = vm::signIn, onPrivacy = { route = "privacy" })
                            r == "new" -> BillEditor(vm, null, knownStores) { saved ->
                                if (saved != null) { burst = "Saved to your box"; route = "detail:${saved.id}" } else route = "home"
                            }
                            r.startsWith("edit:") -> {
                                val b = data.bills.find { it.id == r.removePrefix("edit:") }
                                if (b == null) LaunchedEffect(Unit) { route = "home" }
                                else BillEditor(vm, b, knownStores) { saved ->
                                    if (saved != null) burst = "Bill updated"
                                    route = "detail:${b.id}"
                                }
                            }
                            r.startsWith("detail:") -> {
                                val b = data.bills.find { it.id == r.removePrefix("detail:") }
                                if (b == null) LaunchedEffect(Unit) { route = "home" }
                                else BillDetail(vm, b, data.account != null, onBack = { route = "home" }, onEdit = { route = "edit:${b.id}" })
                            }
                            else -> HomeScaffold(vm, data, sync, snack, listState, onRoute = { route = it })
                        }
                      }
                    }
                }
            }
        }
        if (showWelcome || route != "home") {
            SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
        burst?.let { label -> ReceiptPrint(label) { burst = null } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(
    vm: AppViewModel, data: com.wizlabs.billbox.model.AppData, sync: SyncState,
    snack: SnackbarHostState, listState: LazyListState, onRoute: (String) -> Unit,
) {
    val c = Wiz.c
    val expanded = listState.expandFab()
    val haptics = rememberHaptics()
    val fabPress = remember { MutableInteractionSource() }
    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text("Wiz Bill Box", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    val kind = when {
                        sync is SyncState.Running -> 0
                        data.account == null -> 1
                        sync is SyncState.Failed || sync is SyncState.NeedsSignIn -> 2
                        else -> 3
                    }
                    AnimatedContent(
                        kind,
                        transitionSpec = {
                            (scaleIn(androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f), initialScale = 0.4f) + fadeIn()) togetherWith
                                (scaleOut(targetScale = 0.4f) + fadeOut())
                        },
                        label = "cloud",
                    ) { k ->
                        when (k) {
                            0 -> SyncingIcon()
                            1 -> IconButton(onClick = vm::signIn) { Icon(Icons.Rounded.CloudOff, "Sign in", tint = c.warn) }
                            2 -> IconButton(onClick = { vm.requestSync() }) { Icon(Icons.Rounded.ErrorOutline, "Sync problem", tint = c.bad) }
                            else -> IconButton(onClick = { vm.requestSync() }) { Icon(Icons.Rounded.CloudDone, "Synced with Google Drive", tint = c.good) }
                        }
                    }
                    IconButton(onClick = { onRoute("settings") }) { Icon(Icons.Rounded.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text, actionIconContentColor = c.text),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { haptics.tap(); onRoute("new") }, expanded = expanded, interactionSource = fabPress,
                containerColor = c.accent, contentColor = c.onAccent,
                icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Add bill", fontWeight = FontWeight.Bold) },
                modifier = Modifier.enterUp(index = 3, distance = 40.dp).pressScale(fabPress, 0.92f),
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        HomeScreen(vm, data, sync, pad, listState, onOpen = { onRoute("detail:${it.id}") }, onSignIn = vm::signIn)
    }
}
