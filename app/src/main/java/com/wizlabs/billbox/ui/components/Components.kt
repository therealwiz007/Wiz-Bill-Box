package com.wizlabs.billbox.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.floating
import com.wizlabs.billbox.ui.anim.rememberLoopTime
import com.wizlabs.billbox.ui.anim.pressScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.wizlabs.billbox.ui.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.wizlabs.billbox.R
import com.wizlabs.billbox.engine.WarrantyStatus
import com.wizlabs.billbox.ui.theme.Numeric
import com.wizlabs.billbox.ui.theme.categoryTint
import com.wizlabs.billbox.ui.mascot.Tally
import com.wizlabs.billbox.engine.Mood
import com.wizlabs.billbox.data.Fmt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.model.BillPhoto
import com.wizlabs.billbox.model.BillFile
import androidx.compose.ui.unit.sp
import com.wizlabs.billbox.model.Category
import com.wizlabs.billbox.ui.theme.Wiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Category?.icon(): ImageVector = when (this) {
    Category.ELECTRONICS -> Icons.Rounded.Devices
    Category.APPLIANCES -> Icons.Rounded.Kitchen
    Category.GROCERY -> Icons.Rounded.LocalGroceryStore
    Category.FUEL -> Icons.Rounded.LocalGasStation
    Category.MEDICAL -> Icons.Rounded.MedicalServices
    Category.CLOTHING -> Icons.Rounded.Checkroom
    Category.FURNITURE -> Icons.Rounded.Chair
    Category.VEHICLE -> Icons.Rounded.DirectionsCar
    Category.UTILITIES -> Icons.Rounded.Bolt
    Category.RESTAURANT -> Icons.Rounded.Restaurant
    Category.TRAVEL -> Icons.Rounded.Flight
    Category.OTHER, null -> Icons.Rounded.ReceiptLong
}

@Composable
fun Category?.label(): String = stringResource(
    when (this) {
        Category.ELECTRONICS -> R.string.cat_electronics; Category.APPLIANCES -> R.string.cat_appliances
        Category.GROCERY -> R.string.cat_grocery; Category.FUEL -> R.string.cat_fuel; Category.MEDICAL -> R.string.cat_medical
        Category.CLOTHING -> R.string.cat_clothing; Category.FURNITURE -> R.string.cat_furniture; Category.VEHICLE -> R.string.cat_vehicle
        Category.UTILITIES -> R.string.cat_utilities; Category.RESTAURANT -> R.string.cat_food; Category.TRAVEL -> R.string.cat_travel
        Category.OTHER, null -> R.string.cat_other
    },
)

@Composable
fun com.wizlabs.billbox.model.PaymentMode.label(): String = stringResource(
    when (this) {
        com.wizlabs.billbox.model.PaymentMode.CASH -> R.string.pay_cash; com.wizlabs.billbox.model.PaymentMode.UPI -> R.string.pay_upi
        com.wizlabs.billbox.model.PaymentMode.CARD -> R.string.pay_card; com.wizlabs.billbox.model.PaymentMode.NET_BANKING -> R.string.pay_net
        com.wizlabs.billbox.model.PaymentMode.EMI -> R.string.pay_emi; com.wizlabs.billbox.model.PaymentMode.OTHER -> R.string.pay_other
    },
)

/** A bill card / panel: 18 dp corners, 1 dp line, a hair of shadow in light mode only. */
@Composable
fun WizCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Wiz.c
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.pressScale(interaction).clip(shape)
                .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            else Modifier
        ),
        shape = shape,
        color = c.surface,
        border = BorderStroke(1.dp, c.line),
        shadowElevation = if (c.isDark) 0.dp else 1.dp,
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Wiz.c.muted) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun IconBadge(icon: ImageVector, size: Dp = 44.dp, tint: Color = Wiz.c.onAccent, bg: Color = Wiz.c.accent) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Category glyph: 20 dp icon in a 36 dp tile tinted per category (14 % tint, full-colour glyph). */
@Composable
fun CategoryTile(cat: Category?, size: Dp = 36.dp) {
    val t = categoryTint(cat)
    Box(Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(t.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(cat.icon(), null, tint = t, modifier = Modifier.size(size * 0.56f))
    }
}

/** Small cloud status for a bill: uploaded, waiting, or local-only. */
@Composable
fun CloudMark(uploaded: Boolean, signedIn: Boolean, modifier: Modifier = Modifier) {
    val c = Wiz.c
    val (icon, tint) = when {
        !signedIn -> Icons.Rounded.CloudOff to c.muted
        uploaded -> Icons.Rounded.CloudDone to c.good
        else -> Icons.Rounded.CloudUpload to c.warn
    }
    Icon(icon, contentDescription = stringResource(if (uploaded) R.string.cd_saved_drive else if (!signedIn) R.string.cd_local_only else R.string.cd_waiting_upload),
        tint = tint, modifier = modifier.size(18.dp))
}

@Composable
fun Pill(text: String, icon: ImageVector? = null, color: Color = Wiz.c.muted, bg: Color = Wiz.c.surface2, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Words for a warranty ("2 y 3 m left", "Expires in 6 d", "Expired"). Colour is never the only signal. */
@Composable
fun warrantyText(st: WarrantyStatus): String = when {
    st.kind == WarrantyStatus.Kind.EXPIRED -> stringResource(R.string.wchip_expired)
    st.days == 0L -> stringResource(R.string.wchip_today)
    st.kind != WarrantyStatus.Kind.ACTIVE -> stringResource(R.string.wchip_expires_in, st.days.toInt())
    st.years > 0 -> stringResource(R.string.wchip_left_ym, st.years, st.months)
    st.months > 0 -> stringResource(R.string.wchip_left_md, st.months, st.restDays)
    else -> stringResource(R.string.wchip_left_d, st.days.toInt())
}

@Composable
fun warrantyColor(st: WarrantyStatus): Color = when (st.kind) {
    WarrantyStatus.Kind.EXPIRED -> Wiz.c.bad
    WarrantyStatus.Kind.URGENT -> Wiz.c.warn
    else -> Wiz.c.warrantyInk
}

@Composable
fun WarrantyChip(st: WarrantyStatus, modifier: Modifier = Modifier) {
    val col = warrantyColor(st)
    Pill(warrantyText(st), if (st.kind == WarrantyStatus.Kind.EXPIRED) Icons.Rounded.ErrorOutline else Icons.Rounded.Verified,
        color = col, bg = col.copy(alpha = 0.12f), modifier = modifier)
}

/** What the sync pill shows (decided by the screen, see [syncPillKind]). */
enum class SyncKind { SYNCED, UPLOADING, OFFLINE, SIGN_IN, ERROR }

/** Cloud icon + state: Synced / Uploading n / Offline / Sign in / Problem. At least 48 dp tall to tap. */
@Composable
fun SyncPill(kind: SyncKind, pending: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Wiz.c
    val (icon, tint, text) = when (kind) {
        SyncKind.SYNCED -> Triple(Icons.Rounded.CloudDone, c.good, stringResource(R.string.pill_synced))
        SyncKind.UPLOADING -> Triple(Icons.Rounded.CloudSync, c.accentInk, if (pending > 0) stringResource(R.string.pill_uploading_n, pending) else stringResource(R.string.pill_syncing))
        SyncKind.OFFLINE -> Triple(Icons.Rounded.CloudOff, c.muted, stringResource(R.string.pill_offline))
        SyncKind.SIGN_IN -> Triple(Icons.Rounded.CloudOff, c.warn, stringResource(R.string.pill_sign_in))
        SyncKind.ERROR -> Triple(Icons.Rounded.ErrorOutline, c.bad, stringResource(R.string.pill_problem))
    }
    Box(modifier.minimumInteractiveComponentSize().clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(CircleShape).background(tint.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (kind == SyncKind.UPLOADING) SyncingGlyph(tint) else Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** A cloud with a ring turning around it (shared loop clock, draw-phase only). */
@Composable
fun SyncingGlyph(tint: Color) {
    val t = rememberLoopTime(fast = true)
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.CloudSync, null, tint = tint, modifier = Modifier.size(14.dp))
        CircularProgressIndicator(
            progress = { 0.28f }, strokeWidth = 1.75.dp, color = tint, trackColor = Color.Transparent,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = (t.floatValue % 1.1f) / 1.1f * 360f },
        )
    }
}

/** Shows a bill photo, downloading it from Google Drive first if it was saved on another phone. */
@Composable
fun BillImage(
    vm: AppViewModel, billId: String, photo: BillPhoto, targetPx: Int,
    modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop,
) {
    val c = Wiz.c
    // Use an already-decoded bitmap straight away (keeps shared-element transitions seamless).
    val exact = remember(photo.localName, targetPx) { photo.localName?.let { vm.photos.cached(it, targetPx) } }
    val placeholder = remember(photo.localName, targetPx) { exact ?: photo.localName?.let { vm.photos.anyCached(it) } }
    val photoCacheHit = placeholder != null
    val bmp by produceState(placeholder, photo.localName, photo.driveId, targetPx) {
        val name = photo.localName
        if (exact == null && name != null) withContext(Dispatchers.IO) { vm.photos.load(name, targetPx) }?.let { value = it }
    }
    if (photo.localName == null && photo.driveId != null) {
        LaunchedEffect(photo.id) { vm.ensurePhoto(billId, photo.id) {} }
    }
    Box(modifier.background(c.surface2), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            val shown = remember(b) { Animatable(if (photoCacheHit) 1f else 0f) }
            LaunchedEffect(b) { shown.animateTo(1f, tween(320)) }
            Image(b.asImageBitmap(), contentDescription = stringResource(R.string.cd_bill_photo), contentScale = contentScale,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = shown.value })
        } else if (photo.localName == null && photo.driveId != null) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = c.accentInk)
        } else {
            Icon(Icons.Rounded.Image, null, tint = c.muted)
        }
    }
}

/** First page of a PDF bill with a "PDF" badge. [onClick] opens it. */
@Composable
fun PdfTile(vm: AppViewModel, billId: String, file: BillFile, modifier: Modifier = Modifier, widthPx: Int = 360, onClick: (() -> Unit)? = null) {
    val c = Wiz.c
    val thumb by produceState<Bitmap?>(null, file.localName) {
        val n = file.localName
        if (n != null) value = withContext(Dispatchers.IO) { vm.files.thumbnail(n, widthPx) }
    }
    if (file.localName == null && file.driveId != null) LaunchedEffect(file.id) { vm.ensureFile(billId, file.id) {} }
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.surface2).border(1.dp, c.line, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val b = thumb
        if (b != null) Image(b.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else if (file.localName == null && file.driveId != null) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = c.accentInk)
        else Icon(Icons.Rounded.Pdf, null, tint = c.muted, modifier = Modifier.size(28.dp))
        Text("PDF", Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(c.bad).padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/** Tally + title + body (+ optional button). One per screen. */
@Composable
fun EmptyState(
    mood: Mood, title: String, body: String, modifier: Modifier = Modifier, tallySize: Dp = 140.dp,
    action: String? = null, onAction: (() -> Unit)? = null,
) {
    val c = Wiz.c
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp).enterUp(), horizontalAlignment = Alignment.CenterHorizontally) {
        Tally(mood, tallySize)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(action, onAction, Modifier)
        }
    }
}

/** Emerald button: 16 sp bold text (large text keeps 3:1 contrast on the light emerald). 52 dp tall. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    val c = Wiz.c
    val press = remember { MutableInteractionSource() }
    val haptics = com.wizlabs.billbox.ui.anim.rememberHaptics()
    Button(
        onClick = { haptics.tap(); onClick() }, enabled = enabled && !loading, interactionSource = press,
        modifier = modifier.heightIn(min = 52.dp).pressScale(press, 0.95f), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent,
            disabledContainerColor = c.accent.copy(alpha = 0.55f), disabledContentColor = c.onAccent),
    ) {
        if (loading) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = c.onAccent); Spacer(Modifier.width(10.dp)) }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun InfoRow(label: String, value: String, icon: ImageVector? = null, mono: Boolean = false) {
    val c = Wiz.c
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = c.muted, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp)) }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = if (mono) MaterialTheme.typography.bodyLarge.merge(Numeric) else MaterialTheme.typography.bodyLarge, color = c.text,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
    }
}

@Composable
fun SmallChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Wiz.c
    val haptics = com.wizlabs.billbox.ui.anim.rememberHaptics()
    FilterChip(
        selected = selected, onClick = { haptics.tick(); onClick() }, label = { Text(label, maxLines = 1) }, modifier = modifier,
        leadingIcon = if (selected) {
            {
                // the check pops in with a little overshoot
                val pop = remember { Animatable(0f) }
                LaunchedEffect(Unit) { pop.animateTo(1f, com.wizlabs.billbox.ui.anim.Motion.bouncy()) }
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

/** Ring showing how much of a warranty is left; drawn once per state change, no loops. */
@Composable
fun DaysRing(fraction: Float, color: Color, label: String, sub: String, size: Dp = 72.dp) {
    val c = Wiz.c
    val p = remember { Animatable(0f) }
    val reduce = com.wizlabs.billbox.ui.anim.LocalReduceMotion.current
    LaunchedEffect(fraction) { if (reduce) p.snapTo(fraction) else p.animateTo(fraction, tween(700)) }
    Box(Modifier.size(size).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().drawBehind {
            val w = 7.dp.toPx(); val d = this.size.minDimension - w
            drawArc(c.line, 0f, 360f, false, topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(d, d), style = Stroke(w))
            drawArc(color, -90f, 360f * p.value, false, topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
        })
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleMedium.merge(Numeric), color = c.text, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = c.muted, maxLines = 1)
        }
    }
}
