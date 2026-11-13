package com.wizlabs.billbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.wizlabs.billbox.ui.anim.pressScale
import com.wizlabs.billbox.ui.anim.rememberHaptics
import com.wizlabs.billbox.ui.art.EmptyTrayArt
import com.wizlabs.billbox.ui.art.VaultOrbit
import com.wizlabs.billbox.ui.art.VaultState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.wizlabs.billbox.ui.anim.CountUpText
import com.wizlabs.billbox.ui.anim.LocalReduceMotion
import com.wizlabs.billbox.ui.anim.Motion
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.sharedBoundsHero
import com.wizlabs.billbox.ui.anim.sharedHero
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.SyncState
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.model.SortBy
import com.wizlabs.billbox.ui.components.BillImage
import com.wizlabs.billbox.ui.components.CloudMark
import com.wizlabs.billbox.ui.components.EmptyBox
import com.wizlabs.billbox.ui.components.IconBadge
import com.wizlabs.billbox.ui.components.Overline
import com.wizlabs.billbox.ui.components.Pill
import com.wizlabs.billbox.ui.components.WizCard
import com.wizlabs.billbox.ui.components.icon
import com.wizlabs.billbox.ui.theme.Numeric
import com.wizlabs.billbox.ui.theme.Wiz
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Filters and sorts bills for the list. Kept free of UI so it can be unit-tested. */
/** Remembers, for this app launch, that the home intro already played and which rows were shown. */
private object HomeIntro {
    var played = false
    val seen = mutableSetOf<String>()
}

object BillQuery {
    fun apply(bills: List<Bill>, query: String, category: Category?, sort: SortBy): List<Bill> {
        val q = query.trim().lowercase()
        val filtered = bills.filter { b ->
            (category == null || b.category == category) &&
                (q.isEmpty() || listOf(b.store, b.product, b.invoiceNumber, b.notes, b.gstin, b.category?.label.orEmpty())
                    .any { it.lowercase().contains(q) })
        }
        return when (sort) {
            SortBy.DATE -> filtered.sortedWith(compareByDescending<Bill> { it.invoiceDate }.thenByDescending { it.createdAt })
            SortBy.ADDED -> filtered.sortedByDescending { it.createdAt }
            SortBy.AMOUNT -> filtered.sortedByDescending { it.amount ?: -1.0 }
            SortBy.STORE -> filtered.sortedWith(compareBy<Bill> { it.store.lowercase() }.thenByDescending { it.invoiceDate })
        }
    }

    fun warrantySoon(bills: List<Bill>, days: Int, today: LocalDate = LocalDate.now()): List<Bill> =
        bills.filter { b -> b.warrantyUntil?.let { !it.isBefore(today) && ChronoUnit.DAYS.between(today, it) <= days } == true }
            .sortedBy { it.warrantyUntil }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel, data: AppData, sync: SyncState, contentPadding: PaddingValues, listState: LazyListState,
    onOpen: (Bill) -> Unit, onSignIn: () -> Unit,
) {
    val c = Wiz.c
    // The intro (cards rising in one after another) plays once per app launch, not on every return.
    val intro = remember { !HomeIntro.played }
    LaunchedEffect(Unit) { HomeIntro.played = true }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortBy.DATE) }
    var sortMenu by remember { mutableStateOf(false) }

    val list = remember(data.bills, query, category, sort) { BillQuery.apply(data.bills, query, category, sort) }
    val usedCats = remember(data.bills) { data.bills.mapNotNull { it.category }.distinct().sortedBy { it.ordinal } }
    val soon = remember(data.bills, data.warrantyReminderDays) { BillQuery.warrantySoon(data.bills, data.warrantyReminderDays) }
    val signedIn = data.account != null

    PullToRefreshBox(
        isRefreshing = sync is SyncState.Running,
        onRefresh = { if (signedIn) vm.requestSync() else onSignIn() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "hero") {
                VaultHero(data, sync, soon.size, Modifier.enterUp(0, intro), onSignIn = onSignIn, onSync = { vm.requestSync() })
            }

            if (data.bills.isNotEmpty()) {
                item(key = "search") {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().enterUp(1, intro),
                        placeholder = { Text("Search store, product, invoice no.") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                        },
                        singleLine = true, shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = c.surface, focusedContainerColor = c.surface,
                            unfocusedBorderColor = c.line, focusedBorderColor = c.accentInk,
                        ),
                    )
                }
                item(key = "chips") {
                    Row(Modifier.enterUp(2, intro), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Chip("All", category == null) { category = null } }
                            items(usedCats) { cat -> Chip(cat.label, category == cat) { category = if (category == cat) null else cat } }
                        }
                        Box {
                            IconButton(onClick = { sortMenu = true }) { Icon(Icons.Rounded.Sort, "Sort", tint = c.muted) }
                            DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                                SortBy.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.label, fontWeight = if (s == sort) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sort = s; sortMenu = false },
                                    )
                                }
                            }
                        }
                    }
                }
                if (soon.isNotEmpty() && query.isEmpty() && category == null) {
                    item(key = "warranty") { WarrantyCard(vm, soon, onOpen, Modifier.animateItem().enterUp(3, intro)) }
                }
            }

            if (data.bills.isEmpty()) {
                item {
                    EmptyBox(
                        Icons.Rounded.ReceiptLong, "No bills yet",
                        if (sync is SyncState.Running) "Checking Google Drive for your bills…"
                        else "Tap “Add bill” to photograph your first receipt or invoice.",
                        Modifier.padding(top = 8.dp), art = { EmptyTrayArt() },
                    )
                }
            } else if (list.isEmpty()) {
                item { EmptyBox(Icons.Rounded.SearchOff, "Nothing matches", "Try another word or clear the filter.") }
            } else {
                var lastHeader: String? = null
                var rowIndex = 0
                list.forEach { b ->
                    val header = if (sort == SortBy.DATE) Fmt.month(b.invoiceDate) else null
                    if (header != null && header != lastHeader) {
                        lastHeader = header
                        item(key = "h-$header") { Overline(header, Modifier.animateItem().padding(top = 10.dp, start = 4.dp)) }
                    }
                    val idx = rowIndex++
                    item(key = b.id) {
                        BillRow(vm, b, signedIn, Modifier.animateItem(fadeInSpec = tween(250), placementSpec = Motion.smooth(), fadeOutSpec = tween(200))
                            .enterUp(4 + idx, intro || b.id !in HomeIntro.seen)) { onOpen(b) }
                        LaunchedEffect(b.id) { HomeIntro.seen += b.id }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) = SmallChip(label, selected, onClick)

private fun vaultState(data: AppData, sync: SyncState): VaultState = when {
    data.account == null -> VaultState.LOCAL
    sync is SyncState.Running -> VaultState.SYNCING
    sync is SyncState.Failed -> VaultState.FAILED
    sync is SyncState.NeedsSignIn || data.bills.any { it.dirty } -> VaultState.PENDING
    else -> VaultState.SYNCED
}

/** The home hero: the animated bill vault plus what it means in words, and the one action that helps. */
@Composable
private fun VaultHero(data: AppData, sync: SyncState, soonCount: Int, modifier: Modifier = Modifier, onSignIn: () -> Unit, onSync: () -> Unit) {
    val c = Wiz.c
    val haptics = rememberHaptics()
    val state = vaultState(data, sync)
    val pending = data.bills.count { it.dirty }
    val n = data.bills.size
    fun bills(k: Int) = "$k bill${if (k == 1) "" else "s"}"
    val tint by animateColorAsState(
        when (state) {
            VaultState.SYNCED -> c.good; VaultState.SYNCING, VaultState.INTRO -> c.accentInk
            VaultState.PENDING, VaultState.LOCAL -> c.warn; VaultState.FAILED -> c.bad
        }, tween(500), label = "heroTint",
    )
    val (over, title, body) = when (state) {
        VaultState.LOCAL -> Triple("Only on this phone", if (n == 0) "Start your box" else bills(n), "Sign in with Google to keep every bill safe in your Drive.")
        VaultState.SYNCING -> Triple("Syncing with Drive", if (n == 0) "Looking for bills" else bills(n), "Uploading and fetching bills…")
        VaultState.FAILED -> Triple("Sync problem", if (pending > 0) "${bills(pending)} not uploaded" else "Couldn't reach Drive", (sync as? SyncState.Failed)?.message ?: "")
        VaultState.PENDING -> Triple(
            if (sync is SyncState.NeedsSignIn) "Sign in again" else "Waiting to upload",
            if (pending > 0) "${bills(pending)} to upload" else bills(n),
            if (sync is SyncState.NeedsSignIn) "Google needs you to sign in before sync can continue." else "They'll go to Drive on the next sync.",
        )
        else -> Triple("All safe in Drive", if (n == 0) "Your box is ready" else bills(n),
            data.lastSyncAt?.let { "Synced ${Fmt.ago(it)}" } ?: "Backed up to Google Drive")
    }
    val action: Pair<String, () -> Unit>? = when (state) {
        VaultState.LOCAL -> "Sign in" to onSignIn
        VaultState.FAILED -> "Retry" to onSync
        VaultState.PENDING -> if (sync is SyncState.NeedsSignIn) "Sign in" to onSignIn else "Sync now" to onSync
        VaultState.SYNCED -> "Sync" to onSync
        else -> null
    }
    Surface(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = c.surface, border = BorderStroke(1.dp, c.line),
    ) {
        Column(
            Modifier.drawBehind {
                drawRect(Brush.verticalGradient(listOf(tint.copy(alpha = 0.13f), Color.Transparent), endY = size.height * 0.8f))
            }.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VaultOrbit(state, size = 124.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AnimatedContent(over to title, transitionSpec = {
                        (fadeIn(tween(260, 80)) + slideInVertically(tween(320)) { it / 3 }) togetherWith (fadeOut(tween(160)) + slideOutVertically(tween(220)) { -it / 3 })
                    }, label = "heroTitle") { (o, t) ->
                        Column {
                            Overline(o, color = tint)
                            Spacer(Modifier.height(2.dp))
                            Text(t, style = MaterialTheme.typography.headlineSmall.merge(Numeric), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(body, transitionSpec = { fadeIn(tween(300, 60)) togetherWith fadeOut(tween(150)) }, label = "heroBody") { b ->
                        Text(b, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    AnimatedVisibility(action != null, enter = fadeIn() + expandVertically(Motion.smooth()), exit = fadeOut() + shrinkVertically(Motion.smooth())) {
                        val press = remember { MutableInteractionSource() }
                        val a = action
                        Button(
                            onClick = { haptics.tap(); a?.second?.invoke() }, interactionSource = press,
                            modifier = Modifier.padding(top = 10.dp).height(36.dp).pressScale(press, 0.94f),
                            shape = CircleShape, contentPadding = PaddingValues(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                        ) { Text(a?.first ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
            if (n > 0) {
                val year = LocalDate.now().year
                val spent = data.bills.filter { it.invoiceDate.year == year }.sumOf { it.amount ?: 0.0 }
                val active = data.bills.count { b -> b.warrantyUntil?.let { !it.isBefore(LocalDate.now()) } == true }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = c.line)
                Row(Modifier.fillMaxWidth()) {
                    if (spent > 0) CountStat("Spent in $year", spent, { Fmt.rupees(it) }, Modifier.weight(1.4f))
                    else Stat("Spent in $year", "—", Modifier.weight(1.4f))
                    Stat("Warranties", if (soonCount > 0) "$active · $soonCount soon" else active.toString(), Modifier.weight(1f),
                        color = if (soonCount > 0) c.warn else c.text)
                }
            }
        }
    }
}

@Composable
private fun CountStat(label: String, value: Double, format: (Double) -> String, modifier: Modifier) {
    Column(modifier) {
        Overline(label)
        Spacer(Modifier.height(4.dp))
        CountUpText(value, format, MaterialTheme.typography.titleMedium.merge(Numeric).copy(fontWeight = FontWeight.Bold), Wiz.c.text)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color = Wiz.c.text) {
    Column(modifier) {
        Overline(label)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.merge(Numeric), color = color, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WarrantyCard(vm: AppViewModel, soon: List<Bill>, onOpen: (Bill) -> Unit, modifier: Modifier = Modifier) {
    val c = Wiz.c
    // The hourglass flips over a few times after each touch, then rests (no endless 60 fps spring).
    val allowed = com.wizlabs.billbox.ui.anim.rememberLoopsAllowed()
    val touches = com.wizlabs.billbox.ui.anim.LoopIdle.touches
    val flip = remember { Animatable(0f) }
    LaunchedEffect(allowed, touches) {
        if (!allowed) return@LaunchedEffect
        repeat(3) { delay(2600); flip.animateTo(flip.value + 180f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
    }
    WizCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.HourglassBottom, null, tint = c.warn, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = flip.value })
            Spacer(Modifier.width(8.dp))
            Text("Warranty ending soon", style = MaterialTheme.typography.titleMedium, color = c.text)
        }
        Spacer(Modifier.height(6.dp))
        soon.take(3).forEach { b ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onOpen(b) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(b.product, color = c.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Fmt.warranty(b.warrantyUntil!!).removePrefix("Warranty "), color = c.warn, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun BillRow(vm: AppViewModel, b: Bill, signedIn: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Wiz.c
    WizCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val first = b.photos.firstOrNull()
            if (first != null) {
                BillImage(vm, b.id, first, targetPx = 200, modifier = Modifier.sharedHero("hero-${b.id}").size(56.dp).clip(RoundedCornerShape(14.dp)))
            } else {
                Box(Modifier.sharedHero("hero-${b.id}")) { IconBadge(b.category.icon(), size = 56.dp, tint = c.accentInk, bg = c.surface2) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(b.product, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedBoundsHero("title-${b.id}"))
                Text(b.store, style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.date(b.invoiceDate), style = MaterialTheme.typography.labelMedium, color = c.muted)
                    val w = b.warrantyUntil
                    if (w != null && !w.isBefore(LocalDate.now())) {
                        Pill("Warranty", Icons.Rounded.Verified, color = c.good, bg = c.good.copy(alpha = 0.12f))
                    }
                    if (b.photos.size > 1) Pill("${b.photos.size} photos")
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                b.amount?.let {
                    Text(Fmt.rupees(it), style = MaterialTheme.typography.titleMedium.merge(Numeric), color = c.text, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                CloudMark(uploaded = !b.dirty && b.driveJsonId != null, signedIn = signedIn)
            }
        }
    }
}
