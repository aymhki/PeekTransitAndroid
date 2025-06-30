package com.aymanhki.peektransit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,        // Accent Blue
    secondary = AccentBlue,
    tertiary = AccentBlue,
    background = Color(0xFF171717),
    surface = Color(0xFF232323),
    surfaceVariant = Color(0xFF313131),
    primaryContainer = AccentBlue,
    secondaryContainer = AccentBlue,
    tertiaryContainer = AccentBlue,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onPrimaryContainer = Color.White,
    onSecondaryContainer = Color.White,
    onTertiaryContainer = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    error = Color(0xFFD2183B),      // Error Red
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,        // Accent Blue
    secondary = AccentBlue,      // Same Accent Blue
    tertiary = AccentBlue,       // Same Accent Blue
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0), // Very light gray for slight contrast
    primaryContainer = AccentBlue,
    secondaryContainer = AccentBlue,
    tertiaryContainer = AccentBlue,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onPrimaryContainer = Color.White,
    onSecondaryContainer = Color.White,
    onTertiaryContainer = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
    error = Color(0xFFD2183B),      // Error Red
)

@Composable
fun PeekTransitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    // Force dark theme (for classic theme)
    forceDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveDarkTheme = forceDarkTheme || darkTheme
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}