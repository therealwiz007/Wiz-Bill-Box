package com.wizlabs.billbox.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.data.BillRepository
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.ui.components.BillImage
import com.wizlabs.billbox.ui.components.IconBadge
import com.wizlabs.billbox.ui.components.InfoRow
import com.wizlabs.billbox.ui.components.Overline
import com.wizlabs.billbox.ui.components.WizCard
import com.wizlabs.billbox.ui.components.icon
import com.wizlabs.billbox.ui.theme.Wiz
import java.time.LocalDate
import com.wizlabs.billbox.ui.anim.CountUpText
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.rememberPulse
import com.wizlabs.billbox.ui.anim.sharedBoundsHero
import com.wizlabs.billbox.ui.anim.sharedHero
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillDetail(vm: AppViewModel, bill: Bill, signedIn: Boolean, onBack: () -> Unit, onEdit: () -> Unit) {
    val c = Wiz.c
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<Int?>(null) }
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { share(context, vm, bill) }) { Icon(Icons.Rounded.Share, "Share") }
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, navigationIconContentColor = c.text, actionIconContentColor = c.text),
            )
        },
    ) { pad ->
        val scroll = rememberScrollState()
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(scroll).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bill.photos.isNotEmpty()) {
                val pager = rememberPagerState { bill.photos.size }
                Box {
                    HorizontalPager(pager, pageSpacing = 10.dp) { i ->
                        // Neighbouring photos shrink and dim as you swipe; the photo drifts slower than the page (parallax).
                        val offset = ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        BillImage(
                            vm, bill.id, bill.photos[i], 1200,
                            Modifier.then(if (i == 0) Modifier.sharedHero("hero-${bill.id}") else Modifier)
                                .fillMaxWidth().aspectRatio(0.8f)
                                .graphicsLayer {
                                    val s = 1f - 0.1f * offset
                                    scaleX = s; scaleY = s; alpha = 1f - 0.35f * offset
                                    translationY = scroll.value * 0.35f
                                }
                                .clip(RoundedCornerShape(20.dp)).clickable { viewer = i },
                        )
                    }
                    if (bill.photos.size > 1) {
                        Text(
                            "${pager.currentPage + 1} / ${bill.photos.size}",
                            Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White, style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(if (bill.photos.isEmpty()) Modifier.sharedHero("hero-${bill.id}") else Modifier.enterUp(0)) {
                    IconBadge(bill.category.icon(), size = 48.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(bill.product, style = MaterialTheme.typography.headlineSmall, color = c.text,
                        modifier = Modifier.sharedBoundsHero("title-${bill.id}"))
                    Text(bill.store, style = MaterialTheme.typography.bodyLarge, color = c.muted)
                }
            }
            bill.amount?.let {
                CountUpText(it, { v -> Fmt.rupees(v) }, MaterialTheme.typography.displaySmall, c.accentInk, Modifier.enterUp(1))
            }

            WizCard(Modifier.enterUp(2)) {
                InfoRow("Store", bill.store, Icons.Rounded.Storefront)
                InfoRow("Invoice date", Fmt.date(bill.invoiceDate), Icons.Rounded.CalendarMonth)
                bill.amount?.let { InfoRow("Amount", Fmt.rupees(it), Icons.Rounded.CurrencyRupee) }
                if (bill.invoiceNumber.isNotBlank()) InfoRow("Invoice no.", bill.invoiceNumber, Icons.Rounded.Numbers)
                bill.paymentMode?.let { InfoRow("Paid by", it.label, Icons.Rounded.Payments) }
                bill.category?.let { InfoRow("Category", it.label, it.icon()) }
                if (bill.gstin.isNotBlank()) InfoRow("GSTIN", bill.gstin)
            }

            bill.warrantyUntil?.let { w ->
                val active = !w.isBefore(LocalDate.now())
                val beat = if (active) rememberPulse(0.9f, 1.12f, 1600) else 1f
                WizCard(Modifier.enterUp(3)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Verified, null, tint = if (active) c.good else c.muted,
                            modifier = Modifier.graphicsLayer { scaleX = beat; scaleY = beat })
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Warranty until ${Fmt.date(w)}", style = MaterialTheme.typography.titleSmall, color = c.text, fontWeight = FontWeight.Bold)
                            Text(Fmt.warranty(w), style = MaterialTheme.typography.bodySmall, color = if (active) c.good else c.muted)
                        }
                    }
                }
            }

            if (bill.notes.isNotBlank()) {
                WizCard(Modifier.enterUp(4)) {
                    Overline("Notes")
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
                        when {
                            !signedIn -> "Only on this phone. Sign in to save it to Google Drive."
                            uploaded -> "Saved in Google Drive › Wiz Bill Box"
                            else -> "Waiting to upload to Google Drive"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f),
                    )
                }
                val folder = bill.driveFolderId
                if (folder != null) {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = c.line)
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/folders/$folder")))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.FolderOpen, null, tint = c.accentInk)
                        Spacer(Modifier.width(8.dp))
                        Text("Open in Google Drive", color = c.text)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Added ${Fmt.date(java.time.Instant.ofEpochMilli(bill.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate())}",
                    style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    viewer?.let { start -> PhotoViewer(vm, bill, start) { viewer = null } }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this bill?") },
            text = {
                Text(
                    if (bill.driveFolderId != null) "It will be removed from this phone and moved to the bin in your Google Drive (you can restore it from there for 30 days)."
                    else "It will be removed from this phone."
                )
            },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deleteBill(bill.id); onBack() }) { Text("Delete", color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoViewer(vm: AppViewModel, bill: Bill, start: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val pager = rememberPagerState(initialPage = start) { bill.photos.size }
            HorizontalPager(pager, Modifier.fillMaxSize()) { i -> Zoomable { BillImage(vm, bill.id, bill.photos[i], 2400, Modifier.fillMaxSize(), ContentScale.Fit) } }
            IconButton(
                onClick = onClose,
                modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopStart).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
            ) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
        }
    }
}

@Composable
private fun Zoomable(content: @Composable () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
    ) { content() }
}

private fun share(context: Context, vm: AppViewModel, bill: Bill) {
    val text = BillRepository.description(bill).removeSuffix("\nSaved with Wiz Bill Box") +
        (if (bill.notes.isNotBlank()) "\nNotes: ${bill.notes}" else "")
    val uris = bill.photos.mapNotNull { p ->
        val f = p.localName?.let { vm.photos.file(it) }?.takeIf { it.exists() } ?: return@mapNotNull null
        FileProvider.getUriForFile(context, "${context.packageName}.files", f)
    }
    val intent = when {
        uris.isEmpty() -> Intent(Intent.ACTION_SEND).setType("text/plain")
        uris.size == 1 -> Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uris[0])
        else -> Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/jpeg").putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }.putExtra(Intent.EXTRA_TEXT, text).putExtra(Intent.EXTRA_SUBJECT, "${bill.store} – ${bill.product}")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share bill"))
}
