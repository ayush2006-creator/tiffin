package com.example.propelassignment.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB590), onPrimary = Color(0xFF5E1A00),
    secondary = Color(0xFFFFC45B), tertiary = Color(0xFF75DD91),
    background = Color(0xFF18120F), onBackground = Color(0xFFF1DFD6),
    surface = Color(0xFF211916), onSurface = Color(0xFFF1DFD6),
    surfaceVariant = Color(0xFF51443E), onSurfaceVariant = Color(0xFFD8C2B7),
    outline = Color(0xFFA08C82)
)

private val LightColorScheme = lightColorScheme(
    primary = Saffron, onPrimary = SurfaceWarm,
    primaryContainer = Color(0xFFCC4900), onPrimaryContainer = SurfaceWarm,
    secondary = Color(0xFF855300), onSecondary = SurfaceWarm,
    secondaryContainer = Turmeric, onSecondaryContainer = Ink,
    tertiary = Leaf, onTertiary = SurfaceWarm,
    background = Cream, onBackground = Ink,
    surface = SurfaceWarm, onSurface = Ink,
    surfaceVariant = Color(0xFFE4E2DF), onSurfaceVariant = Color(0xFF5A4138),
    outline = OutlineWarm, error = ErrorRed
)

@Composable
fun PropelAssignmentTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
