package com.example.tasksbot.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = Typography()
private val sans = FontFamily.SansSerif

/**
 * Типографика Material 3: чёткая иерархия заголовков и удобочитаемый основной текст.
 */
val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = Default.titleLarge.copy(fontFamily = sans, fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.copy(fontFamily = sans, fontWeight = FontWeight.SemiBold),
    titleSmall = Default.titleSmall.copy(fontFamily = sans, fontWeight = FontWeight.Medium),
    bodyLarge = Default.bodyLarge.copy(fontFamily = sans, lineHeight = 24.sp),
    bodyMedium = Default.bodyMedium.copy(fontFamily = sans, lineHeight = 22.sp),
    bodySmall = Default.bodySmall.copy(fontFamily = sans, lineHeight = 18.sp),
    labelLarge = Default.labelLarge.copy(
        fontFamily = sans,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = Default.labelMedium.copy(fontFamily = sans, fontWeight = FontWeight.Medium),
    labelSmall = Default.labelSmall.copy(
        fontFamily = sans,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    ),
)
