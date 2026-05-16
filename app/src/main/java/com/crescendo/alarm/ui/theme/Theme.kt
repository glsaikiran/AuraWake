package com.crescendo.alarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF63B3ED),
    onPrimary = Color.White,
    background = Color(0xFF0A0A1A),
    surface = Color(0xFF111122),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun CrescendoAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}
