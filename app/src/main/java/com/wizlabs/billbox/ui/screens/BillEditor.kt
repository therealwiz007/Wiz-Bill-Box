package com.wizlabs.billbox.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import com.wizlabs.billbox.ui.anim.Motion
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.pressScale
import com.wizlabs.billbox.ui.anim.pulseBorder
import com.wizlabs.billbox.ui.anim.rememberHaptics
import androidx.compose.material.icons.rounded.Check
import com.wizlabs.billbox.ui.anim.shakeOn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.data.Fmt
import com.wizlabs.billbox.model.Bill
import com.wizlabs.billbox.model.BillPhoto
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.model.PaymentMode
import com.wizlabs.billbox.ui.components.BillImage
import com.wizlabs.billbox.ui.components.Overline
import com.wizlabs.billbox.ui.components.WizCard
import com.wizlabs.billbox.ui.theme.Wiz
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class DateField { INVOICE, WARRANTY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BillEditor(vm: AppViewModel, existing: Bill?, knownStores: List<String>, onDone: (Bill?) -> Unit) {
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
    val photos = remember { mutableStateListOf<BillPhoto>().apply { addAll(existing?.photos.orEmpty()) } }
    val added = remember { mutableStateListOf<String>() } // files imported in this session, removed on cancel
    var importing by remember { mutableStateOf(0) }
    var showErrors by remember { mutableStateOf(false) }
    var shake by remember { mutableIntStateOf(0) }
    var datePicker by remember { mutableStateOf<DateField?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }

    fun addUris(uris: List<Uri>) {
        uris.forEach { uri ->
            importing++
            scope.launch {
                val name = vm.importPhoto(uri)
                importing--
                if (name != null) { photos += BillPhoto(localName = name); added += name } else vm.toast("Couldn't read that picture")
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

    fun openCamera() {
        val f = vm.photos.newCameraFile()
        cameraFile = f.path
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
        runCatching { camera.launch(uri) }.onFailure { vm.toast("No camera app found") }
    }

    val changed = existing == null && (store.isNotBlank() || product.isNotBlank() || photos.isNotEmpty()) ||
        existing != null && (store != existing.store || product != existing.product || date != existing.invoiceDate ||
            photos.toList() != existing.photos || notes != existing.notes || amount != (existing.amount?.let { Fmt.money(it).replace(",", "") } ?: "") ||
            invoiceNo != existing.invoiceNumber || category != existing.category || payment != existing.paymentMode ||
            warranty != existing.warrantyUntil || gstin != existing.gstin)

    fun cancel() {
        added.forEach { vm.photos.delete(it) }
        onDone(null)
    }

    val amountValue = Fmt.parseAmount(amount)
    val amountBad = amount.isNotBlank() && amountValue == null
    val valid = store.isNotBlank() && product.isNotBlank() && !amountBad && importing == 0

    val haptics = rememberHaptics()
    fun save() {
        if (!valid) { showErrors = true; shake++; haptics.reject(); return }
        val base = existing ?: Bill(store = "", product = "", invoiceDate = date)
        val bill = base.copy(
            store = store.trim(), product = product.trim(), invoiceDate = date, amount = amountValue,
            invoiceNumber = invoiceNo.trim(), category = category, paymentMode = payment, warrantyUntil = warranty,
            gstin = gstin.trim().uppercase(), notes = notes.trim(), photos = photos.toList(),
        )
        vm.saveBill(bill)
        onDone(bill)
    }

    BackHandler { if (changed) confirmDiscard = true else cancel() }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New bill" else "Edit bill") },
                navigationIcon = { IconButton(onClick = { if (changed) confirmDiscard = true else cancel() }) { Icon(Icons.Rounded.Close, "Close") } },
                actions = {
                    Button(
                        onClick = ::save, enabled = importing == 0, shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent,
                            disabledContainerColor = c.surface2, disabledContentColor = c.muted),
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text, navigationIconContentColor = c.text),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Photos
            Overline("Bill photos")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(photos, key = { it.id }) { p ->
                    // New photos pop in with a little bounce; removed ones shrink away.
                    val fresh = p.localName in added
                    val pop = remember { Animatable(if (fresh) 0.4f else 1f) }
                    LaunchedEffect(Unit) { pop.animateTo(1f, Motion.bouncy()) }
                    Box(
                        Modifier.animateItem(fadeInSpec = tween(220), placementSpec = Motion.smooth(), fadeOutSpec = tween(180))
                            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) }
                            .size(width = 110.dp, height = 140.dp),
                    ) {
                        BillImage(vm, existing?.id ?: "", p, 360, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)).clickable {
                                    photos.remove(p)
                                    if (p.localName in added) { vm.photos.delete(p.localName); added.remove(p.localName) }
                                },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Close, "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                }
                if (importing > 0) item {
                    Box(Modifier.size(width = 110.dp, height = 140.dp).clip(RoundedCornerShape(16.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = c.accentInk)
                    }
                }
                item { AddTile(Icons.Rounded.PhotoCamera, "Camera") { openCamera() } }
                item {
                    AddTile(Icons.Rounded.AddPhotoAlternate, "Gallery") {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }

            // Required details
            Overline("Bill details", Modifier.padding(top = 8.dp))
            Field(
                store, { store = it }, "Store name *", error = showErrors && store.isBlank(),
                cap = KeyboardCapitalization.Words,
                modifier = Modifier.enterUp(1).shakeOn(if (store.isBlank()) shake else 0),
            )
            val suggestions = remember(store, knownStores) {
                if (store.length < 1) emptyList()
                else knownStores.filter { it.contains(store, ignoreCase = true) && !it.equals(store, ignoreCase = true) }.take(4)
            }
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { s -> SmallChip(s, false) { store = s } }
                }
            }
            Field(
                product, { product = it }, "Product / items *", error = showErrors && product.isBlank(), cap = KeyboardCapitalization.Sentences,
                modifier = Modifier.enterUp(2).shakeOn(if (product.isBlank()) shake else 0),
            )
            Box(Modifier.enterUp(3)) { DateBox("Invoice date *", Fmt.date(date)) { datePicker = DateField.INVOICE } }

            // Optional
            Overline("Optional", Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(
                    amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, "Amount (₹)",
                    error = amountBad, keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f),
                )
                Field(invoiceNo, { invoiceNo = it }, "Invoice no.", modifier = Modifier.weight(1f), cap = KeyboardCapitalization.Characters)
            }

            Text("Category", style = MaterialTheme.typography.labelLarge, color = c.muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Category.entries.forEach { cat -> SmallChip(cat.label, category == cat) { category = if (category == cat) null else cat } }
            }

            Text("Paid by", style = MaterialTheme.typography.labelLarge, color = c.muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PaymentMode.entries.forEach { m -> SmallChip(m.label, payment == m) { payment = if (payment == m) null else m } }
            }

            WizCard {
                Text("Warranty", style = MaterialTheme.typography.titleSmall, color = c.text, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallChip("None", warranty == null) { warranty = null }
                    listOf(6L to "6 months", 12L to "1 year", 24L to "2 years", 36L to "3 years", 60L to "5 years").forEach { (m, label) ->
                        val d = date.plusMonths(m)
                        SmallChip(label, warranty == d) { warranty = d }
                    }
                    SmallChip("Pick date", false) { datePicker = DateField.WARRANTY }
                }
                AnimatedContent(
                    warranty,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(Motion.smooth()) { it / 2 }) togetherWith fadeOut(tween(150)) using
                            SizeTransform(clip = false)
                    },
                    label = "warranty",
                ) { w ->
                    if (w != null) Column {
                        Spacer(Modifier.height(8.dp))
                        Text("Valid until ${Fmt.date(w)} · ${Fmt.warranty(w)}", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }

            Field(gstin, { gstin = it.take(15) }, "Store GSTIN", cap = KeyboardCapitalization.Characters)
            Field(notes, { notes = it }, "Notes (serial no., model, where kept…)", singleLine = false, cap = KeyboardCapitalization.Sentences)
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { datePicker = null }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("What you entered for this bill won't be saved.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; cancel() }) { Text("Discard", color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun AddTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val c = Wiz.c
    val press = remember { MutableInteractionSource() }
    Column(
        Modifier.pressScale(press, 0.93f).size(width = 110.dp, height = 140.dp).clip(RoundedCornerShape(16.dp)).background(c.surface)
            .pulseBorder(c.accentInk, 16.dp)
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = c.accentInk, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.text)
    }
}

@Composable
private fun Field(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    error: Boolean = false, keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true,
    cap: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    val c = Wiz.c
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, isError = error,
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = cap, imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        supportingText = if (error) ({ Text(if (keyboard == KeyboardType.Decimal) "Enter a number" else "Required") }) else null,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = c.surface, focusedContainerColor = c.surface,
            unfocusedBorderColor = c.line, focusedBorderColor = c.accentInk, focusedLabelColor = c.accentInk, cursorColor = c.accentInk,
        ),
    )
}

@Composable
private fun DateBox(label: String, value: String, onClick: () -> Unit) {
    val c = Wiz.c
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = c.text)
        }
        Icon(Icons.Rounded.CalendarMonth, null, tint = c.accentInk)
    }
}

@Composable
fun SmallChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Wiz.c
    val haptics = rememberHaptics()
    FilterChip(
        selected = selected, onClick = { haptics.tick(); onClick() }, label = { Text(label) },
        leadingIcon = if (selected) {
            {
                // the check pops in with a little overshoot
                val pop = remember { Animatable(0f) }
                LaunchedEffect(Unit) { pop.animateTo(1f, Motion.bouncy()) }
                Icon(Icons.Rounded.Check, null, tint = c.onAccent,
                    modifier = Modifier.size(18.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = (1f - pop.value) * -45f })
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = c.accent, selectedLabelColor = c.onAccent, containerColor = c.surface, labelColor = c.text,
            selectedLeadingIconColor = c.onAccent,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = c.line, selectedBorderColor = c.accent),
        shape = RoundedCornerShape(12.dp),
    )
}
