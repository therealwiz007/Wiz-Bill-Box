@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.wizlabs.billbox.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.R
import com.wizlabs.billbox.data.BillRepository
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.engine.WarrantyStatus
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.BillFile
import com.wizlabs.billbox.ui.Icons
import com.wizlabs.billbox.ui.anim.CountUpText
import com.wizlabs.billbox.ui.anim.WizBackHandler
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.rememberHaptics
import com.wizlabs.billbox.ui.anim.sharedBoundsHero
import com.wizlabs.billbox.ui.anim.sharedHero
import com.wizlabs.billbox.ui.components.BillImage
import com.wizlabs.billbox.ui.components.CategoryTile
import com.wizlabs.billbox.ui.components.DaysRing
import com.wizlabs.billbox.ui.components.InfoRow
import com.wizlabs.billbox.ui.components.Overline
import com.wizlabs.billbox.ui.components.PdfTile
import com.wizlabs.billbox.ui.components.WizCard
import com.wizlabs.billbox.ui.components.label
import com.wizlabs.billbox.ui.components.icon
import com.wizlabs.billbox.ui.components.warrantyColor
import com.wizlabs.billbox.ui.components.warrantyText
import com.wizlabs.billbox.ui.theme.Numeric
import com.wizlabs.billbox.ui.theme.Wiz
import com.wizlabs.billbox.reminders.WarrantyScheduler
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun BillDetail(vm: AppViewModel, bill: Bill, data: AppData, onBack: () -> Unit, onEdit: () -> Unit, showBack: Boolean = true) {
    val c = Wiz.c
    val context = LocalContext.current
    val signedIn = data.account != null
    var confirmDelete by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    var viewer by remember { mutableStateOf<Int?>(null) }
    if (showBack) WizBackHandler("detail:${bill.id}", onBack = onBack)
    val noViewer = stringResource(R.string.err_no_pdf_viewer)

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { if (showBack) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, navigationIconContentColor = c.text),
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(c.surface).border(1.dp, c.line).navigationBarsPadding().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionButton(Icons.Rounded.Share, stringResource(R.string.share)) { share(context, vm, bill) }
                if (bill.driveFolderId != null) ActionButton(Icons.Rounded.FolderOpen, stringResource(R.string.open_in_drive)) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/folders/${bill.driveFolderId}"))) }
                }
                ActionButton(Icons.Rounded.Edit, stringResource(R.string.edit), onClick = onEdit)
                ActionButton(Icons.Rounded.Delete, stringResource(R.string.delete), tint = c.bad) { confirmDelete = true }
            }
        },
    ) { pad ->
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bill.photos.isNotEmpty()) {
                val pager = rememberPagerState { bill.photos.size }
                Box {
                    HorizontalPager(pager, pageSpacing = 10.dp) { i ->
                        // Neighbouring photos shrink and dim as you swipe; the photo drifts slower than the page.
                        val offset = ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        BillImage(
                            vm, bill.id, bill.photos[i], 1200,
                            Modifier.then(if (i == 0) Modifier.sharedHero("hero-${bill.id}") else Modifier)
                                .fillMaxWidth().aspectRatio(1.6f)
                                .graphicsLayer {
                                    val s = 1f - 0.1f * offset
                                    scaleX = s; scaleY = s; alpha = 1f - 0.35f * offset
                                    translationY = scroll.value * 0.35f
                                }
                                .clip(RoundedCornerShape(18.dp)).clickable { viewer = i },
                        )
                    }
                    if (bill.photos.size > 1) {
                        Row(Modifier.align(Alignment.BottomCenter).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(bill.photos.size) { i ->
                                Box(Modifier.size(if (i == pager.currentPage) 8.dp else 6.dp).clip(CircleShape)
                                    .background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.55f)))
                            }
                        }
                    }
                }
            }

            if (bill.files.isNotEmpty()) {
                Overline(stringResource(R.string.pdf_bills))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bill.files, key = { it.id }) { f ->
                        Column(Modifier.width(110.dp)) {
                            PdfTile(vm, bill.id, f, Modifier.size(width = 110.dp, height = 140.dp)) { openPdf(context, vm, bill, f, noViewer) }
                            Text(f.name, style = MaterialTheme.typography.labelMedium, color = c.muted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(if (bill.photos.isEmpty() && bill.files.isEmpty()) Modifier.sharedHero("hero-${bill.id}") else Modifier.enterUp(0)) { CategoryTile(bill.category, 48.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(bill.store, style = MaterialTheme.typography.headlineSmall, color = c.text, modifier = Modifier.sharedBoundsHero("title-${bill.id}"))
                    Text(bill.product, style = MaterialTheme.typography.bodyLarge, color = c.muted)
                }
            }
            bill.amount?.let {
                val words = stringResource(R.string.cd_rupees, Fmt.money(it))
                CountUpText(it, { v -> Fmt.rupees(v) }, MaterialTheme.typography.displaySmall.merge(Numeric), c.text, Modifier.enterUp(1).semantics { contentDescription = words })
            }

            bill.warrantyUntil?.let { w -> WarrantyCard(bill, w, data, Modifier.enterUp(2)) }

            WizCard(Modifier.enterUp(3)) {
                InfoRow(stringResource(R.string.f_store_short), bill.store, Icons.Rounded.Storefront)
                InfoRow(stringResource(R.string.f_date_short), Fmt.date(bill.invoiceDate), Icons.Rounded.CalendarMonth, mono = true)
                bill.amount?.let { InfoRow(stringResource(R.string.f_amount_short), Fmt.rupees(it), Icons.Rounded.CurrencyRupee, mono = true) }
                if (bill.invoiceNumber.isNotBlank()) InfoRow(stringResource(R.string.f_invoice_no), bill.invoiceNumber, Icons.Rounded.Numbers, mono = true)
                bill.paymentMode?.let { InfoRow(stringResource(R.string.f_paid_by), it.label(), Icons.Rounded.Payments) }
                bill.category?.let { InfoRow(stringResource(R.string.f_category), it.label(), it.icon()) }
                if (bill.gstin.isNotBlank()) InfoRow(stringResource(R.string.f_gstin_short), bill.gstin, mono = true)
            }

            if (bill.notes.isNotBlank()) {
                WizCard(Modifier.enterUp(4)) {
                    Overline(stringResource(R.string.f_notes_short))
                    Spacer(Modifier.height(6.dp))
                    Text(bill.notes, style = MaterialTheme.typography.bodyLarge, color = c.text)
                }
            }

            WizCard(Modifier.enterUp(5)) {
                val uploaded = !bill.dirty && bill.driveJsonId != null
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (uploaded) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload, null, tint = if (uploaded) c.good else c.warn)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(when {
                            !signedIn -> R.string.detail_local_only
                            uploaded -> R.string.detail_saved_drive
                            else -> R.string.detail_waiting
                        }),
                        style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.added_on, Fmt.date(java.time.Instant.ofEpochMilli(bill.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate())),
                    style = MaterialTheme.typography.labelMedium.merge(Numeric), color = c.muted)
            }
            Spacer(Modifier.height(16.dp))
        }
        }
    }

    viewer?.let { start -> PhotoViewer(vm, bill, start) { viewer = null } }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(if (bill.driveFolderId != null) R.string.delete_body_drive else R.string.delete_body_local)) },
            confirmButton = { TextButton(onClick = { haptics.reject(); confirmDelete = false; vm.deleteBill(bill.id); onBack() }) { Text(stringResource(R.string.delete), color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = Wiz.c.text, onClick: () -> Unit) {
    Column(
        Modifier.heightIn(min = 56.dp).widthIn(min = 64.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}

/** Days-left ring, end date and which reminders are on. */
@Composable
private fun WarrantyCard(bill: Bill, w: java.time.LocalDate, data: AppData, modifier: Modifier) {
    val c = Wiz.c
    val st = WarrantyStatus.of(w, windowDays = data.warrantyReminderDays)
    val total = java.time.temporal.ChronoUnit.DAYS.between(bill.invoiceDate, w).coerceAtLeast(30)
    val col = warrantyColor(st)
    val offsets = WarrantyScheduler.enabledOffsets(data)
    WizCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DaysRing(
                if (st.isActive) (st.days.toFloat() / total).coerceIn(0.03f, 1f) else 0f, col,
                if (st.isActive) st.days.toString() else "—", stringResource(if (st.isActive) R.string.days_short else R.string.wchip_expired), size = 76.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(warrantyText(st), style = MaterialTheme.typography.titleMedium, color = col)
                Text(stringResource(R.string.w_ends, Fmt.date(w)), style = MaterialTheme.typography.bodyMedium.merge(Numeric), color = c.text)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (offsets.isEmpty()) stringResource(R.string.reminders_off)
                    else stringResource(R.string.reminders_on, offsets.joinToString(" · ")),
                    style = MaterialTheme.typography.labelMedium, color = c.muted,
                )
            }
        }
    }
}

@Composable
private fun PhotoViewer(vm: AppViewModel, bill: Bill, start: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val dy = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (1f - dy.value.absoluteValue / 900f).coerceIn(0.2f, 1f)))) {
            val pager = rememberPagerState(initialPage = start) { bill.photos.size }
            var zoomed by remember { mutableStateOf(false) }
            HorizontalPager(pager, Modifier.fillMaxSize().graphicsLayer { translationY = dy.value }, userScrollEnabled = !zoomed) { i ->
                Zoomable(onZoomChange = { zoomed = it }, onDragDown = { d -> scope.launch { dy.snapTo((dy.value + d).coerceAtLeast(0f)) } },
                    onDragEnd = { scope.launch { if (dy.value > 260f) onClose() else dy.animateTo(0f, spring(0.8f, 400f)) } }) {
                    BillImage(vm, bill.id, bill.photos[i], 2400, Modifier.fillMaxSize(), ContentScale.Fit)
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopStart).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
            ) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White) }
        }
    }
}

/** Pinch and double-tap to zoom; drag down (when not zoomed) to close. */
@Composable
private fun Zoomable(onZoomChange: (Boolean) -> Unit, onDragDown: (Float) -> Unit, onDragEnd: () -> Unit, content: @Composable () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                    onZoomChange(scale > 1f)
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                    onZoomChange(scale > 1f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { if (scale <= 1f) onDragEnd() }, onDragCancel = { if (scale <= 1f) onDragEnd() }) { change, drag ->
                    if (scale <= 1f && drag.y > 0f && drag.y.absoluteValue > drag.x.absoluteValue) { change.consume(); onDragDown(drag.y) }
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
    ) { content() }
}

private fun openPdf(context: Context, vm: AppViewModel, bill: Bill, f: BillFile, noViewer: String) {
    val name = f.localName?.takeIf { vm.files.file(it).exists() }
    if (name == null) { vm.ensureFile(bill.id, f.id) { n -> if (n != null) openPdfFile(context, vm, n, noViewer) }; return }
    openPdfFile(context, vm, name, noViewer)
}

private fun openPdfFile(context: Context, vm: AppViewModel, name: String, noViewer: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", vm.files.file(name))
    val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(i) }.onFailure { vm.toast(noViewer) }
}

private fun share(context: Context, vm: AppViewModel, bill: Bill) {
    val text = BillRepository.description(bill).removeSuffix("\nSaved with Wiz Bill Box") +
        (if (bill.notes.isNotBlank()) "\nNotes: ${bill.notes}" else "")
    val photoUris = bill.photos.mapNotNull { p ->
        val f = p.localName?.let { vm.photos.file(it) }?.takeIf { it.exists() } ?: return@mapNotNull null
        FileProvider.getUriForFile(context, "${context.packageName}.files", f)
    }
    val pdfUris = bill.files.mapNotNull { f ->
        val file = f.localName?.let { vm.files.file(it) }?.takeIf { it.exists() } ?: return@mapNotNull null
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    val uris = photoUris + pdfUris
    val intent = when {
        uris.isEmpty() -> Intent(Intent.ACTION_SEND).setType("text/plain")
        uris.size == 1 -> Intent(Intent.ACTION_SEND).setType(if (pdfUris.isNotEmpty()) "application/pdf" else "image/jpeg").putExtra(Intent.EXTRA_STREAM, uris[0])
        else -> Intent(Intent.ACTION_SEND_MULTIPLE).setType(if (pdfUris.isEmpty()) "image/jpeg" else "*/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }.putExtra(Intent.EXTRA_TEXT, text).putExtra(Intent.EXTRA_SUBJECT, "${bill.store} – ${bill.product}")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_bill)))
}
