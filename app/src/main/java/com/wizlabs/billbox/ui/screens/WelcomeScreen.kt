@file:OptIn(ExperimentalFoundationApi::class)

package com.wizlabs.billbox.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wizlabs.billbox.R
import com.wizlabs.billbox.engine.Line
import com.wizlabs.billbox.engine.TallyBrain
import com.wizlabs.billbox.ui.anim.enterUp
import com.wizlabs.billbox.ui.components.PrimaryButton
import com.wizlabs.billbox.ui.mascot.Tally
import com.wizlabs.billbox.ui.theme.Wiz

/** Three onboarding pages with Tally (hello, file, bell), then Google sign-in or "use without sign-in". */
@Composable
fun WelcomeScreen(signingIn: Boolean, error: String?, onSignIn: () -> Unit, onSkip: () -> Unit, onPrivacy: () -> Unit) {
    val c = Wiz.c
    val pager = rememberPagerState { 3 }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(Wordmark(18f), maxLines = 1)
        HorizontalPager(pager, Modifier.weight(1f).widthIn(max = 520.dp)) { page ->
            val mood = TallyBrain.onboarding(page)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Tally(mood, 150.dp, replay = if (pager.currentPage == page) 1 else 0)
                Spacer(Modifier.height(20.dp))
                Text(stringResource(titleOf(mood.line)), style = MaterialTheme.typography.headlineMedium, color = c.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(bodyOf(mood.line)), style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(if (i == pager.currentPage) 22.dp else 8.dp, 8.dp).clip(CircleShape).background(if (i == pager.currentPage) c.accent else c.line))
            }
        }
        if (error != null) {
            Row(
                Modifier.fillMaxWidth().widthIn(max = 520.dp).clip(RoundedCornerShape(14.dp)).background(c.bad.copy(alpha = 0.10f))
                    .border(1.dp, c.bad.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(error, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.text)
                TextButton(onClick = onSignIn, enabled = !signingIn, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.retry), color = c.bad) }
            }
            Spacer(Modifier.height(10.dp))
        }
        PrimaryButton(stringResource(R.string.sign_in_google), onSignIn, Modifier.fillMaxWidth().widthIn(max = 520.dp).enterUp(1, distance = 24.dp), loading = signingIn)
        TextButton(onClick = onSkip, enabled = !signingIn, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.use_without), color = c.muted) }
        TextButton(onClick = onPrivacy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.privacy_policy), color = c.muted, style = MaterialTheme.typography.labelMedium) }
    }
}

private fun titleOf(l: Line?) = when (l) { Line.Onboard1 -> R.string.ob1_title; Line.Onboard2 -> R.string.ob2_title; else -> R.string.ob3_title }
private fun bodyOf(l: Line?) = when (l) { Line.Onboard1 -> R.string.ob1_body; Line.Onboard2 -> R.string.ob2_body; else -> R.string.ob3_body }
