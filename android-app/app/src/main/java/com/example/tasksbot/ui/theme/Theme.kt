package com.example.tasksbot.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppColors = lightColorScheme(
    primary = TealMain,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = TealDark,
    secondary = SandAccent,
    onSecondary = Color(0xFF2C1810),
    tertiary = TealDark,
    background = SurfaceMint,
    onBackground = Color(0xFF1A2C2E),
    surface = SurfaceCard,
    onSurface = Color(0xFF1A2C2E),
    surfaceVariant = Color(0xFFDCECEB),
    onSurfaceVariant = Color(0xFF3D4949),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

private val AppTypography = Typography()

@Composable
fun TasksBotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
