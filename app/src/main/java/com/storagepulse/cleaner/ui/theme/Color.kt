package com.storagepulse.cleaner.ui.theme

import androidx.compose.ui.graphics.Color

// Obsidian Telemetry palette (design_screenshots/.../obsidian_telemetry/DESIGN.md).

// Canvas & surfaces
val ObsidianCanvas = Color(0xFF090D16)
val SurfaceCard = Color(0xFF111827)
val SurfaceStroke = Color(0xFF1F2937)
val SurfaceSunken = Color(0xFF0A0E17)
val SurfaceRaised = Color(0xFF1C1F29)

// Functional accents
val Emerald = Color(0xFF10B981)
val EmeraldLight = Color(0xFF4EDEA3) // CTA fill in the Stitch screens
val OnEmeraldLight = Color(0xFF003824)
val Indigo = Color(0xFF6366F1)
val IndigoLight = Color(0xFFC0C1FF)
val Amber = Color(0xFFF59E0B)
val AmberLight = Color(0xFFFFB95F)

// Category data tokens
val VideoRose = Color(0xFFF43F5E)
val PhotoViolet = Color(0xFF8B5CF6)
val AppsCyan = Color(0xFF06B6D4)
val AudioAmber = Amber
val SocialEmerald = Emerald

// Destructive actions
val DestructiveRed = Color(0xFFEF4444)
val OnDestructive = Color(0xFFFFFFFF)

// Text
val TextPrimary = Color(0xFFDFE2EF)
val TextSecondary = Color(0xFFBBCABF)
val TextMuted = Color(0xFF86948A)

/** Telemetry accent (Screen 10): base = glyphs/active nav, light = CTA fill, onLight = text on it. */
data class Accent(val base: Color, val light: Color, val onLight: Color)

fun accentOf(accent: com.storagepulse.cleaner.data.settings.AccentColor): Accent = when (accent) {
    com.storagepulse.cleaner.data.settings.AccentColor.OBSIDIAN -> Accent(Emerald, EmeraldLight, OnEmeraldLight)
    com.storagepulse.cleaner.data.settings.AccentColor.INDIGO -> Accent(Indigo, IndigoLight, Color(0xFF16166B))
    com.storagepulse.cleaner.data.settings.AccentColor.AMBER -> Accent(Amber, AmberLight, Color(0xFF3E2400))
}
