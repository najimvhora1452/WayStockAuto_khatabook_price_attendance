package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = lightColorScheme(
    primary = WayStockPrimary,
    secondary = WayStockTealMedium,
    tertiary = WayStockCyan,
    background = WayStockBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = WayStockTextMain,
    onSurface = WayStockDark,
    outline = WayStockBorder
)

private val LightColorScheme = lightColorScheme(
    primary = WayStockPrimary,
    secondary = WayStockTealMedium,
    tertiary = WayStockCyan,
    background = WayStockBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = WayStockTextMain,
    onSurface = WayStockDark,
    outline = WayStockBorder
)

@Composable
fun WayStockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WayStockTheme(darkTheme = darkTheme, content = content)
}
