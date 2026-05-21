package com.crescendo.alarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontFamily

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF63B3ED),
    onPrimary = Color.White,
    background = Color(0xFF0A0A1A),
    surface = Color(0xFF111122),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun CrescendoAlarmTheme(
    fontFamily: FontFamily = FontFamily.Default,
    fontSizeMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = getTypography(fontFamily, fontSizeMultiplier),
        content = content
    )
}
