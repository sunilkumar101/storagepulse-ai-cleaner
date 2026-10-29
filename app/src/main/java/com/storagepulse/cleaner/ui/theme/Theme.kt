package com.storagepulse.cleaner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.storagepulse.cleaner.data.settings.AccentColor

private val ObsidianColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = OnEmeraldLight,
    primaryContainer = Emerald,
    onPrimaryContainer = OnEmeraldLight,
    secondary = IndigoLight,
    onSecondary = ObsidianCanvas,
    secondaryContainer = Indigo,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberLight,
    onTertiary = ObsidianCanvas,
    tertiaryContainer = Amber,
    background = ObsidianCanvas,
    onBackground = TextPrimary,
    surface = ObsidianCanvas,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = SurfaceSunken,
    surfaceContainerLow = SurfaceCard,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceRaised,
    outline = SurfaceStroke,
    outlineVariant = SurfaceStroke,
    error = DestructiveRed,
    onError = OnDestructive,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * StoragePulse is dark-only by design (OLED Obsidian Telemetry). [accent] (Screen 10) recolors
 * the primary role: CTAs, switches, the active bottom-nav tab.
 */
@Composable
fun StoragePulseTheme(
    accent: AccentColor = AccentColor.OBSIDIAN,
    content: @Composable () -> Unit,
) {
    val a = accentOf(accent)
    MaterialTheme(
        colorScheme = ObsidianColorScheme.copy(
            primary = a.light,
            onPrimary = a.onLight,
            primaryContainer = a.base,
            onPrimaryContainer = a.onLight,
        ),
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
