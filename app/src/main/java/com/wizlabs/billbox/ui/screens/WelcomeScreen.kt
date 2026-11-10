package com.wizlabs.billbox.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.wizlabs.billbox.ui.anim.LocalReduceMotion
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.anim.floating
import com.wizlabs.billbox.ui.anim.pressScale
import com.wizlabs.billbox.ui.anim.rememberPulse
import com.wizlabs.billbox.ui.anim.shimmer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wizlabs.billbox.R
import com.wizlabs.billbox.ui.components.IconBadge
import com.wizlabs.billbox.ui.theme.Wiz

@Composable
fun WelcomeScreen(signingIn: Boolean, onSignIn: () -> Unit, onSkip: () -> Unit, onPrivacy: () -> Unit) {
    val c = Wiz.c
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        HeroLogo()
        Spacer(Modifier.height(12.dp))
        Text("Wiz Bill Box", style = MaterialTheme.typography.displaySmall, color = c.text, modifier = Modifier.enterUp(1))
        Spacer(Modifier.height(8.dp))
        Text(
            "Snap every bill once. Find it in seconds, on any phone.",
            style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center,
            modifier = Modifier.enterUp(2),
        )
        Spacer(Modifier.height(32.dp))
        Point(Icons.Rounded.PhotoCamera, "Photograph bills", "Add the store, product and invoice date, plus price, warranty and notes if you want.", index = 3)
        Point(Icons.Rounded.CloudSync, "Kept in your Google Drive", "Every bill goes to a \"Wiz Bill Box\" folder in your own Drive. Sign in on a new phone and they all come back.", index = 4)
        Point(Icons.Rounded.Search, "Find anything", "Search by store, product or invoice number. Get warranty end dates at a glance.", index = 5)
        Point(Icons.Rounded.Lock, "Private", "The app can only see the files it creates. Nothing else in your Drive.", index = 6)
        Spacer(Modifier.height(28.dp))
        val press = remember { MutableInteractionSource() }
        Button(
            onClick = onSignIn, enabled = !signingIn, interactionSource = press,
            modifier = Modifier.enterUp(7, distance = 40.dp).pressScale(press, 0.95f).fillMaxWidth().height(56.dp)
                .clip(RoundedCornerShape(16.dp)).then(if (signingIn) Modifier else Modifier.shimmer()),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
        ) {
            if (signingIn) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = c.onAccent)
                Spacer(Modifier.width(12.dp))
                Text("Connecting to Google…", fontWeight = FontWeight.Bold)
            } else {
                Text("G", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Text("Sign in with Google", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip, enabled = !signingIn, modifier = Modifier.enterUp(8)) { Text("Use on this phone only for now", color = c.muted) }
        TextButton(onClick = onPrivacy) { Text("Privacy policy", color = c.muted, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String, index: Int) {
    val c = Wiz.c
    Row(Modifier.fillMaxWidth().enterUp(index, stepMs = 90).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBadge(icon, size = 40.dp, tint = c.accentInk, bg = c.surface2)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted)
        }
    }
}

/** The logo pops in with a spin, then floats while a soft yellow halo breathes behind it. */
@Composable
private fun HeroLogo() {
    val c = Wiz.c
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 220f)) }
    val halo = rememberPulse(0.85f, 1.12f, 2600)
    Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(150.dp).graphicsLayer { scaleX = halo * pop.value; scaleY = halo * pop.value; alpha = 0.9f * pop.value }
                .background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.38f), Color.Transparent)), CircleShape),
        )
        Image(
            painterResource(R.drawable.ic_splash), null,
            Modifier.floating(6.dp).size(124.dp).graphicsLayer {
                scaleX = pop.value; scaleY = pop.value
                rotationZ = (1f - pop.value) * -25f
                alpha = pop.value.coerceIn(0f, 1f)
            },
        )
    }
}
