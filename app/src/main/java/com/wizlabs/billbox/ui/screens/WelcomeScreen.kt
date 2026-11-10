package com.wizlabs.billbox.ui.screens

import androidx.compose.foundation.Image
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
        Image(painterResource(R.drawable.ic_splash), null, Modifier.size(120.dp))
        Spacer(Modifier.height(12.dp))
        Text("Wiz Bill Box", style = MaterialTheme.typography.displaySmall, color = c.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "Snap every bill once. Find it in seconds, on any phone.",
            style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Point(Icons.Rounded.PhotoCamera, "Photograph bills", "Add the store, product and invoice date, plus price, warranty and notes if you want.")
        Point(Icons.Rounded.CloudSync, "Kept in your Google Drive", "Every bill goes to a \"Wiz Bill Box\" folder in your own Drive. Sign in on a new phone and they all come back.")
        Point(Icons.Rounded.Search, "Find anything", "Search by store, product or invoice number. Get warranty end dates at a glance.")
        Point(Icons.Rounded.Lock, "Private", "The app can only see the files it creates. Nothing else in your Drive.")
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onSignIn, enabled = !signingIn,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
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
        TextButton(onClick = onSkip, enabled = !signingIn) { Text("Use on this phone only for now", color = c.muted) }
        TextButton(onClick = onPrivacy) { Text("Privacy policy", color = c.muted, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    val c = Wiz.c
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBadge(icon, size = 40.dp, tint = c.accentInk, bg = c.surface2)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted)
        }
    }
}
