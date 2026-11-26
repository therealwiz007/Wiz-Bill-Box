@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.wizlabs.billbox.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wizlabs.billbox.AdsSetup
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.R
import com.wizlabs.billbox.SyncState
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.engine.Mood
import com.wizlabs.billbox.engine.TallyBrain
import com.wizlabs.billbox.engine.Expression
import com.wizlabs.billbox.engine.Action
import com.wizlabs.billbox.engine.Line
import com.wizlabs.billbox.engine.WarrantyStatus
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.model.SortBy
import com.wizlabs.billbox.model.WarrantyFilter
import com.wizlabs.billbox.ui.Icons
import com.wizlabs.billbox.ui.anim.CountUpText
import com.wizlabs.billbox.ui.anim.Motion
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.pressScale
import com.wizlabs.billbox.ui.anim.rememberHaptics
import com.wizlabs.billbox.ui.anim.sharedBoundsHero
import com.wizlabs.billbox.ui.anim.sharedHero
import com.wizlabs.billbox.ui.components.BillImage
import com.wizlabs.billbox.ui.components.CategoryTile
import com.wizlabs.billbox.ui.components.CloudMark
import com.wizlabs.billbox.ui.components.DaysRing
import com.wizlabs.billbox.ui.components.EmptyState
import com.wizlabs.billbox.ui.components.PdfTile
import com.wizlabs.billbox.ui.components.PrimaryButton
import com.wizlabs.billbox.ui.components.SmallChip
import com.wizlabs.billbox.ui.components.SyncKind
import com.wizlabs.billbox.ui.components.SyncPill
import com.wizlabs.billbox.ui.components.WarrantyChip
import com.wizlabs.billbox.ui.components.WizCard
import com.wizlabs.billbox.ui.components.label
import com.wizlabs.billbox.ui.mascot.Tally
import com.wizlabs.billbox.ui.mascot.tallyContext
import com.wizlabs.billbox.ui.theme.Manrope
import com.wizlabs.billbox.ui.theme.Numeric
import com.wizlabs.billbox.ui.theme.Wiz
import java.time.LocalDate

/** Remembers, for this app launch, that the home intro already played and which rows were shown. */
private object HomeIntro {
    var played = false
    val seen = mutableSetOf<String>()
}

/** "Wiz" at weight 700 then the product word at 500: the Wiz Labs wordmark. */
@Composable
fun Wordmark(size: Float = 20f, color: androidx.compose.ui.graphics.Color = Wiz.c.text): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = size.sp, color = color)) { append("Wiz") }
    withStyle(SpanStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = size.sp, color = color)) { append(" Bill Box") }
}

private fun syncKind(data: AppData, sync: SyncState): SyncKind = when {
    data.account == null -> SyncKind.SIGN_IN
    sync is SyncState.NeedsSignIn -> SyncKind.SIGN_IN
    sync is SyncState.Running -> SyncKind.UPLOADING
    sync is SyncState.Failed -> if (sync.offline) SyncKind.OFFLINE else SyncKind.ERROR
    data.bills.any { it.dirty } -> SyncKind.UPLOADING
    else -> SyncKind.SYNCED
}

/** True while the list is at the top or the user is scrolling back up (keeps the FAB label visible). */
@Composable
private fun LazyListState.expandFab(): Boolean {
    var prevIndex by remember(this) { androidx.compose.runtime.mutableIntStateOf(firstVisibleItemIndex) }
    var prevOffset by remember(this) { androidx.compose.runtime.mutableIntStateOf(firstVisibleItemScrollOffset) }
    var up by remember(this) { mutableStateOf(true) }
    val atTop by remember(this) { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset < 40 } }
    LaunchedEffect(firstVisibleItemIndex, firstVisibleItemScrollOffset) {
        up = if (firstVisibleItemIndex != prevIndex) firstVisibleItemIndex < prevIndex else firstVisibleItemScrollOffset <= prevOffset
        prevIndex = firstVisibleItemIndex; prevOffset = firstVisibleItemScrollOffset
    }
    return atTop || up
}

@Composable
fun HomeScaffold(
    vm: AppViewModel, data: AppData, sync: SyncState, listState: LazyListState,
    selectedId: String? = null, onOpen: (Bill) -> Unit, onAdd: () -> Unit, onSettings: () -> Unit,
) {
    val c = Wiz.c
    val expanded = listState.expandFab()
    val haptics = rememberHaptics()
    val fabPress = remember { MutableInteractionSource() }
    var errorSheet by remember { mutableStateOf(false) }
    val kind = syncKind(data, sync)
    Scaffold(
        containerColor = c.bg,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Wordmark(), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                SyncPill(kind, data.bills.count { it.dirty }, onClick = {
                    when (kind) {
                        SyncKind.SIGN_IN -> vm.signIn()
                        SyncKind.ERROR -> errorSheet = true
                        SyncKind.UPLOADING -> {}
                        else -> vm.requestSync()
                    }
                })
                val acc = data.account
                Box(Modifier.minimumInteractiveComponentSize().clip(CircleShape).clickable(onClick = onSettings)
                    .semantics { contentDescription = "" }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        val initial = (acc?.name?.takeIf { it.isNotBlank() } ?: acc?.email)?.firstOrNull()?.uppercaseChar()
                        if (initial != null) Text(initial.toString(), color = c.accentInk, fontWeight = FontWeight.Bold, fontFamily = Manrope)
                        else Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = c.accentInk, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { haptics.tap(); onAdd() }, expanded = expanded, interactionSource = fabPress,
                containerColor = c.accent, contentColor = c.onAccent,
                icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(stringResource(R.string.add_bill), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                modifier = Modifier.enterUp(index = 3, distance = 40.dp).pressScale(fabPress, 0.92f),
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            HomeScreen(vm, data, sync, pad, listState, selectedId, onOpen = onOpen, onAdd = onAdd, onSignIn = vm::signIn, modifier = Modifier.widthIn(max = 640.dp))
        }
    }
    if (errorSheet) {
        val st = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { errorSheet = false }, sheetState = st, containerColor = c.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Tally(TallyBrain.decide(tallyContext(data, sync)), 72.dp)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.sync_error_title), style = MaterialTheme.typography.titleLarge, color = c.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text((sync as? SyncState.Failed)?.message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(stringResource(R.string.retry), { errorSheet = false; vm.requestSync() }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun HomeScreen(
    vm: AppViewModel, data: AppData, sync: SyncState, contentPadding: PaddingValues, listState: LazyListState, selectedId: String?,
    onOpen: (Bill) -> Unit, onAdd: () -> Unit, onSignIn: () -> Unit, modifier: Modifier = Modifier,
) {
    val c = Wiz.c
    // The intro (cards rising in one after another) plays once per app launch, not on every return.
    val intro = remember { !HomeIntro.played }
    LaunchedEffect(Unit) { HomeIntro.played = true }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortBy.DATE) }
    var wf by rememberSaveable { mutableStateOf(WarrantyFilter.ALL) }
    var sortMenu by remember { mutableStateOf(false) }
    val window = data.warrantyReminderDays

    val list = remember(data.bills, query, category, sort, wf, window) { BillQuery.apply(data.bills, query, category, sort, wf, window) }
    val counts = remember(data.bills, window) { BillQuery.counts(data.bills, window) }
    val usedCats = remember(data.bills) { data.bills.mapNotNull { it.category }.distinct().sortedBy { it.ordinal } }
    val soon = remember(data.bills, window) { BillQuery.warrantySoon(data.bills, window) }
    val signedIn = data.account != null
    val filtering = query.isNotBlank() || category != null || wf != WarrantyFilter.ALL

    PullToRefreshBox(
        isRefreshing = sync is SyncState.Running,
        onRefresh = { if (signedIn) vm.requestSync() else onSignIn() },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (data.bills.isNotEmpty()) {
                item(key = "search", contentType = "search") {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).enterUp(0, intro),
                        placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.clear)) }
                        },
                        singleLine = true, shape = CircleShape,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = c.surface, focusedContainerColor = c.surface,
                            unfocusedBorderColor = c.line, focusedBorderColor = c.accentInk,
                        ),
                    )
                }
                item(key = "segments", contentType = "segments") {
                    Segmented(wf, counts, onSelect = { wf = it }, modifier = Modifier.enterUp(1, intro))
                }
                item(key = "stats", contentType = "stats") { StatsRow(data, Modifier.enterUp(2, intro)) }
                if (soon.isNotEmpty() && !filtering) {
                    item(key = "warranty", contentType = "strip") { WarrantyStrip(vm, soon, onOpen, Modifier.animateItem().enterUp(3, intro)) }
                }
                item(key = "chips", contentType = "chips") {
                    Row(Modifier.enterUp(4, intro), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { SmallChip(stringResource(R.string.all), category == null, { category = null }) }
                            items(usedCats) { cat -> SmallChip(cat.label(), category == cat, { category = if (category == cat) null else cat }) }
                        }
                        Box {
                            IconButton(onClick = { sortMenu = true }) { Icon(Icons.Rounded.Sort, stringResource(R.string.sort), tint = c.muted) }
                            DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                                SortBy.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(sortLabel(s)), fontWeight = if (s == sort) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sort = s; sortMenu = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (data.bills.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        TallyBrain.decide(tallyContext(data, sync)), stringResource(R.string.empty_title),
                        if (sync is SyncState.Running) stringResource(R.string.empty_checking) else stringResource(R.string.empty_body),
                        action = stringResource(R.string.empty_cta), onAction = onAdd,
                    )
                }
            } else if (list.isEmpty()) {
                item(key = "nomatch") {
                    EmptyState(
                        TallyBrain.decide(tallyContext(data, sync, searchNoResults = true)), stringResource(R.string.no_match_title),
                        stringResource(R.string.no_match_body), tallySize = 96.dp,
                    )
                }
            } else {
                var lastHeader: String? = null
                var rowIndex = 0
                var groups = 0
                list.forEach { b ->
                    val header = if (sort == SortBy.DATE) Fmt.month(b.invoiceDate) else null
                    if (header != null && header != lastHeader) {
                        // The "home" native ad slot goes after the first month group (3+ bills, not while searching or filtering).
                        if (groups == 1 && AdsSetup.ENABLED && list.size >= 3 && !filtering) item(key = "ad-home") { com.wizlabs.billbox.AdSlot("home") }
                        groups++
                        lastHeader = header
                        stickyHeader(key = "h-$header", contentType = "month") {
                            Text(
                                header.uppercase(), Modifier.fillMaxWidth().background(c.bg).padding(top = 8.dp, bottom = 6.dp, start = 4.dp),
                                style = MaterialTheme.typography.labelMedium.merge(Numeric), color = c.muted,
                            )
                        }
                    }
                    val idx = rowIndex++
                    item(key = b.id, contentType = "bill") {
                        BillCard(vm, b, signedIn, selected = b.id == selectedId, window = window,
                            modifier = Modifier.animateItem(fadeInSpec = tween(180), placementSpec = Motion.smooth(), fadeOutSpec = tween(120))
                                .enterUp(5 + idx.coerceAtMost(3), intro || b.id !in HomeIntro.seen)) { onOpen(b) }
                        LaunchedEffect(b.id) { HomeIntro.seen += b.id }
                    }
                }
            }
        }
    }
}

private fun sortLabel(s: SortBy) = when (s) {
    SortBy.DATE -> R.string.sort_date; SortBy.ADDED -> R.string.sort_added; SortBy.AMOUNT -> R.string.sort_amount; SortBy.STORE -> R.string.sort_store
}

/** All · Under warranty · Expiring · Expired, each with its count. */
@Composable
private fun Segmented(selected: WarrantyFilter, counts: Map<WarrantyFilter, Int>, onSelect: (WarrantyFilter) -> Unit, modifier: Modifier = Modifier) {
    val c = Wiz.c
    val haptics = rememberHaptics()
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(3.dp)) {
        WarrantyFilter.entries.forEach { f ->
            val on = f == selected
            val label = stringResource(when (f) {
                WarrantyFilter.ALL -> R.string.seg_all; WarrantyFilter.ACTIVE -> R.string.seg_active
                WarrantyFilter.EXPIRING -> R.string.seg_expiring; WarrantyFilter.EXPIRED -> R.string.seg_expired
            })
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (on) c.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, c.line, RoundedCornerShape(11.dp)) else Modifier)
                    .clickable { haptics.tick(); onSelect(f) }.padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = if (on) c.accentInk else c.muted,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp)
                    Text("${counts[f] ?: 0}", style = MaterialTheme.typography.labelSmall.merge(Numeric).copy(letterSpacing = 0.sp), color = if (on) c.text else c.muted)
                }
            }
        }
    }
}

/** Bills, ₹ this year, active warranties. Numbers roll when they change. */
@Composable
private fun StatsRow(data: AppData, modifier: Modifier = Modifier) {
    val c = Wiz.c
    val year = LocalDate.now().year
    val spent = remember(data.bills) { data.bills.filter { it.invoiceDate.year == year }.sumOf { it.amount ?: 0.0 } }
    val active = remember(data.bills) { data.bills.count { b -> b.warrantyUntil?.let { !it.isBefore(LocalDate.now()) } == true } }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(stringResource(R.string.stat_bills), data.bills.size.toDouble(), { it.toLong().toString() }, Modifier.weight(0.8f))
        StatTile(stringResource(R.string.stat_spent, year), spent, { if (it > 0) Fmt.rupees(it) else "—" }, Modifier.weight(1.5f))
        StatTile(stringResource(R.string.stat_warranties), active.toDouble(), { it.toLong().toString() }, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: Double, format: (Double) -> String, modifier: Modifier) {
    val c = Wiz.c
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp), color = c.muted, maxLines = 2, lineHeight = 13.sp)
        Spacer(Modifier.height(4.dp))
        CountUpText(value, format, MaterialTheme.typography.titleMedium.merge(Numeric).copy(fontSize = 18.sp), c.text)
    }
}

/** Horizontal cards for warranties ending within the window, with Tally's bell at the header. */
@Composable
private fun WarrantyStrip(vm: AppViewModel, soon: List<Bill>, onOpen: (Bill) -> Unit, modifier: Modifier = Modifier) {
    val c = Wiz.c
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tally(Mood(Expression.ALERT, Action.BELL_RING, Line.WarrantiesSoon(soon.size)), 40.dp)
            Spacer(Modifier.width(8.dp))
            Text(pluralStringResource(R.plurals.strip_title, soon.size, soon.size), style = MaterialTheme.typography.titleSmall, color = c.text)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(soon.take(8), key = { it.id }) { b ->
                val st = WarrantyStatus.of(b.warrantyUntil!!, windowDays = 90)
                val total = java.time.temporal.ChronoUnit.DAYS.between(b.invoiceDate, b.warrantyUntil).coerceAtLeast(30)
                WizCard(Modifier.width(220.dp), onClick = { onOpen(b) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DaysRing((st.days.toFloat() / total).coerceIn(0.04f, 1f), com.wizlabs.billbox.ui.components.warrantyColor(st), st.days.toString(), stringResource(R.string.days_short), size = 56.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(b.product, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(b.store, style = MaterialTheme.typography.labelMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillCard(
    vm: AppViewModel, b: Bill, signedIn: Boolean, selected: Boolean, window: Int,
    modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val c = Wiz.c
    WizCard(modifier.heightIn(min = 72.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val first = b.photos.firstOrNull()
            val pdf = b.files.firstOrNull()
            when {
                first != null -> BillImage(vm, b.id, first, targetPx = 200, modifier = Modifier.sharedHero("hero-${b.id}").size(56.dp).clip(RoundedCornerShape(12.dp)))
                pdf != null -> PdfTile(vm, b.id, pdf, Modifier.sharedHero("hero-${b.id}").size(56.dp), widthPx = 200)
                else -> Box(Modifier.sharedHero("hero-${b.id}")) { CategoryTile(b.category, 56.dp) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(b.store, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp), color = if (selected) c.accentInk else c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedBoundsHero("title-${b.id}"))
                Text(b.product, style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.date(b.invoiceDate), style = MaterialTheme.typography.labelMedium.merge(Numeric), color = c.muted, maxLines = 1)
                    b.amount?.let {
                        val words = stringResource(R.string.cd_rupees, Fmt.money(it))
                        Text(Fmt.rupees(it), Modifier.semantics { contentDescription = words },
                            style = MaterialTheme.typography.labelMedium.merge(Numeric), color = c.text, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                b.warrantyUntil?.let { WarrantyChip(WarrantyStatus.of(it, windowDays = window)) }
                CloudMark(uploaded = !b.dirty && b.driveJsonId != null, signedIn = signedIn)
            }
        }
    }
}
