package com.aymanhki.peektransit.ui.theme

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
    primary = AccentBlue,
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
    error = Color(0xFFD2183B),
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentBlue,
    tertiary = AccentBlue,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
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
    error = Color(0xFFD2183B),
)

@Composable
fun PeekTransitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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