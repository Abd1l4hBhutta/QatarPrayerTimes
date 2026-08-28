package com.example.qatarprayertimes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    secondary = Accent,
    onSecondary = Background,
    background = Background,
    onBackground = Foreground,
    surface = Card,
    onSurface = Foreground,
    surfaceVariant = Card,
    onSurfaceVariant = Muted,
    outline = Color(0xFF3A372F),
)

@Composable
fun QatarPrayerTimesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
