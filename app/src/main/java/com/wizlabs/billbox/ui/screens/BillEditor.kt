@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.wizlabs.billbox.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.R
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.engine.WarrantyStatus
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.BillFile
import com.wizlabs.billbox.model.BillPhoto
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.model.PaymentMode
import com.wizlabs.billbox.ui.Icons
import com.wizlabs.billbox.ui.anim.Motion
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.pressScale
import com.wizlabs.billbox.ui.anim.rememberHaptics
import com.wizlabs.billbox.ui.anim.shakeOn
import com.wizlabs.billbox.ui.components.BillImage
import com.wizlabs.billbox.ui.components.CategoryTile
import com.wizlabs.billbox.ui.components.Overline
import com.wizlabs.billbox.ui.components.PdfTile
import com.wizlabs.billbox.ui.components.SmallChip
import com.wizlabs.billbox.ui.components.WizCard
import com.wizlabs.billbox.ui.components.label
import com.wizlabs.billbox.ui.theme.Numeric
import com.wizlabs.billbox.ui.theme.Wiz
import com.wizlabs.billbox.ui.theme.categoryTint
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class DateField { INVOICE, WARRANTY }

/** GSTIN is 15 characters: 2 digits, 5 letters, 4 digits, a letter, then 3 more. Checked loosely (length + charset). */
internal fun gstinValid(s: String) = s.isEmpty() || (s.length == 15 && s.all { it.isLetterOrDigit() })

@Composable
fun BillEditor(vm: AppViewModel, existing: Bill?, knownStores: List<String>, data: AppData, onDone: (Bill?) -> Unit) {
    val c = Wiz.c
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var store by rememberSaveable { mutableStateOf(existing?.store.orEmpty()) }
    var product by rememberSaveable { mutableStateOf(existing?.product.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(existing?.invoiceDate ?: LocalDate.now()) }
    var amount by rememberSaveable { mutableStateOf(existing?.amount?.let { Fmt.money(it).replace(",", "") }.orEmpty()) }
    var invoiceNo by rememberSaveable { mutableStateOf(existing?.invoiceNumber.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category) }
    var payment by rememberSaveable { mutableStateOf(existing?.paymentMode) }
    var warranty by rememberSaveable { mutableStateOf(existing?.warrantyUntil) }
    var gstin by rememberSaveable { mutableStateOf(existing?.gstin.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var detailsOpen by rememberSaveable { mutableStateOf(existing != null) }
    val photos = remember { mutableStateListOf<BillPhoto>().apply { addAll(existing?.photos.orEmpty()) } }
    val docs = remember { mutableStateListOf<BillFile>().apply { addAll(existing?.files.orEmpty()) } }
    val added = remember { mutableStateListOf<String>() } // files imported in this session, removed on cancel
    val addedDocs = remember { mutableStateListOf<String>() }
    var importing by remember { mutableIntStateOf(0) }
    var showErrors by remember { mutableStateOf(false) }
    var shake by remember { mutableIntStateOf(0) }
    var datePicker by remember { mutableStateOf<DateField?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }

    val tooBig = stringResource(R.string.pdf_too_big)
    val notPdf = stringResource(R.string.pdf_unreadable)
    val noPicture = stringResource(R.string.err_picture)
    val noCamera = stringResource(R.string.err_no_camera)

    fun addUris(uris: List<Uri>) {
        uris.forEach { uri ->
            importing++
            scope.launch {
                val name = vm.importPhoto(uri)
                importing--
                if (name != null) { photos += BillPhoto(localName = name); added += name } else vm.toast(noPicture)
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile?.let(::File)
        if (ok && f != null && f.length() > 0) {
            importing++
            scope.launch {
                val name = vm.importPhoto(Uri.fromFile(f))
                importing--
                f.delete()
                if (name != null) { photos += BillPhoto(localName = name); added += name }
            }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { addUris(it) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importing++
            scope.launch {
                val r = vm.importPdf(uri)
                importing--
                r.onSuccess { docs += it; it.localName?.let { n -> addedDocs += n } }
                    .onFailure { vm.toast(if (it.message == "too-big") tooBig else notPdf) }
            }
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun openCamera() {
        val f = vm.photos.newCameraFile()
        cameraFile = f.path
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
        runCatching { camera.launch(uri) }.onFailure { vm.toast(noCamera) }
    }

    val amountText = existing?.amount?.let { Fmt.money(it).replace(",", "") } ?: ""
    val changed = existing == null && (store.isNotBlank() || product.isNotBlank() || photos.isNotEmpty() || docs.isNotEmpty()) ||
        existing != null && (store != existing.store || product != existing.product || date != existing.invoiceDate ||
            photos.toList() != existing.photos || docs.toList() != existing.files || notes != existing.notes || amount != amountText ||
            invoiceNo != existing.invoiceNumber || category != existing.category || payment != existing.paymentMode ||
            warranty != existing.warrantyUntil || gstin != existing.gstin)

    fun cancel() {
        added.forEach { vm.photos.delete(it) }
        addedDocs.forEach { vm.files.delete(it) }
        onDone(null)
    }

    val amountValue = Fmt.parseAmount(amount)
    val amountBad = amount.isNotBlank() && amountValue == null
    val gstBad = !gstinValid(gstin.trim())
    val required = store.isNotBlank() && product.isNotBlank()
    val valid = required && !amountBad && !gstBad && importing == 0

    val haptics = rememberHaptics()
    fun save() {
        if (!valid) { showErrors = true; shake++; haptics.reject(); if (gstBad || amountBad) detailsOpen = true; return }
        val base = existing ?: Bill(store = "", product = "", invoiceDate = date)
        val bill = base.copy(
            store = store.trim(), product = product.trim(), invoiceDate = date, amount = amountValue,
            invoiceNumber = invoiceNo.trim(), category = category, paymentMode = payment, warrantyUntil = warranty,
            gstin = gstin.trim().uppercase(), notes = notes.trim(), photos = photos.toList(), files = docs.toList(),
        )
        vm.saveBill(bill)
        // First warranty save: ask once for permission to remind (Android 13+).
        if (warranty != null && !data.notifAsked) {
            vm.notifAsked()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onDone(bill)
    }

    BackHandler { if (changed) confirmDiscard = true else cancel() }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.new_bill else R.string.edit_bill)) },
                navigationIcon = { IconButton(onClick = { if (changed) confirmDiscard = true else cancel() }) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) } },
                actions = {
                    TextButton(onClick = ::save, enabled = importing == 0, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (valid) c.accentInk else c.muted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text, navigationIconContentColor = c.text),
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The bill itself: camera, gallery, PDF, then what was added
            Overline(stringResource(R.string.sec_bill))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { AddTile(Icons.Rounded.PhotoCamera, stringResource(R.string.camera)) { openCamera() } }
                item { AddTile(Icons.Rounded.AddPhotoAlternate, stringResource(R.string.gallery)) { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
                item { AddTile(Icons.Rounded.Pdf, stringResource(R.string.add_pdf)) { pdfPicker.launch(arrayOf("application/pdf")) } }
                if (importing > 0) item {
                    Box(Modifier.size(width = 96.dp, height = 120.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = c.accentInk)
                    }
                }
                items(photos, key = { it.id }) { p ->
                    val fresh = p.localName in added
                    val pop = remember { Animatable(if (fresh) 0.4f else 1f) }
                    LaunchedEffect(Unit) { pop.animateTo(1f, Motion.bouncy()) }
                    Box(
                        Modifier.animateItem(fadeInSpec = tween(180), placementSpec = Motion.smooth(), fadeOutSpec = tween(120))
                            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) }
                            .size(width = 96.dp, height = 120.dp),
                    ) {
                        BillImage(vm, existing?.id ?: "", p, 360, Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        RemoveX(Modifier.align(Alignment.TopEnd)) {
                            photos.remove(p)
                            if (p.localName in added) { vm.photos.delete(p.localName); added.remove(p.localName) }
                        }
                    }
                }
                items(docs, key = { it.id }) { f ->
                    Box(Modifier.animateItem().size(width = 96.dp, height = 120.dp)) {
                        PdfTile(vm, existing?.id ?: "", f, Modifier.fillMaxSize())
                        RemoveX(Modifier.align(Alignment.TopEnd)) {
                            docs.remove(f)
                            if (f.localName in addedDocs) { vm.files.delete(f.localName); addedDocs.remove(f.localName) }
                        }
                    }
                }
            }
            if (docs.isNotEmpty()) Text(docs.joinToString(" · ") { it.name }, style = MaterialTheme.typography.labelMedium, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)

            // Required details
            Overline(stringResource(R.string.sec_basics), Modifier.padding(top = 8.dp))
            Field(
                store, { store = it }, stringResource(R.string.f_store), error = showErrors && store.isBlank(), errorText = stringResource(R.string.required),
                cap = KeyboardCapitalization.Words, modifier = Modifier.enterUp(0).shakeOn(if (store.isBlank()) shake else 0),
            )
            val suggestions = remember(store, knownStores) {
                if (store.isEmpty()) emptyList()
                else knownStores.filter { it.contains(store, ignoreCase = true) && !it.equals(store, ignoreCase = true) }.take(4)
            }
            if (suggestions.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { suggestions.forEach { s -> SmallChip(s, false, { store = s }) } }
            Field(
                product, { product = it }, stringResource(R.string.f_product), error = showErrors && product.isBlank(), errorText = stringResource(R.string.required),
                cap = KeyboardCapitalization.Sentences, modifier = Modifier.enterUp(1).shakeOn(if (product.isBlank()) shake else 0),
            )
            DateBox(stringResource(R.string.f_date), Fmt.date(date)) { datePicker = DateField.INVOICE }

            // Details (optional)
            val rot by androidx.compose.animation.core.animateFloatAsState(if (detailsOpen) 90f else 0f, Motion.smooth(), label = "chev")
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable { detailsOpen = !detailsOpen },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Overline(stringResource(R.string.sec_details), Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = c.muted, modifier = Modifier.graphicsLayer { rotationZ = rot })
            }
            AnimatedVisibility(detailsOpen, enter = fadeIn(tween(160)) + expandVertically(Motion.smooth()), exit = fadeOut(tween(100)) + shrinkVertically(Motion.smooth())) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Field(
                            amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, stringResource(R.string.f_amount),
                            error = amountBad, errorText = stringResource(R.string.enter_number), keyboard = KeyboardType.Decimal, mono = true, modifier = Modifier.weight(1f),
                        )
                        Field(invoiceNo, { invoiceNo = it }, stringResource(R.string.f_invoice_no), modifier = Modifier.weight(1f), cap = KeyboardCapitalization.Characters, mono = true)
                    }
                    Text(stringResource(R.string.f_category), style = MaterialTheme.typography.labelLarge, color = c.muted)
                    CategoryGrid(category) { category = if (category == it) null else it }
                    Text(stringResource(R.string.f_paid_by), style = MaterialTheme.typography.labelLarge, color = c.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PaymentMode.entries.forEach { m -> SmallChip(m.label(), payment == m, { payment = if (payment == m) null else m }) }
                    }
                    Field(
                        gstin, { gstin = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(15) }, stringResource(R.string.f_gstin),
                        error = gstBad && (showErrors || gstin.length > 0 && gstin.length == 15), errorText = stringResource(R.string.gstin_15),
                        cap = KeyboardCapitalization.Characters, mono = true,
                        support = if (gstin.isNotEmpty() && gstin.length < 15) "${gstin.length}/15" else null,
                    )
                }
            }

            // Warranty
            Overline(stringResource(R.string.sec_warranty), Modifier.padding(top = 8.dp))
            WizCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallChip(stringResource(R.string.none), warranty == null, { warranty = null })
                    listOf(6L to R.string.w_6m, 12L to R.string.w_1y, 24L to R.string.w_2y, 36L to R.string.w_3y, 60L to R.string.w_5y).forEach { (m, label) ->
                        val d = date.plusMonths(m)
                        SmallChip(stringResource(label), warranty == d, { warranty = d })
                    }
                    SmallChip(stringResource(R.string.w_custom), warranty != null && listOf(6L, 12L, 24L, 36L, 60L).none { date.plusMonths(it) == warranty }, { datePicker = DateField.WARRANTY })
                }
                AnimatedContent(
                    warranty,
                    transitionSpec = { (fadeIn(tween(180)) + slideInVertically(Motion.smooth()) { it / 2 }) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false) },
                    label = "warranty",
                ) { w ->
                    if (w != null) Column {
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.w_ends, Fmt.date(w)), style = MaterialTheme.typography.bodyMedium.merge(Numeric), color = c.warrantyInk, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Field(notes, { notes = it }, stringResource(R.string.f_notes), singleLine = false, cap = KeyboardCapitalization.Sentences)
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
        }
    }

    datePicker?.let { which ->
        val initial = if (which == DateField.INVOICE) date else (warranty ?: date.plusYears(1))
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { datePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (which == DateField.INVOICE) date = d else warranty = d
                    }
                    datePicker = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { datePicker = null }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; cancel() }) { Text(stringResource(R.string.discard), color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) } },
        )
    }
}

@Composable
private fun RemoveX(modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.padding(2.dp).size(48.dp).clickable(onClick = onClick), contentAlignment = Alignment.TopEnd) {
        Box(Modifier.padding(4.dp).size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, stringResource(R.string.remove), tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun AddTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val c = Wiz.c
    val press = remember { MutableInteractionSource() }
    Column(
        Modifier.pressScale(press, 0.93f).size(width = 96.dp, height = 120.dp).clip(RoundedCornerShape(12.dp)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = c.accentInk, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.text, maxLines = 1)
    }
}

/** 12 categories as a 4 x 3 grid of tinted tiles. */
@Composable
private fun CategoryGrid(selected: Category?, onPick: (Category) -> Unit) {
    val c = Wiz.c
    val haptics = rememberHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Category.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val on = cat == selected
                    val t = categoryTint(cat)
                    Column(
                        Modifier.weight(1f).heightIn(min = 72.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (on) t.copy(alpha = 0.14f) else c.surface)
                            .border(if (on) 2.dp else 1.dp, if (on) t else c.line, RoundedCornerShape(12.dp))
                            .clickable { haptics.tick(); onPick(cat) }.padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        CategoryTile(cat, 36.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(cat.label(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = c.text, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp)
                    }
                }
            }
        }
    }
}

/** Label above, 52 dp tall box, 12 dp corners; mono for numbers. */
@Composable
private fun Field(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    error: Boolean = false, errorText: String = "", keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true,
    cap: KeyboardCapitalization = KeyboardCapitalization.None, mono: Boolean = false, support: String? = null,
) {
    val c = Wiz.c
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (error) c.bad else c.muted, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange, isError = error,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), singleLine = singleLine, minLines = if (singleLine) 1 else 3,
            shape = RoundedCornerShape(12.dp),
            textStyle = if (mono) MaterialTheme.typography.bodyLarge.merge(Numeric) else MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = cap, imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
            supportingText = when { error -> ({ Text(errorText) }); support != null -> ({ Text(support, style = MaterialTheme.typography.labelMedium.merge(Numeric)) }); else -> null },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = c.surface, focusedContainerColor = c.surface,
                unfocusedBorderColor = c.line, focusedBorderColor = c.accentInk, cursorColor = c.accentInk,
            ),
        )
    }
}

@Composable
private fun DateBox(label: String, value: String, onClick: () -> Unit) {
    val c = Wiz.c
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.muted, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.merge(Numeric), color = c.text)
            Icon(Icons.Rounded.CalendarMonth, null, tint = c.accentInk)
        }
    }
}
