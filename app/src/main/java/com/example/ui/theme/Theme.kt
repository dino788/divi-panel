package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DiviDarkColorScheme = darkColorScheme(
    primary = DiviCyan,
    onPrimary = TextOnAccent,
    primaryContainer = DiviCyanContainer,
    onPrimaryContainer = DiviCyan,
    secondary = DiviViolet,
    onSecondary = TextPrimary,
    secondaryContainer = DiviVioletContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = DiviMagenta,
    onTertiary = TextPrimary,
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianDeep,
    surfaceContainerLow = ObsidianBase,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceElevated,
    surfaceContainerHighest = ObsidianSurfaceHigh,
    outline = ObsidianBorder,
    outlineVariant = ObsidianSurfaceHigh,
    error = LiveRed,
    onError = TextPrimary
)

val DiviShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DiviTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DiviDarkColorScheme,
        typography = Typography,
        shapes = DiviShapes,
        content = content
    )
}
