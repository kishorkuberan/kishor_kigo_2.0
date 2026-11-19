package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.UserSettings

val LocalKigoSettings = staticCompositionLocalOf { UserSettings() }

private val KigoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KigoChatTheme(
    settings: UserSettings = UserSettings(),
    content: @Composable () -> Unit
) {
    val isDark = settings.themeMode != "light"
    val isAmoled = settings.themeMode == "amoled"
    val primary = accentColorForPreset(settings.accentPreset, isDark)
    val onPrimary = if (isDark && settings.accentPreset in listOf("cyan", "emerald", "amber")) {
        Color(0xFF04101C)
    } else {
        Color.White
    }

    val colorScheme = when {
        isAmoled -> darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.18f),
            onPrimaryContainer = primary,
            secondary = KigoVioletLight,
            onSecondary = Color(0xFF080A14),
            background = KigoAmoledBlack,
            onBackground = Color(0xFFF4F7FB),
            surface = KigoAmoledSurface,
            onSurface = Color(0xFFF4F7FB),
            surfaceVariant = Color(0xFF121826),
            onSurfaceVariant = Color(0xFF9FB0C7),
            outline = Color(0xFF233044),
            error = KigoDangerRed
        )
        isDark -> darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.18f),
            onPrimaryContainer = primary,
            secondary = KigoVioletLight,
            onSecondary = Color(0xFF0B0E1A),
            background = KigoObsidian,
            onBackground = Color(0xFFECF3FC),
            surface = KigoMidnight,
            onSurface = Color(0xFFECF3FC),
            surfaceVariant = KigoDeepSlate,
            onSurfaceVariant = Color(0xFFA2B5CD),
            outline = Color(0xFF263B59),
            error = KigoDangerRed
        )
        else -> lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.14f),
            onPrimaryContainer = primary,
            secondary = KigoViolet,
            onSecondary = Color.White,
            background = KigoLightBg,
            onBackground = Color(0xFF0F172A),
            surface = KigoLightSurface,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = KigoLightSurfaceVariant,
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            error = KigoDangerRed
        )
    }

    val typography = buildKigoTypography(
        fontStyle = settings.fontStyle,
        fontScale = settings.fontScale
    )

    CompositionLocalProvider(LocalKigoSettings provides settings) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = KigoShapes,
            content = content
        )
    }
}
