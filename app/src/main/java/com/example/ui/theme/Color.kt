package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// KIGO Brand Core Palette
val KigoObsidian = Color(0xFF081120)
val KigoMidnight = Color(0xFF101B30)
val KigoDeepSlate = Color(0xFF16243D)
val KigoAmoledBlack = Color(0xFF000000)
val KigoAmoledSurface = Color(0xFF090E18)

val KigoLightBg = Color(0xFFF5F8FC)
val KigoLightSurface = Color(0xFFFFFFFF)
val KigoLightSurfaceVariant = Color(0xFFE9EFF8)

// Accent Presets
val KigoCyan = Color(0xFF00E5FF)
val KigoCyanDark = Color(0xFF0097A7)
val KigoViolet = Color(0xFF7C4DFF)
val KigoVioletLight = Color(0xFFB388FF)
val KigoEmerald = Color(0xFF00E676)
val KigoEmeraldDark = Color(0xFF00A152)
val KigoAmber = Color(0xFFFFB300)
val KigoRose = Color(0xFFFF4081)

val KigoOnlineGreen = Color(0xFF22C55E)
val KigoDangerRed = Color(0xFFEF4444)

fun accentColorForPreset(preset: String, isDark: Boolean): Color {
    return when (preset) {
        "violet" -> if (isDark) KigoVioletLight else KigoViolet
        "emerald" -> if (isDark) KigoEmerald else KigoEmeraldDark
        "amber" -> KigoAmber
        "rose" -> KigoRose
        else -> if (isDark) KigoCyan else KigoCyanDark
    }
}

fun parseBubbleColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> Color(0xFF0097A7)
        }
    } catch (_: Exception) {
        Color(0xFF0097A7)
    }
}
