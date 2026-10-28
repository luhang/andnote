package com.localnotes.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = InkBlue,
    onPrimary = Color.White,
    primaryContainer = InkBlueContainer,
    onPrimaryContainer = OnInkBlueContainer,
    secondary = Teal,
    onSecondary = Color.White,
    background = PaperLight,
    onBackground = Color(0xFF14161C),
    surface = SurfaceLight,
    onSurface = Color(0xFF14161C),
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = Color(0xFF5A5F6E),
    outline = Color(0xFF9AA0AE),
    outlineVariant = OutlineLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = Color(0xFFF1F3F9),
    surfaceContainerHigh = Color(0xFFEAEDF5)
)

private val DarkColors = darkColorScheme(
    primary = InkBlueLight,
    onPrimary = Color(0xFF152A8C),
    primaryContainer = InkBlueContainerDark,
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Color(0xFF5AD1A8),
    onSecondary = Color(0xFF04291C),
    background = PaperDark,
    onBackground = Color(0xFFE6E8EF),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E8EF),
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = Color(0xFFADB2C0),
    outline = Color(0xFF6C7280),
    outlineVariant = OutlineDark,
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = Color(0xFF1E222A),
    surfaceContainerHigh = Color(0xFF262B34)
)

@Composable
fun LocalNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
