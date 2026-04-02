package com.example.tasksbot.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tasksbot.R
import kotlinx.coroutines.launch

/**
 * Короткий заставочный экран: метла с покачиванием «взмаха» и лёгкие частицы пыли.
 */
@Composable
fun BroomSplashScreen(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val baseBg = scheme.background
    val tintTop = scheme.primary.copy(alpha = 0.42f)
    val tintMid = scheme.primaryContainer.copy(alpha = 0.35f)

    val infinite = rememberInfiniteTransition(label = "broomSweep")
    val sweepDeg by infinite.animateFloat(
        initialValue = -16f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sweepDeg",
    )
    val driftPx by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )

    val enterScale = remember { Animatable(0.72f) }
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            enterAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
            )
        }
        enterScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(520, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(tintTop, tintMid, baseBg),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val dust = scheme.onBackground.copy(alpha = 0.12f)
            val w = size.width
            val h = size.height
            listOf(
                Offset(w * 0.22f + driftPx * 3f, h * 0.38f),
                Offset(w * 0.78f - driftPx * 2f, h * 0.44f),
                Offset(w * 0.52f + driftPx * 4f, h * 0.52f),
            ).forEachIndexed { i, p ->
                drawCircle(
                    color = dust.copy(alpha = 0.08f + i * 0.02f),
                    radius = (5 + i).dp.toPx(),
                    center = p,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(132.dp)
                    .offset(x = (driftPx * 1.8f).dp)
                    .scale(enterScale.value)
                    .alpha(enterAlpha.value)
                    .graphicsLayer {
                        rotationZ = sweepDeg
                        transformOrigin = TransformOrigin(0.5f, 0.92f)
                    },
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(enterAlpha.value),
            )
            Text(
                text = "Готовим рабочее место…",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(enterAlpha.value * 0.9f)
                    .offset(y = 6.dp),
            )
        }
    }
}
