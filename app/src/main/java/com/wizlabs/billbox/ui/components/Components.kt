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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
fun WizCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Wiz.c
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.pressScale(interaction).clip(RoundedCornerShape(20.dp))
                .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            else Modifier
        ),
        shape = RoundedCornerShape(20.dp),
        color = c.surface,
        border = BorderStroke(1.dp, c.line),
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

/** Small cloud status for a bill: uploaded, waiting, or local-only. */
@Composable
fun CloudMark(uploaded: Boolean, signedIn: Boolean, modifier: Modifier = Modifier) {
    val c = Wiz.c
    val (icon, tint) = when {
        !signedIn -> Icons.Rounded.CloudOff to c.muted
        uploaded -> Icons.Rounded.CloudDone to c.good
        else -> Icons.Rounded.CloudUpload to c.warn
    }
    Icon(icon, contentDescription = if (uploaded) "Saved in Google Drive" else "Waiting to upload", tint = tint, modifier = modifier.size(18.dp))
}

@Composable
fun Pill(text: String, icon: ImageVector? = null, color: Color = Wiz.c.muted, bg: Color = Wiz.c.surface2, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SyncingIcon() {
    // A cloud that gently bobs while a ring spins around it.
    val t = rememberInfiniteTransition(label = "sync")
    val turn by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "turn")
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.CloudSync, "Syncing", tint = Wiz.c.accentInk, modifier = Modifier.size(20.dp).floating(1.5.dp, 900))
        CircularProgressIndicator(
            progress = { 0.28f }, strokeWidth = 2.5.dp, color = Wiz.c.accentInk, trackColor = Color.Transparent,
            modifier = Modifier.size(32.dp).graphicsLayer { rotationZ = turn },
        )
    }
}

/**
 * Shows a bill photo, downloading it from Google Drive first if it was saved on another phone.
 */
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
            Image(b.asImageBitmap(), contentDescription = "Bill photo", contentScale = contentScale,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = shown.value })
        } else if (photo.localName == null && photo.driveId != null) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = c.accentInk)
        } else {
            Icon(Icons.Rounded.Image, null, tint = c.muted)
        }
    }
}

@Composable
fun EmptyBox(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    val c = Wiz.c
    Column(modifier.fillMaxWidth().padding(32.dp).enterUp(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.floating(7.dp).size(84.dp).clip(RoundedCornerShape(28.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = c.accentInk, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.size(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.text)
        Spacer(Modifier.size(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun InfoRow(label: String, value: String, icon: ImageVector? = null) {
    val c = Wiz.c
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = c.muted, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp)) }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = c.text, fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1.4f))
    }
}
