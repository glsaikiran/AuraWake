package com.crescendo.alarm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun getTypography(fontFamily: FontFamily, sizeMultiplier: Float = 1.0f): Typography {
    return Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * sizeMultiplier).sp,
            lineHeight = (24 * sizeMultiplier).sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * sizeMultiplier).sp,
            lineHeight = (20 * sizeMultiplier).sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * sizeMultiplier).sp,
            lineHeight = (16 * sizeMultiplier).sp,
            letterSpacing = 0.4.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22 * sizeMultiplier).sp,
            lineHeight = (28 * sizeMultiplier).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * sizeMultiplier).sp,
            lineHeight = (24 * sizeMultiplier).sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * sizeMultiplier).sp,
            lineHeight = (20 * sizeMultiplier).sp,
            letterSpacing = 0.1.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * sizeMultiplier).sp,
            lineHeight = (20 * sizeMultiplier).sp,
            letterSpacing = 0.1.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * sizeMultiplier).sp,
            lineHeight = (16 * sizeMultiplier).sp,
            letterSpacing = 0.5.sp
        ),
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = (57 * sizeMultiplier).sp,
            lineHeight = (64 * sizeMultiplier).sp,
            letterSpacing = (-0.25).sp
        )
    )
}

val Typography = getTypography(FontFamily.Default)
