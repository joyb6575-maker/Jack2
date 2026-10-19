package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val JackDarkColorScheme = darkColorScheme(
    primary = JackCyanNeon,
    onPrimary = Color.Black,
    primaryContainer = JackSurfaceElevated,
    onPrimaryContainer = JackCyanNeon,
    secondary = JackBlueNeon,
    onSecondary = Color.White,
    secondaryContainer = JackSurfaceCard,
    onSecondaryContainer = JackTextPrimary,
    tertiary = JackVioletNeon,
    onTertiary = Color.White,
    background = JackSpaceDark,
    onBackground = JackTextPrimary,
    surface = JackSurfaceDark,
    onSurface = JackTextPrimary,
    surfaceVariant = JackSurfaceCard,
    onSurfaceVariant = JackTextSecondary,
    outline = JackCyanNeon.copy(alpha = 0.35f),
    outlineVariant = Color(0xFF1E293B),
    error = JackRedAlert,
    onError = Color.White
)

val JackLightColorScheme = lightColorScheme(
    primary = JackLightCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    background = JackLightBg,
    onBackground = JackLightTextPrimary,
    surface = JackLightSurface,
    onSurface = JackLightTextPrimary,
    surfaceVariant = JackLightCard,
    onSurfaceVariant = JackLightTextSecondary,
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = JackRedAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve distinctive futuristic identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JackDarkColorScheme else JackLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun JackAITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
}
