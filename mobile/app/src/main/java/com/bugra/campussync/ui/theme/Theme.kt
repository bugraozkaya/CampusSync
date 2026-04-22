package com.bugra.campussync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Açık Renk Şeması ─────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = Blue700,
    onPrimary           = Color.White,
    primaryContainer    = BlueContainer,
    onPrimaryContainer  = Blue900,
    secondary           = BlueGrey600,
    onSecondary         = Color.White,
    tertiary            = Teal600,
    onTertiary          = Color.White,
    tertiaryContainer   = TealContainer,
    onTertiaryContainer = Color(0xFF004D40),
    background          = Color(0xFFF5F7FA),
    onBackground        = Color(0xFF1A1C1E),
    surface             = Color.White,
    onSurface           = Color(0xFF1A1C1E),
    surfaceVariant      = Color(0xFFE8ECF0),
    onSurfaceVariant    = Color(0xFF42474E),
    error               = Color(0xFFB00020),
    onError             = Color.White,
    errorContainer      = Color(0xFFFFDAD6),
    onErrorContainer    = Color(0xFF410002),
    outline             = Color(0xFF72777F),
)

// ── Koyu Renk Şeması ─────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = Blue200,
    onPrimary           = Blue800Dark,
    primaryContainer    = BlueContainer700,
    onPrimaryContainer  = BlueContainer,
    secondary           = BlueGrey200,
    onSecondary         = Color(0xFF253140),
    tertiary            = Teal200,
    onTertiary          = Color(0xFF003731),
    tertiaryContainer   = TealContainer800,
    onTertiaryContainer = Teal200,
    background          = Color(0xFF0F1114),
    onBackground        = Color(0xFFE2E2E6),
    surface             = Color(0xFF1A1C1F),
    onSurface           = Color(0xFFE2E2E6),
    surfaceVariant      = Color(0xFF42474E),
    onSurfaceVariant    = Color(0xFFC2C7CF),
    error               = Color(0xFFFFB4AB),
    onError             = Color(0xFF690005),
    errorContainer      = Color(0xFF93000A),
    onErrorContainer    = Color(0xFFFFDAD6),
    outline             = Color(0xFF8C9198),
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun CampusSyncTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
