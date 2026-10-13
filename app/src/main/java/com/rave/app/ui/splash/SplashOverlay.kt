package com.rave.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rave.app.R
import com.rave.app.ui.components.RaveLogo
import com.rave.app.ui.theme.RaveTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Animated launch screen shown once per cold start, on top of the app:
 * the three pills grow in one after another, the wordmark slides out beside them, then the
 * whole screen fades into the app. About 1.6 s; taps are blocked while it shows.
 */
@Composable
fun SplashOverlay(onFinished: () -> Unit) {
    val colors = RaveTheme.colors
    val pills = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.82f) }
    val word = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            async { pills.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
            async { logoScale.animateTo(1f, tween(750, easing = LinearOutSlowInEasing)) }
            async {
                delay(450)
                word.animateTo(1f, tween(450, easing = LinearOutSlowInEasing))
            }
        }
        delay(300)
        exit.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - exit.value }
            .background(colors.splashBackground)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        // Horizontal lockup, like the logo: mark on the left, wordmark sliding out from behind it.
        Row(verticalAlignment = Alignment.CenterVertically) {
            RaveLogo(
                color = colors.splashMark,
                progress = pills.value,
                modifier = Modifier.size(76.dp).graphicsLayer {
                    val s = logoScale.value * (1f + 0.06f * exit.value)
                    scaleX = s; scaleY = s
                },
            )
            Text(
                text = stringResource(R.string.app_name).lowercase(),
                color = colors.splashText,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, letterSpacing = (-1.5).sp),
                modifier = Modifier.graphicsLayer {
                    alpha = word.value
                    // Slide the word out from behind the mark.
                    translationX = -2.dp.toPx() - (1f - word.value) * 18.dp.toPx()
                    translationY = -2.dp.toPx()
                },
            )
        }
    }
}
