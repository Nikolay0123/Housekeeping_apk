package com.example.tasksbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Компактная шапка в стиле главного экрана — для единообразия подэкранов.
 */
@Composable
fun ScreenWelcomeStrip(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val brush = Brush.horizontalGradient(
        colors = listOf(
            scheme.primary.copy(alpha = 0.12f),
            scheme.tertiary.copy(alpha = 0.08f),
            scheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
