package com.wizlabs.billbox

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wizlabs.billbox.engine.Action
import com.wizlabs.billbox.engine.Expression
import com.wizlabs.billbox.engine.Line
import com.wizlabs.billbox.engine.Mood
import com.wizlabs.billbox.reminders.WarrantyAlarms
import com.wizlabs.billbox.ui.anim.BackPeek
import com.wizlabs.billbox.ui.anim.LocalBackPeek
import com.wizlabs.billbox.ui.anim.LocalReduceMotion
import com.wizlabs.billbox.ui.anim.LocalSharedScope
import com.wizlabs.billbox.ui.anim.ProvideLoopClock
import com.wizlabs.billbox.ui.anim.ProvideNavScope
import com.wizlabs.billbox.ui.anim.backPeek
import com.wizlabs.billbox.ui.anim.rememberReduceMotion
import com.wizlabs.billbox.ui.components.EmptyState
import com.wizlabs.billbox.ui.mascot.Tally
import com.wizlabs.billbox.ui.screens.BillDetail
import com.wizlabs.billbox.ui.screens.BillEditor
import com.wizlabs.billbox.ui.screens.HomeScaffold
import com.wizlabs.billbox.ui.screens.PrivacyScreen
import com.wizlabs.billbox.ui.screens.SettingsScreen
import com.wizlabs.billbox.ui.screens.WelcomeScreen
import com.wizlabs.billbox.ui.theme.Wiz
import com.wizlabs.billbox.ui.theme.WizTheme
import com.wizlabs.update.WizUpdate
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var update: WizUpdate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val data by vm.data.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
                WizTheme(data.theme) {
                    val c = Wiz.c
                    LaunchedEffect(c.isDark) {
                        BillUi.dark = c.isDark
                        val bar = if (c.isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK)
                        enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
                    }
                    ProvideLoopClock { App(vm) }
                }
            }
        }
        AdsSetup.install(applicationContext)
        update = createWizUpdate(this)
        openFrom(intent)
        window.decorView.post { reportFullyDrawn() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFrom(intent)
    }

    /** A reminder notification opens its bill. */
    private fun openFrom(i: Intent?) {
        i?.getStringExtra(WarrantyAlarms.EXTRA_BILL)?.let { vm.requestOpen(it); i.removeExtra(WarrantyAlarms.EXTRA_BILL) }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) com.wizlabs.billbox.ui.anim.LoopIdle.onTouch()
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        vm.requestSync(delayMs = 300)
        update?.onResume()
    }

    override fun onPause() { update?.onPause(); super.onPause() }
    override fun onDestroy() { update?.onDestroy(); update = null; super.onDestroy() }
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

/** Wiz motion: slide 12 % of the width + fade, 320 ms in / 200 ms out, outgoing screen at 0.97. */
private fun AnimatedContentTransitionScope<String>.navTransition(reduce: Boolean): ContentTransform {
    val from = initialState
    val to = targetState
    val t = when {
        reduce -> fadeIn(tween(120)) togetherWith fadeOut(tween(120))
        isHeroPair(from, to) -> fadeIn(tween(300, delayMillis = 40)) togetherWith fadeOut(tween(200))
        isSheet(to) -> (slideInVertically(tween(320, easing = Emphasized)) { it / 4 } + fadeIn(tween(300))) togetherWith
            (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.97f))
        isSheet(from) -> (fadeIn(tween(260)) + scaleIn(tween(320, easing = Emphasized), initialScale = 0.97f)) togetherWith
            (slideOutVertically(tween(200, easing = Emphasized)) { it / 4 } + fadeOut(tween(200)))
        from == "welcome" || to == "welcome" -> fadeIn(tween(320)) togetherWith (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.97f))
        depth(to) >= depth(from) -> (slideInHorizontally(tween(320, easing = Emphasized)) { it * 12 / 100 } + fadeIn(tween(320))) togetherWith
            (slideOutHorizontally(tween(200, easing = Emphasized)) { -it * 12 / 100 } + fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.97f))
        else -> (slideInHorizontally(tween(320, easing = Emphasized)) { -it * 12 / 100 } + fadeIn(tween(320))) togetherWith
            (slideOutHorizontally(tween(200, easing = Emphasized)) { it * 12 / 100 } + fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.97f))
    }
    return t.using(SizeTransform(clip = false))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun App(vm: AppViewModel) {
    val c = Wiz.c
    val reduce = LocalReduceMotion.current
    val data by vm.data.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val signingIn by vm.signingIn.collectAsStateWithLifecycle()
    val signInError by vm.signInError.collectAsStateWithLifecycle()
    val openBill by vm.openBill.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var route by rememberSaveable { mutableStateOf("home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<Boolean?>(null) }
    val listState = rememberLazyListState() // kept here so the list keeps its scroll position across screens
    val peek = remember { BackPeek() }
    val wide = LocalConfiguration.current.screenWidthDp >= 840
    // Once the back transition has played, drop the peek so the next visit starts full size.
    LaunchedEffect(route) { delay(650); if (peek.route != null && peek.route != route) peek.reset() }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        vm.onConsentResult(r.resultCode == Activity.RESULT_OK, r.data)
    }
    LaunchedEffect(Unit) {
        vm.consent.collect { pi -> consentLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build()) }
    }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val showWelcome = data.account == null && !data.skippedSignIn
    // A reminder notification opens its bill.
    LaunchedEffect(openBill, data.bills) {
        val id = openBill ?: return@LaunchedEffect
        if (data.bills.any { it.id == id }) { if (wide) { selectedId = id; route = "home" } else route = "detail:$id"; vm.openHandled() }
    }
    // A narrow route carried over to a wide window becomes a selection.
    LaunchedEffect(wide, route) {
        if (wide && route.startsWith("detail:")) { selectedId = route.removePrefix("detail:"); route = "home" }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2400); toast = null } }
    BillUi.busy = showWelcome || route == "new" || route.startsWith("edit:")

    val knownStores = remember(data.bills) { data.bills.map { it.store }.distinct().sorted() }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedScope provides (if (wide) null else this), LocalBackPeek provides peek) {
                AnimatedContent(
                    if (showWelcome) "welcome" else route,
                    transitionSpec = { navTransition(reduce) },
                    label = "nav",
                ) { r ->
                    ProvideNavScope(this) {
                        Box(Modifier.fillMaxSize().backPeek(r)) {
                            when {
                                r == "welcome" -> WelcomeScreen(signingIn, signInError, onSignIn = vm::signIn, onSkip = vm::skipSignIn, onPrivacy = { route = "privacy" })
                                r == "privacy" -> PrivacyScreen { route = "home" }
                                r == "settings" -> SettingsScreen(vm, data, sync, signingIn, onBack = { route = "home" }, onSignIn = vm::signIn, onPrivacy = { route = "privacy" })
                                r == "new" -> BillEditor(vm, null, knownStores, data) { saved ->
                                    if (saved != null) { toast = true; if (wide) { selectedId = saved.id; route = "home" } else route = "detail:${saved.id}" } else route = "home"
                                }
                                r.startsWith("edit:") -> {
                                    val b = data.bills.find { it.id == r.removePrefix("edit:") }
                                    if (b == null) LaunchedEffect(Unit) { route = "home" }
                                    else BillEditor(vm, b, knownStores, data) { saved ->
                                        if (saved != null) toast = true
                                        if (wide) route = "home" else route = "detail:${b.id}"
                                    }
                                }
                                r.startsWith("detail:") -> {
                                    val b = data.bills.find { it.id == r.removePrefix("detail:") }
                                    if (b == null) LaunchedEffect(Unit) { route = "home" }
                                    else BillDetail(vm, b, data, onBack = { route = "home" }, onEdit = { route = "edit:${b.id}" })
                                }
                                wide -> TwoPane(vm, data, sync, listState, selectedId, onSelect = { selectedId = it }, onRoute = { route = it })
                                else -> HomeScaffold(vm, data, sync, listState, onOpen = { route = "detail:${it.id}" }, onAdd = { route = "new" }, onSettings = { route = "settings" })
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (route == "home" && !showWelcome) 88.dp else 0.dp))
        toast?.let { SaveToast(signedIn = data.account != null, Modifier.align(Alignment.BottomCenter)) }
    }
}

/** Tablet: list on the left (40 %), the selected bill on the right. */
@Composable
private fun TwoPane(
    vm: AppViewModel, data: com.wizlabs.billbox.model.AppData, sync: SyncState, listState: androidx.compose.foundation.lazy.LazyListState,
    selectedId: String?, onSelect: (String?) -> Unit, onRoute: (String) -> Unit,
) {
    val c = Wiz.c
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(0.4f).fillMaxSize()) {
            HomeScaffold(vm, data, sync, listState, selectedId, onOpen = { onSelect(it.id) }, onAdd = { onRoute("new") }, onSettings = { onRoute("settings") })
        }
        Box(Modifier.weight(0.6f).fillMaxSize().border(1.dp, c.line)) {
            val b = data.bills.find { it.id == selectedId }
            if (b != null) BillDetail(vm, b, data, onBack = { onSelect(null) }, onEdit = { onRoute("edit:${b.id}") }, showBack = false)
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Mood(Expression.HAPPY, Action.STILL, null), stringResource(R.string.pick_bill_title), stringResource(R.string.pick_bill_body),
                )
            }
        }
    }
}

/** Tally's save confirmation: a card at the bottom for 2.4 s with the receipt drop. */
@Composable
private fun SaveToast(signedIn: Boolean, modifier: Modifier = Modifier) {
    val c = Wiz.c
    Row(
        modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 96.dp).fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(18.dp)).padding(10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tally(Mood(Expression.PROUD, Action.RECEIPT_DROP, Line.Saved), 48.dp)
        Text(stringResource(if (signedIn) R.string.toast_saved_drive else R.string.toast_saved_local), color = c.text, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
    }
}
